package com.realmmc.core.manager;

import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class combatLogManager implements Listener {
    private final Plugin plugin;
    private final Set<String> allowedCommands = new HashSet<>(Arrays.asList("tell", "r", "reply", "discord"));
    private final Map<UUID, Set<CombatData>> combatMap = new ConcurrentHashMap<>();
    private final Map<UUID, Long> combatStartTimes = new ConcurrentHashMap<>();
    private final int combatDuration = 30; // segundos

    private static final String COMBAT_END_MESSAGE = ChatColor.GREEN + "Você não está mais em combate.";
    public static final String COMMAND_BLOCKED = ChatColor.RED + "Você não pode usar este comando enquanto estiver em combate!";
    private static final String DEATH_MESSAGE = ChatColor.RED + "O jogador %s morreu em combate!";
    private static final String DISCONNECT_MESSAGE = ChatColor.RED + "O jogador %s deslogou em combate e foi morto!";

    public combatLogManager(Plugin plugin) {
        this.plugin = plugin;
        startActionBarUpdater();
    }

    public boolean isPlayerInCombat(UUID playerId) {
        Set<CombatData> set = combatMap.get(playerId);
        if (set == null) return false;
        long now = System.currentTimeMillis();
        long latest = set.stream().mapToLong(d -> d.timestamp).max().orElse(0);
        long elapsed = TimeUnit.SECONDS.convert(now - latest, TimeUnit.MILLISECONDS);
        return elapsed < combatDuration;
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
            combatStartTimes.put(playerId, now);
        }
    }

    private void endCombatFor(UUID playerId, boolean notify) {
        Set<CombatData> dataSet = combatMap.remove(playerId);
        combatStartTimes.remove(playerId);
        if (notify) notifyPlayerExitCombat(playerId);
        if (dataSet != null) {
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

    private void notifyPlayerExitCombat(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.sendMessage(COMBAT_END_MESSAGE);
            soundUtils.reproduzirSucesso(player);
        }
    }

    // Mata o jogador e avisa se ele sair/kickar em combate
    public void handleDisconnect(Player player) {
        UUID uuid = player.getUniqueId();
        if (isPlayerInCombat(uuid) && !player.hasPermission("core.moderator")) {
            player.setHealth(0.0);
            Bukkit.broadcastMessage(String.format(DISCONNECT_MESSAGE, playerNameUtils.getFormattedName(player)));
            endCombatFor(uuid, true);
        }
    }

    // BLOQUEIA COMANDOS NÃO PERMITIDOS
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerInCombat(player.getUniqueId()) || player.hasPermission("core.moderator")) return;

        String cmd = event.getMessage().split(" ")[0].replace("/", "").toLowerCase();
        if (allowedCommands.contains(cmd)) return;

        event.setCancelled(true);
        player.sendMessage(COMMAND_BLOCKED);
    }

    // Mata jogador ao sair em combate e avisa
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        handleDisconnect(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onKick(PlayerKickEvent event) {
        handleDisconnect(event.getPlayer());
    }

    // ActionBar e lógica de expiração do combate
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
        CombatData(UUID opponent, long timestamp) { this.opponent = opponent; this.timestamp = timestamp; }
        @Override public boolean equals(Object o) { return o instanceof CombatData && opponent.equals(((CombatData)o).opponent); }
        @Override public int hashCode() { return Objects.hash(opponent); }
    }
}