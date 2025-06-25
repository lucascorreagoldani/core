package com.realmmc.core.commands;

import com.realmmc.core.manager.spawnManager;
import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import com.realmmc.core.utils.teleportUtils;
import com.realmmc.core.combatLog.combatLog;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class spawn implements CommandExecutor {

    private final spawnManager spawnManager;
    private final Plugin plugin;
    private final combatLog combatLogInstance;

    private static final String SPAWN_NAO_DEFINIDO = ChatColor.RED + "O spawn não foi encontrado!";
    private static final String MUNDO_SPAWN_INVALIDO = ChatColor.RED + "Mundo de spawn não foi encontrado!";
    private static final String PERMISSAO_MODERADOR = ChatColor.RED + "Apenas Moderador ou superiores podem executar esse comando!";
    private static final String JOGADOR_OFFLINE = ChatColor.RED + "O jogador %s está offline no momento!";
    private static final String JOGADOR_INEXISTENTE = ChatColor.RED + "O jogador %s nunca entrou no servidor!";
    private static final String AUTO_TELEPORTE = ChatColor.RED + "Você não pode teleportar a si mesmo para o spawn!";
    private static final String SUCESSO_TELEPORTE = ChatColor.GREEN + "Teleportado para o spawn!";
    private static final String SUCESSO_TELEPORTE_OUTRO = ChatColor.GREEN + "Você teleportou o jogador %s para o spawn!";
    private static final String NOTIFICACAO_TELEPORTE = ChatColor.YELLOW + "Você foi teleportado para o spawn por %s!";
    private static final String COMANDO_CONSOLE = ChatColor.RED + "Apenas jogadores podem usar este comando!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /spawn [jogador]";

    public spawn(spawnManager spawnManager, Plugin plugin, combatLog combatLogInstance) {
        this.spawnManager = spawnManager;
        this.plugin = plugin;
        this.combatLogInstance = combatLogInstance;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        Location spawnLocation = spawnManager.getSpawnLocation();
        if (spawnLocation == null) {
            enviarMensagemErro(sender, SPAWN_NAO_DEFINIDO);
            return true;
        }
        if (spawnLocation.getWorld() == null) {
            enviarMensagemErro(sender, MUNDO_SPAWN_INVALIDO);
            return true;
        }

        if (args.length == 0) {
            return processarTeleporteProprio(sender, spawnLocation);
        }

        if (args.length == 1) {
            return processarTeleporteOutro(sender, args[0], spawnLocation);
        }

        enviarMensagemErro(sender, USO_CORRETO);
        return true;
    }

    private boolean processarTeleporteProprio(CommandSender sender, Location spawn) {
        if (!(sender instanceof Player)) {
            enviarMensagemErro(sender, COMANDO_CONSOLE);
            return true;
        }
        Player jogador = (Player) sender;
        teleportUtils.iniciarTeleporte(plugin, jogador, spawn, combatLogInstance);
        return true;
    }

    private boolean processarTeleporteOutro(CommandSender sender, String nomeAlvo, Location spawn) {
        if (!sender.hasPermission("core.moderator")) {
            enviarMensagemErro(sender, PERMISSAO_MODERADOR);
            return true;
        }
        if (sender instanceof Player && ((Player) sender).getName().equalsIgnoreCase(nomeAlvo)) {
            enviarMensagemErro(sender, AUTO_TELEPORTE);
            return true;
        }

        Player alvo = Bukkit.getPlayerExact(nomeAlvo);
        if (alvo != null) {
            String nomeFormatadoAlvo = playerNameUtils.getFormattedName(alvo);
            String nomeFormatadoRemetente = getNomeFormatadoRemetente(sender);
            alvo.teleport(spawn);
            alvo.sendMessage(String.format(NOTIFICACAO_TELEPORTE, nomeFormatadoRemetente));
            enviarMensagemSucesso(sender, String.format(SUCESSO_TELEPORTE_OUTRO, nomeFormatadoAlvo));
            soundUtils.reproduzirSucesso(alvo);
            if (sender instanceof Player) soundUtils.reproduzirSucesso((Player) sender);
            return true;
        } else {
            playerNameUtils.getFormattedOfflineName(nomeAlvo).thenAccept(nomeFormatado -> Bukkit.getScheduler().runTask(plugin, () -> {
                if (nomeFormatado == null || nomeFormatado.equals(nomeAlvo)) {
                    enviarMensagemErro(sender, String.format(JOGADOR_INEXISTENTE, nomeAlvo));
                } else {
                    enviarMensagemErro(sender, String.format(JOGADOR_OFFLINE, nomeFormatado));
                }
            }));
            return true;
        }
    }

    private String getNomeFormatadoRemetente(CommandSender sender) {
        return sender instanceof Player ?
                playerNameUtils.getFormattedName((Player) sender) :
                "Console";
    }

    private void enviarMensagemErro(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) soundUtils.reproduzirErro((Player) sender);
    }

    private void enviarMensagemSucesso(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) soundUtils.reproduzirSucesso((Player) sender);
    }
}