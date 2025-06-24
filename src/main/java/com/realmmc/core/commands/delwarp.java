package com.realmmc.core.commands;

import com.realmmc.core.utils.soundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class delwarp implements CommandExecutor {

    private final FileConfiguration config;
    private final Plugin plugin;

    private static final String PERMISSAO = ChatColor.RED + "Apenas Gerente ou superiores podem executar esse comando!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /delwarp <nome>";
    private static final String WARP_NAO_EXISTE = ChatColor.RED + "A warp '%s' não existe!";
    private static final String SUCESSO = ChatColor.GREEN + "Warp '%s' deletada com sucesso!";

    public delwarp(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("core.manager")) {
            sender.sendMessage(PERMISSAO);
            if (sender instanceof Player) {
                soundUtils.reproduzirErro((Player) sender);
            }
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(USO_CORRETO);
            if (sender instanceof Player) {
                soundUtils.reproduzirErro((Player) sender);
            }
            return true;
        }

        String warpName = args[0].toLowerCase();
        if (!config.contains("warps." + warpName)) {
            sender.sendMessage(String.format(WARP_NAO_EXISTE, warpName));
            if (sender instanceof Player) {
                soundUtils.reproduzirErro((Player) sender);
            }
            return true;
        }

        String displayName = config.getString("warps." + warpName + ".display", warpName);
        config.set("warps." + warpName, null);
        plugin.saveConfig();
        
        sender.sendMessage(String.format(SUCESSO, displayName));
        if (sender instanceof Player) {
            soundUtils.reproduzirSucesso((Player) sender);
        }
        return true;
    }
}