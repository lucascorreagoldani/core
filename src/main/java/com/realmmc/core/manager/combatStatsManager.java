package com.realmmc.core.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.realmmc.core.combatLog.playerCombatInfo;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class combatStatsManager {
    private static combatStatsManager instance;
    private final Map<UUID, playerCombatInfo> combatStats = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

    private combatStatsManager() {}

    public static combatStatsManager getInstance() {
        if (instance == null) {
            instance = new combatStatsManager();
        }
        return instance;
    }

    public void recordCombatEntry(UUID playerId, UUID opponentId) {
        playerCombatInfo info = combatStats.computeIfAbsent(playerId, k -> new playerCombatInfo());
        info.setLastOpponent(opponentId);
        info.setLastCombatTime(dateFormat.format(new Date()));
    }

    public void recordCombatStart(UUID playerId) {
        playerCombatInfo info = combatStats.computeIfAbsent(playerId, k -> new playerCombatInfo());
        info.incrementCombatEntries();
    }

    public void recordCombatTime(UUID playerId, long milliseconds) {
        playerCombatInfo info = combatStats.computeIfAbsent(playerId, k -> new playerCombatInfo());
        info.addCombatTime(milliseconds);
    }

    public void recordDisconnectInCombat(UUID playerId) {
        playerCombatInfo info = combatStats.get(playerId);
        if (info != null) {
            info.incrementDisconnectCount();
        }
    }

    public playerCombatInfo getPlayerInfo(UUID playerId) {
        return combatStats.getOrDefault(playerId, new playerCombatInfo());
    }

    public void sendCombatInfo(Player player) {
        playerCombatInfo info = getPlayerInfo(player.getUniqueId());

        long totalSeconds = info.getTotalCombatTime() / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        String formattedTime = String.format("%02d:%02d:%02d", hours, minutes, seconds);

        player.sendMessage("§6§lESTATÍSTICAS DE COMBATE");
        player.sendMessage("§fEntradas em combate: §a" + info.getCombatEntries());
        player.sendMessage("§fTempo total em combate: §a" + formattedTime);
        player.sendMessage("§fDesconexões em combate: §c" + info.getDisconnectCount());

        if (info.getLastOpponent() != null) {
            Player opponent = Bukkit.getPlayer(info.getLastOpponent());
            String opponentName = opponent != null ? opponent.getName() : "Offline";
            player.sendMessage("§fÚltimo oponente: §e" + opponentName);
            player.sendMessage("§fData/hora do último combate: §a" + info.getLastCombatTime());
        } else {
            player.sendMessage("§fÚltimo oponente: §cNenhum");
        }
    }
}