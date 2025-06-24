package com.realmmc.core.combatLog;

import java.util.UUID;

public class playerCombatInfo {
    private int disconnectCount;
    private UUID lastOpponent;
    private String lastCombatTime;
    private int combatEntries;
    private long totalCombatTime;

    public playerCombatInfo() {
        this.disconnectCount = 0;
        this.lastOpponent = null;
        this.lastCombatTime = "Nunca";
        this.combatEntries = 0;
        this.totalCombatTime = 0;
    }

    public int getDisconnectCount() { return disconnectCount; }
    public void incrementDisconnectCount() { this.disconnectCount++; }

    public UUID getLastOpponent() { return lastOpponent; }
    public void setLastOpponent(UUID lastOpponent) { this.lastOpponent = lastOpponent; }

    public String getLastCombatTime() { return lastCombatTime; }
    public void setLastCombatTime(String lastCombatTime) { this.lastCombatTime = lastCombatTime; }

    public int getCombatEntries() { return combatEntries; }
    public void incrementCombatEntries() { this.combatEntries++; }

    public long getTotalCombatTime() { return totalCombatTime; }
    public void addCombatTime(long milliseconds) { this.totalCombatTime += milliseconds; }
}