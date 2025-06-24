package com.realmmc.core.commands;

import com.realmmc.core.utils.soundUtils;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.util.Arrays;
import org.bukkit.Bukkit;

public final class setwarp implements CommandExecutor {

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
            // Verificar se o último argumento parece ser uma permissão (contém ponto ou palavra-chave)
            String lastArg = args[args.length - 1];
            if (lastArg.contains(".") || lastArg.contains("permission") || lastArg.startsWith("core.")) {
                permission = lastArg;
                // Se houver mais de 2 argumentos, o displayname é o que está no meio
                if (args.length > 2) {
                    displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length - 1));
                } else {
                    displayName = warpName;
                }
            } else {
                displayName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        // Verificar se a warp já existe
        boolean warpExiste = config.contains("warps." + warpName);
        boolean displayNameExiste = false;
        String warpExistenteComDisplayName = null;

        // Verificar se já existe uma warp com esse displayname
        if (config.getConfigurationSection("warps") != null) {
            for (String key : config.getConfigurationSection("warps").getKeys(false)) {
                String existingDisplay = config.getString("warps." + key + ".display");
                if (existingDisplay != null && existingDisplay.equalsIgnoreCase(displayName) && !key.equalsIgnoreCase(warpName)) {
                    displayNameExiste = true;
                    warpExistenteComDisplayName = key;
                    break;
                }
            }
        }

        // Se já existe uma warp com esse displayname (e não é a mesma warp), cancelar
        if (displayNameExiste && warpExistenteComDisplayName != null && !warpExistenteComDisplayName.equalsIgnoreCase(warpName)) {
            sender.sendMessage(ChatColor.RED + "Já existe uma warp com o displayname '" + displayName + "' (warp: " + warpExistenteComDisplayName + ")");
            if (sender instanceof Player) {
                soundUtils.reproduzirErro((Player) sender);
            }
            return true;
        }

        Location location = player.getLocation();
        String path = "warps." + warpName;

        if (warpExiste) {
            // Atualizar warp existente
            String oldDisplayName = config.getString(path + ".display", warpName);
            String oldPermission = config.getString(path + ".permission");
            Location oldLocation = getWarpLocation(warpName);

            boolean displayNameAlterado = false;
            boolean localizacaoAlterada = false;
            boolean permissaoAlterada = false;

            // Verificar se o displayname foi alterado
            if (!oldDisplayName.equalsIgnoreCase(displayName)) {
                displayNameAlterado = true;
                sender.sendMessage(String.format(DISPLAYNAME_ALTERADO, oldDisplayName, displayName));
            }

            // Verificar se a permissão foi alterada
            if (permission == null && oldPermission != null) {
                permissaoAlterada = true;
                sender.sendMessage(PERMISSAO_REMOVIDA);
            } else if (permission != null && !permission.equals(oldPermission)) {
                permissaoAlterada = true;
                if (oldPermission == null) {
                    sender.sendMessage(String.format(PERMISSAO_ADICIONADA, permission));
                } else {
                    sender.sendMessage(String.format(PERMISSAO_ALTERADA, oldPermission, permission));
                }
            }

            // Verificar se a localização foi alterada
            if (!locationsEqual(oldLocation, location)) {
                localizacaoAlterada = true;
                sender.sendMessage(LOCALIZACAO_ALTERADA);
            }

            // Atualizar os dados
            config.set(path + ".world", location.getWorld().getName());
            config.set(path + ".x", location.getX());
            config.set(path + ".y", location.getY());
            config.set(path + ".z", location.getZ());
            config.set(path + ".yaw", location.getYaw());
            config.set(path + ".pitch", location.getPitch());
            config.set(path + ".display", displayName);
            config.set(path + ".permission", permission);

            plugin.saveConfig();
            sender.sendMessage(String.format(SUCESSO_ATUALIZACAO, displayName));
            soundUtils.reproduzirSucesso(player);
        } else {
            // Criar nova warp
            config.set(path + ".world", location.getWorld().getName());
            config.set(path + ".x", location.getX());
            config.set(path + ".y", location.getY());
            config.set(path + ".z", location.getZ());
            config.set(path + ".yaw", location.getYaw());
            config.set(path + ".pitch", location.getPitch());
            config.set(path + ".display", displayName);
            config.set(path + ".permission", permission);

            plugin.saveConfig();
            sender.sendMessage(String.format(SUCESSO_CRIACAO, displayName));
            if (permission != null) {
                sender.sendMessage(String.format(PERMISSAO_ADICIONADA, permission));
            }
            soundUtils.reproduzirSucesso(player);
        }

        return true;
    }

    private Location getWarpLocation(String warpName) {
        String path = "warps." + warpName;
        if (!config.contains(path + ".world")) return null;

        return new Location(
            Bukkit.getWorld(config.getString(path + ".world")),
            config.getDouble(path + ".x"),
            config.getDouble(path + ".y"),
            config.getDouble(path + ".z"),
            (float) config.getDouble(path + ".yaw"),
            (float) config.getDouble(path + ".pitch")
        );
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