package com.realmmc.core.commands;

import com.realmmc.core.manager.warpManager;
import com.realmmc.core.utils.soundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.Arrays;

/**
 * Comando para criar e atualizar warps.
 * Usa WarpManager para evitar duplicidade de código.
 *
 * @author github.com/lucascorreagoldani
 */
public final class setwarp implements CommandExecutor {

    private final warpManager warpManager;
    private final FileConfiguration config;
    private final Plugin plugin;

    private static final String PERMISSAO = ChatColor.RED + "Apenas Gerente ou superiores podem executar esse comando!";
    private static final String CONSOLE_BLOQUEADO = ChatColor.RED + "Apenas jogadores podem usar este comando!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /setwarp <nome> [displayname] [permissão]";
    private static final String SUCESSO_CRIACAO = ChatColor.GREEN + "Warp '%s' criada com sucesso!";
    private static final String SUCESSO_ATUALIZACAO = ChatColor.GREEN + "Warp '%s' atualizada com sucesso!";
    private static final String DISPLAYNAME_ALTERADO = ChatColor.YELLOW + "O displayname da warp foi alterado de '%s' para '%s'";
    private static final String LOCALIZACAO_ALTERADA = ChatColor.YELLOW + "A localização da warp foi atualizada";
    private static final String PERMISSAO_ALTERADA = ChatColor.YELLOW + "A permissão da warp foi alterada de '%s' para '%s'";
    private static final String PERMISSAO_REMOVIDA = ChatColor.YELLOW + "A permissão da warp foi removida";
    private static final String PERMISSAO_ADICIONADA = ChatColor.YELLOW + "Permissão '%s' adicionada à warp";

    public setwarp(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
        this.warpManager = new warpManager(config);
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

        if (!(sender instanceof Player)) {
            sender.sendMessage(CONSOLE_BLOQUEADO);
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(USO_CORRETO);
            if (sender instanceof Player) {
                soundUtils.reproduzirErro((Player) sender);
            }
            return true;
        }

        Player player = (Player) sender;
        String warpName = args[0].toLowerCase();
        String displayName = warpName;
        String permission = null;

        // Processar argumentos
        if (args.length > 1) {
            String lastArg = args[args.length - 1];
            if (lastArg.contains(".") || lastArg.contains("permission") || lastArg.startsWith("core.")) {
                permission = lastArg;
                if (args.length > 2) {
                    displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
                } else {
                    displayName = warpName;
                }
            } else {
                displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        boolean warpExiste = config.contains("warps." + warpName);

        String warpExistenteComDisplayName = warpManager.existsDisplayName(displayName, warpName);
        if (warpExistenteComDisplayName != null) {
            sender.sendMessage(ChatColor.RED + "Já existe uma warp com o displayname '" + displayName + "' (warp: " + warpExistenteComDisplayName + ")");
            if (sender instanceof Player) {
                soundUtils.reproduzirErro((Player) sender);
            }
            return true;
        }

        Location location = player.getLocation();
        String path = "warps." + warpName;

        if (warpExiste) {
            String oldDisplayName = config.getString(path + ".display", warpName);
            String oldPermission = config.getString(path + ".permission");
            Location oldLocation = warpManager.getWarpLocation(warpName);

            if (!oldDisplayName.equalsIgnoreCase(displayName)) {
                sender.sendMessage(String.format(DISPLAYNAME_ALTERADO, oldDisplayName, displayName));
            }
            if (permission == null && oldPermission != null) {
                sender.sendMessage(PERMISSAO_REMOVIDA);
            } else if (permission != null && !permission.equals(oldPermission)) {
                if (oldPermission == null) {
                    sender.sendMessage(String.format(PERMISSAO_ADICIONADA, permission));
                } else {
                    sender.sendMessage(String.format(PERMISSAO_ALTERADA, oldPermission, permission));
                }
            }
            if (!locationsEqual(oldLocation, location)) {
                sender.sendMessage(LOCALIZACAO_ALTERADA);
            }
            warpManager.setWarp(warpName, displayName, permission, location);
            plugin.saveConfig();
            sender.sendMessage(String.format(SUCESSO_ATUALIZACAO, displayName));
            soundUtils.reproduzirSucesso(player);
        } else {
            warpManager.setWarp(warpName, displayName, permission, location);
            plugin.saveConfig();
            sender.sendMessage(String.format(SUCESSO_CRIACAO, displayName));
            if (permission != null) {
                sender.sendMessage(String.format(PERMISSAO_ADICIONADA, permission));
            }
            soundUtils.reproduzirSucesso(player);
        }
        return true;
    }

    private boolean locationsEqual(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null) return false;
        return loc1.getWorld().equals(loc2.getWorld()) &&
                loc1.getX() == loc2.getX() &&
                loc1.getY() == loc2.getY() &&
                loc1.getZ() == loc2.getZ() &&
                loc1.getYaw() == loc2.getYaw() &&
                loc1.getPitch() == loc2.getPitch();
    }
}