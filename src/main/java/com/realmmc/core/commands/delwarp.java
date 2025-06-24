package com.realmmc.core.commands;

import com.realmmc.core.manager.warpManager;
import com.realmmc.core.utils.soundUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Comando para deletar warps do servidor. Usa WarpManager.
 */
public final class delwarp implements CommandExecutor {

    private final warpManager warpManager;
    private final Plugin plugin;

    private static final String PERMISSAO = ChatColor.RED + "Apenas Gerente ou superiores podem executar esse comando!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /delwarp <nome>";
    private static final String WARP_NAO_EXISTE = ChatColor.RED + "A warp '%s' não existe!";
    private static final String SUCESSO = ChatColor.GREEN + "Warp '%s' deletada com sucesso!";

    public delwarp(warpManager warpManager, Plugin plugin) {
        this.warpManager = warpManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("core.manager")) {
            enviarMensagemErro(sender, PERMISSAO);
            return true;
        }

        if (args.length != 1) {
            enviarMensagemErro(sender, USO_CORRETO);
            return true;
        }

        String input = args[0];
        String warpKey = warpManager.getInternalWarpName(input);
        if (warpKey == null) {
            enviarMensagemErro(sender, String.format(WARP_NAO_EXISTE, input));
            return true;
        }

        String displayName = warpManager.getDisplayName(warpKey);
        warpManager.removeWarp(warpKey);
        plugin.saveConfig();

        enviarMensagemSucesso(sender, String.format(SUCESSO, displayName));
        return true;
    }

    private void enviarMensagemErro(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) {
            soundUtils.reproduzirErro((Player) sender);
        }
    }

    private void enviarMensagemSucesso(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) {
            soundUtils.reproduzirSucesso((Player) sender);
        }
    }
}