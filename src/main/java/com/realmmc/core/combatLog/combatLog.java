package com.realmmc.core.combatLog;

import com.realmmc.core.manager.combatStatsManager;
import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class combatLog implements Listener {
    private final JavaPlugin plugin;
    private final combatStatsManager statsManager = combatStatsManager.getInstance();
    private final Map<UUID, Set<CombatData>> combatData = new ConcurrentHashMap<>();
    private final Map<UUID, Long> combatStartTimes = new ConcurrentHashMap<>();
    private final List<String> allowedCommands = Arrays.asList("tell", "r", "reply", "discord");
    private final int combatDuration = 30;

    private static final TextComponent ACTION_BAR_MESSAGE = new TextComponent();
    private static final String COMBAT_MESSAGE = ChatColor.RED + "Você está em combate com " + "%s" + ChatColor.RED + " por mais %d segundos.";
    private static final String MULTIPLE_COMBAT_MESSAGE = ChatColor.RED + "Você está em combate com %d jogadores por mais %ds.";
    private static final String COMBAT_END_MESSAGE = ChatColor.GREEN + "Você não está mais em combate.";
    private static final String DISCONNECT_MESSAGE = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " deslogou em combate e foi morto!";
    private static final String COMMAND_BLOCKED = ChatColor.RED + "Você não pode usar este comando enquanto estiver em combate!";
    private static final String DEATH_MESSAGE = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " morreu em combate!";

    public combatLog(JavaPlugin plugin) {
        this.plugin = plugin;
        startActionBarUpdater();
    }

    public boolean isPlayerInCombat(UUID playerId) {
        return combatData.containsKey(playerId);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player damaged = (Player) event.getEntity();
        Player damager = getDamager(event.getDamager());
        if (damager == null || damaged == damager) return;

        addCombat(damager, damaged);
        addCombat(damaged, damager);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        endCombatForPlayer(player, true);

        Set<CombatData> playerCombatData = combatData.get(player.getUniqueId());
        if (playerCombatData != null) {
            for (CombatData data : playerCombatData) {
                Player opponent = Bukkit.getPlayer(data.opponent);
                if (opponent != null && opponent.isOnline()) {
                    opponent.sendMessage(String.format(DEATH_MESSAGE, playerNameUtils.getFormattedName(player)));
                    removePlayerFromCombat(opponent.getUniqueId(), player.getUniqueId());
                }
            }
        }
    }

    private Player getDamager(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }

    public void addCombat(Player player, Player opponent) {
        UUID playerId = player.getUniqueId();
        UUID opponentId = opponent.getUniqueId();
        long now = System.currentTimeMillis();

        Set<CombatData> dataSet = combatData.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());

        CombatData existingData = null;
        for (CombatData data : dataSet) {
            if (data.opponent.equals(opponentId)) {
                existingData = data;
                break;
            }
        }

        if (existingData != null) {
            existingData.timestamp = now;
        } else {
            dataSet.add(new CombatData(opponentId, now));
        }

        if (dataSet.size() == 1) {
            statsManager.recordCombatStart(playerId);
            combatStartTimes.put(playerId, now);
        }

        statsManager.recordCombatEntry(playerId, opponentId);
    }

    private void removePlayerFromCombat(UUID playerId, UUID opponentId) {
        Set<CombatData> dataSet = combatData.get(playerId);
        if (dataSet != null) {
            dataSet.removeIf(data -> data.opponent.equals(opponentId));
            if (dataSet.isEmpty()) {
                endCombatForPlayer(playerId, true);
            }
        }
    }

    private void endCombatForPlayer(Player player, boolean sendMessage) {
        endCombatForPlayer(player.getUniqueId(), sendMessage);
    }

    private void endCombatForPlayer(UUID playerId, boolean sendMessage) {
        Set<CombatData> dataSet = combatData.remove(playerId);
        Long startTime = combatStartTimes.remove(playerId);

        if (dataSet != null) {
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;
                statsManager.recordCombatTime(playerId, duration);
            }

            if (sendMessage) {
                notifyPlayerExitCombat(playerId);
            }

            for (CombatData data : dataSet) {
                UUID opponentId = data.opponent;
                removePlayerFromCombat(opponentId, playerId);
            }
        }
    }

    private void notifyPlayerExitCombat(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(COMBAT_END_MESSAGE);
            soundUtils.reproduzirSucesso(player);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerInCombat(player.getUniqueId()) || player.hasPermission("core.moderator")) return;

        String cmd = event.getMessage().split(" ")[0].replace("/", "").toLowerCase();
        if (allowedCommands.contains(cmd)) return;

        event.setCancelled(true);
        player.sendMessage(COMMAND_BLOCKED);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        handleDisconnect(event.getPlayer());
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        handleDisconnect(event.getPlayer());
    }

    private void handleDisconnect(Player player) {
        if (isPlayerInCombat(player.getUniqueId()) && !player.hasPermission("core.moderator")) {
            player.setHealth(0.0);
            Bukkit.broadcastMessage(String.format(DISCONNECT_MESSAGE, playerNameUtils.getFormattedName(player)));
            endCombatForPlayer(player, true);
            statsManager.recordDisconnectInCombat(player.getUniqueId());
        }
    }

    private void startActionBarUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerId : new HashSet<>(combatData.keySet())) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || player.getGameMode() == GameMode.SPECTATOR) {
                        endCombatForPlayer(playerId, true);
                        continue;
                    }

                    Set<CombatData> dataSet = combatData.get(playerId);
                    if (dataSet == null || dataSet.isEmpty()) continue;

                    long latestTimestamp = dataSet.stream()
                        .mapToLong(data -> data.timestamp)
                        .max()
                        .orElse(0);

                    long elapsed = System.currentTimeMillis() - latestTimestamp;
                    long remaining = combatDuration - TimeUnit.SECONDS.convert(elapsed, TimeUnit.MILLISECONDS);

                    if (remaining <= 0) {
                        endCombatForPlayer(playerId, true);
                        continue;
                    }

                    if (dataSet.size() > 1) {
                        String message = String.format(MULTIPLE_COMBAT_MESSAGE, dataSet.size(), remaining);
                        ACTION_BAR_MESSAGE.setText(message);
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, ACTION_BAR_MESSAGE);
                    } else {
                        UUID opponentId = dataSet.iterator().next().opponent;
                        Player opponent = Bukkit.getPlayer(opponentId);
                        if (opponent != null && opponent.isOnline()) {
                            String message = String.format(COMBAT_MESSAGE,
                                playerNameUtils.getFormattedName(opponent), remaining);
                            ACTION_BAR_MESSAGE.setText(message);
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, ACTION_BAR_MESSAGE);
                        } else {
                            removePlayerFromCombat(playerId, opponentId);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    private static class CombatData {
        private final UUID opponent;
        private long timestamp;

        public CombatData(UUID opponent, long timestamp) {
            this.opponent = opponent;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CombatData that = (CombatData) o;
            return opponent.equals(that.opponent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(opponent);
        }
    }
}