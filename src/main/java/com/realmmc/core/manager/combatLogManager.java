package com.realmmc.core.manager;

import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Gerencia status de combate dos jogadores.
 */
public class combatLogManager implements Listener {
    private final Plugin plugin;
    private final combatStatsManager statsManager = combatStatsManager.getInstance();

    // status de combate: UUID -> lista de adversários e timestamps
    private final Map<UUID, Set<CombatData>> combatMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> combatStartTimes = new ConcurrentHashMap<>();
    private final int combatDuration = 30; // segundos

    // Comandos permitidos em combate
    private final Set<String> allowedCommands = new HashSet<>(Arrays.asList("tell", "r", "reply", "discord"));

    private static final String COMBAT_END_MESSAGE = ChatColor.GREEN + "Você não está mais em combate.";
    public static final String COMMAND_BLOCKED = ChatColor.RED + "Você não pode usar este comando enquanto estiver em combate!";
    private static final String DEATH_MESSAGE = ChatColor.RED + "O jogador %s morreu em combate!";
    private static final String DISCONNECT_MESSAGE = ChatColor.RED + "O jogador %s deslogou em combate e foi morto!";

    public combatLogManager(Plugin plugin) {
        this.plugin = plugin;
        startActionBarUpdater();
    }

    // API principal para teleportUtils e outros sistemas
    public boolean isPlayerInCombat(UUID playerId) {
        return combatMap.containsKey(playerId);
    }

    public void handleCombat(Player player, Player opponent) {
        UUID playerId = player.getUniqueId();
        UUID opponentId = opponent.getUniqueId();
        long now = System.currentTimeMillis();

        Set<CombatData> playerCombats = combatMap.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet());
        Optional<CombatData> existing = playerCombats.stream().filter(d -> d.opponent.equals(opponentId)).findFirst();

        if (existing.isPresent()) {
            existing.get().timestamp = now;
        } else {
            playerCombats.add(new CombatData(opponentId, now));
        }

        if (playerCombats.size() == 1) {
            statsManager.recordCombatStart(playerId);
            combatStartTimes.put(playerId, now);
        }

        statsManager.recordCombatEntry(playerId, opponentId);
    }

    public void endCombatFor(Player player, boolean notify) {
        endCombatFor(player.getUniqueId(), notify);
    }

    public void endCombatFor(UUID playerId, boolean notify) {
        Set<CombatData> dataSet = combatMap.remove(playerId);
        Long start = combatStartTimes.remove(playerId);

        if (dataSet != null) {
            if (start != null) {
                statsManager.recordCombatTime(playerId, System.currentTimeMillis() - start);
            }
            if (notify) notifyPlayerExitCombat(playerId);
            for (CombatData data : dataSet) {
                removeCombatFor(data.opponent, playerId);
            }
        }
    }

    public void removeCombatFor(UUID playerId, UUID opponentId) {
        Set<CombatData> dataSet = combatMap.get(playerId);
        if (dataSet != null) {
            dataSet.removeIf(data -> data.opponent.equals(opponentId));
            if (dataSet.isEmpty()) {
                endCombatFor(playerId, true);
            }
        }
    }

    public boolean isAllowedCommand(String cmd) {
        return allowedCommands.contains(cmd.toLowerCase());
    }

    private void notifyPlayerExitCombat(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(COMBAT_END_MESSAGE);
            soundUtils.reproduzirSucesso(player);
        }
    }

    public void handleDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (isPlayerInCombat(uuid) && !player.hasPermission("core.moderator")) {
            player.setHealth(0.0);
            Bukkit.broadcastMessage(String.format(DISCONNECT_MESSAGE, playerNameUtils.getFormattedName(player)));
            endCombatFor(player, true);
            statsManager.recordDisconnectInCombat(uuid);
        }
    }

    public void handleDeath(Player player) {
        endCombatFor(player, true);

        Set<CombatData> playerCombats = combatMap.get(player.getUniqueId());
        if (playerCombats != null) {
            for (CombatData data : playerCombats) {
                Player opponent = Bukkit.getPlayer(data.opponent);
                if (opponent != null && opponent.isOnline()) {
                    opponent.sendMessage(String.format(DEATH_MESSAGE, playerNameUtils.getFormattedName(player)));
                    removeCombatFor(opponent.getUniqueId(), player.getUniqueId());
                }
            }
        }
    }

    private void startActionBarUpdater() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID playerId : new HashSet<>(combatMap.keySet())) {
                    Player player = Bukkit.getPlayer(playerId);
                    if (player == null || player.getGameMode() == GameMode.SPECTATOR) {
                        endCombatFor(playerId, true);
                        continue;
                    }

                    Set<CombatData> dataSet = combatMap.get(playerId);
                    if (dataSet == null || dataSet.isEmpty()) continue;

                    long latest = dataSet.stream().mapToLong(data -> data.timestamp).max().orElse(0);
                    long elapsed = System.currentTimeMillis() - latest;
                    long remaining = combatDuration - TimeUnit.SECONDS.convert(elapsed, TimeUnit.MILLISECONDS);

                    if (remaining <= 0) {
                        endCombatFor(playerId, true);
                        continue;
                    }

                    if (dataSet.size() > 1) {
                        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                new net.md_5.bungee.api.chat.TextComponent(
                                        ChatColor.RED + "Você está em combate com " + dataSet.size() + " jogadores por mais " + remaining + "s."));
                    } else {
                        UUID opponentId = dataSet.iterator().next().opponent;
                        Player opponent = Bukkit.getPlayer(opponentId);
                        if (opponent != null && opponent.isOnline()) {
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    new net.md_5.bungee.api.chat.TextComponent(
                                            ChatColor.RED + "Você está em combate com " +
                                                    playerNameUtils.getFormattedName(opponent) +
                                                    ChatColor.RED + " por mais " + remaining + " segundos."));
                        } else {
                            removeCombatFor(playerId, opponentId);
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 20, 20);
    }

    private static class CombatData {
        private final UUID opponent;
        private long timestamp;

        CombatData(UUID opponent, long timestamp) {
            this.opponent = opponent;
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CombatData)) return false;
            CombatData that = (CombatData) o;
            return opponent.equals(that.opponent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(opponent);
        }
    }
}