package com.realmmc.core.commands;

import com.realmmc.core.manager.combatStatsManager;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

/**
 * Comando para visualizar estatísticas de combate.
 */
public class combatStats implements CommandExecutor {
    private final combatStatsManager statsManager = combatStatsManager.getInstance();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Apenas jogadores podem usar este comando!");
            return true;
        }
        Player player = (Player) sender;
        statsManager.sendCombatInfo(player);
        return true;
    }
}