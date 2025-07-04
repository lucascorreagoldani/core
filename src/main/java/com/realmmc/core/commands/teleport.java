package com.realmmc.core.commands;

import com.realmmc.core.Main;
import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

/**
 * Comando staff /tp:
 * /tp <jogador>        - teleporta o executor até o jogador (moderador+)
 * /tp <jogador> <alvo> - teleporta um jogador até outro (admin+)
 *
 * Usa playerNameUtils (com cache, async, LuckPerms) e soundUtils.
 * Teleporte é instantâneo, sem delay/cancelamento.
 */
public final class teleport implements CommandExecutor {

    private final Main plugin;

    private static final String SEM_PERMISSAO = ChatColor.RED + "Apenas Moderador ou superiores podem executar esse comando!";
    private static final String CONSOLE_BLOQUEADO = ChatColor.RED + "Apenas jogadores podem usar este comando.";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /tp <jogador> [jogador]";
    private static final String TP_PARA_SI = ChatColor.RED + "Você não pode se teleportar até você mesmo.";
    private static final String TP_MESMO_JOGADOR = ChatColor.RED + "Não é possível teleportar um jogador até ele mesmo.";
    private static final String TP_SUCESSO = ChatColor.GREEN + "Você foi teleportado até o jogador %s" + ChatColor.GREEN + ".";
    private static final String TP_PUXOU_JOGADOR = ChatColor.GREEN + "Você puxou o jogador %s" + ChatColor.GREEN + " até você!";
    private static final String TP_OUTRO_SUCESSO = ChatColor.GREEN + "Você teleportou o jogador %s" + ChatColor.GREEN + " até o jogador %s" + ChatColor.GREEN + ".";
    private static final String JOGADOR_OFFLINE = ChatColor.RED + "O jogador %s está offline no momento!";
    private static final String JOGADOR_INEXISTENTE = ChatColor.RED + "O jogador %s nunca entrou no servidor!";
    private static final String TP_OUTRO_PERMISSAO = ChatColor.RED + "Apenas Administrador ou superiores podem executar esse comando!";

    public teleport(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("core.moderator")) {
            sendMessage(sender, SEM_PERMISSAO, true);
            return true;
        }

        if (args.length < 1 || args.length > 2) {
            sendMessage(sender, USO_CORRETO, true);
            return true;
        }

        if (args.length == 1) {
            return teleportSelf(sender, args[0]);
        } else {
            return teleportOther(sender, args[0], args[1]);
        }
    }

    // /tp <jogador>
    private boolean teleportSelf(CommandSender sender, String targetName) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, CONSOLE_BLOQUEADO, true);
            return true;
        }

        Player player = (Player) sender;

        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            // Checa se o nome já jogou alguma vez no servidor
            Optional<UUID> uuidOpt = getPlayerUUID(targetName);
            if (!uuidOpt.isPresent()) {
                sendMessage(sender, String.format(JOGADOR_INEXISTENTE, targetName), true);
            } else {
                playerNameUtils.getFormattedOfflineName(targetName).thenAccept(formattedName -> {
                    sendMessage(sender, String.format(JOGADOR_OFFLINE, formattedName), true);
                });
            }
            return true;
        }

        if (player.equals(target)) {
            sendMessage(player, TP_PARA_SI, true);
            return true;
        }

        executeTeleport(player, target, player);
        return true;
    }

    // /tp <jogador> <alvo>
    private boolean teleportOther(CommandSender sender, String playerName, String targetName) {
        if (!sender.hasPermission("core.administrator")) {
            sendMessage(sender, TP_OUTRO_PERMISSAO, true);
            return true;
        }

        Player player = Bukkit.getPlayerExact(playerName);
        Player target = Bukkit.getPlayerExact(targetName);

        if (player == null) {
            Optional<UUID> uuidOpt = getPlayerUUID(playerName);
            if (!uuidOpt.isPresent()) {
                sendMessage(sender, String.format(JOGADOR_INEXISTENTE, playerName), true);
            } else {
                playerNameUtils.getFormattedOfflineName(playerName).thenAccept(formattedName -> {
                    sendMessage(sender, String.format(JOGADOR_OFFLINE, formattedName), true);
                });
            }
            return true;
        }
        if (target == null) {
            Optional<UUID> uuidOpt = getPlayerUUID(targetName);
            if (!uuidOpt.isPresent()) {
                sendMessage(sender, String.format(JOGADOR_INEXISTENTE, targetName), true);
            } else {
                playerNameUtils.getFormattedOfflineName(targetName).thenAccept(formattedName -> {
                    sendMessage(sender, String.format(JOGADOR_OFFLINE, formattedName), true);
                });
            }
            return true;
        }
        if (player.equals(target)) {
            sendMessage(sender, TP_MESMO_JOGADOR, true);
            return true;
        }

        // Se admin faz /tp <alvo> você, puxa o player até si
        if (sender instanceof Player && (
                targetName.equalsIgnoreCase("você") ||
                targetName.equalsIgnoreCase("voce") ||
                ((Player)sender).equals(target)
        )) {
            executePullPlayer(player, (Player)sender);
            return true;
        }

        executeTeleport(player, target, sender);
        return true;
    }

    private static Optional<UUID> getPlayerUUID(String name) {
        try {
            // Usa o método utilitário correto do Bukkit (offline)
            return Optional.ofNullable(Bukkit.getOfflinePlayer(name))
                    .filter(p -> p.hasPlayedBefore() || p.isOnline())
                    .map(offline -> offline.getUniqueId());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private void executeTeleport(Player toMove, Player target, CommandSender initiator) {
        toMove.teleport(target);

        String formattedToMove = playerNameUtils.getFormattedName(toMove);
        String formattedTarget = playerNameUtils.getFormattedName(target);

        if (initiator.equals(toMove)) {
            sendMessage(initiator, String.format(TP_SUCESSO, formattedTarget), false);
        } else {
            sendMessage(initiator, String.format(TP_OUTRO_SUCESSO, formattedToMove, formattedTarget), false);
        }

        soundUtils.reproduzirSucesso(toMove);
        if (initiator instanceof Player && !initiator.equals(toMove)) {
            soundUtils.reproduzirSucesso((Player) initiator);
        }
    }

    private void executePullPlayer(Player toMove, Player target) {
        toMove.teleport(target);
        String formattedToMove = playerNameUtils.getFormattedName(toMove);
        sendMessage(target, String.format(TP_PUXOU_JOGADOR, formattedToMove), false);
        soundUtils.reproduzirSucesso(toMove);
        soundUtils.reproduzirSucesso(target);
    }

    private void sendMessage(CommandSender sender, String message, boolean isError) {
        sender.sendMessage(message);
        if (isError && sender instanceof Player) {
            soundUtils.reproduzirErro((Player) sender);
        }
    }
}