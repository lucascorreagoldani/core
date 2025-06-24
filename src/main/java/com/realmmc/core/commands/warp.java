package com.realmmc.core.commands;

import com.realmmc.core.manager.warpManager;
import com.realmmc.core.utils.jsonUtils;
import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import com.realmmc.core.utils.teleportUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import java.util.*;

/**
 * Comando para teleportar para warps e listar warps.
 * Suporta /warp <warp> e /warp <warp>:<jogador>
 * 
 * Usa WarpManager, teleportUtils, playerNameUtils, soundUtils e jsonUtils.
 *
 * @author github.com/lucascorreagoldani
 */
public final class warp implements CommandExecutor {

    private final warpManager warpManager;
    private final FileConfiguration config;
    private final Plugin plugin;

    private static final String PERMISSAO_OUTROS = ChatColor.RED + "Apenas Moderador ou superiores podem executar esse comando!";
    private static final String JOGADOR_OFFLINE = ChatColor.RED + "O jogador %s está offline no momento!";
    private static final String JOGADOR_INEXISTENTE = ChatColor.RED + "O jogador %s nunca entrou no servidor!";
    private static final String AUTO_TELEPORTE = ChatColor.RED + "Você não pode teleportar a si mesmo para uma warp!";
    private static final String SUCESSO_TELEPORTE = ChatColor.GREEN + "Teleportado para a warp %s!";
    private static final String SUCESSO_TELEPORTE_OUTRO = ChatColor.GREEN + "Você teleportou o jogador %s para a warp %s!";
    private static final String NOTIF_TELEPORTE = ChatColor.YELLOW + "Você foi teleportado para a warp %s por %s.";
    private static final String COMANDO_CONSOLE = ChatColor.RED + "Apenas jogadores podem usar este comando!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /warp <warp>:[jogador]";
    private static final String LISTA_WARPS = ChatColor.GOLD + "Warps disponíveis:";
    private static final String SINTAXE_TELEPORTE_OUTRO = ChatColor.RED + "Utilize: /warp <warp>:[jogador]";
    private static final String SINTAXE_INVALIDA = ChatColor.RED + "Utilize: /warp <warp>";
    private static final String SEM_PERMISSAO_WARP = ChatColor.RED + "Você não pode acessar a warp %s!";
    private static final String SEM_PERMISSAO_WARP_VIP = ChatColor.RED + "Para acessar esta área você precisa do grupo Campeão ou superior!";
    private static final String SEM_PERMISSAO_WARP_OUTRO = ChatColor.RED + "O jogador %s não pode acessar essa área!";
    private static final String SEM_PERMISSAO_WARP_VIP_OUTRO = ChatColor.RED + "O jogador %s não tem o grupo necessário para acessar essa área!";
    private static final String WARP_NAO_ENCONTRADA = ChatColor.RED + "A warp %s não foi encontrada!";
    private static final String MUNDO_WARP_INVALIDO = ChatColor.RED + "Mundo da warp não foi encontrado!";

    public warp(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
        this.warpManager = new warpManager(config);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            listarWarps(sender);
            return true;
        }

        String input = String.join(" ", args);
        if (input.contains(":")) {
            return processarTeleporteOutro(sender, input);
        }
        return processarTeleporteProprio(sender, input);
    }

    private boolean processarTeleporteProprio(CommandSender sender, String warpIdentifier) {
        String warpName = warpManager.getInternalWarpName(warpIdentifier);
        if (warpName == null) {
            enviarMensagemErro(sender, String.format(WARP_NAO_ENCONTRADA, warpIdentifier));
            return true;
        }

        String warpPermission = warpManager.getPermission(warpName);
        String displayName = warpManager.getDisplayName(warpName);

        if (warpPermission != null && !warpPermission.isEmpty()) {
            if (!(sender instanceof Player) || !((Player) sender).hasPermission(warpPermission)) {
                if (warpName.equalsIgnoreCase("vip")) {
                    enviarMensagemErro(sender, SEM_PERMISSAO_WARP_VIP);
                } else {
                    enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP, displayName));
                }
                return true;
            }
        }

        Location warpLocation = warpManager.getWarpLocation(warpName);
        if (warpLocation == null || warpLocation.getWorld() == null) {
            enviarMensagemErro(sender, MUNDO_WARP_INVALIDO);
            return true;
        }

        if (!(sender instanceof Player)) {
            enviarMensagemErro(sender, COMANDO_CONSOLE);
            return true;
        }

        Player jogador = (Player) sender;
        teleportUtils.iniciarTeleporte(plugin, jogador, warpLocation, null); // combatLog não é usado aqui

        // Mensagem de sucesso será enviada no teleportUtils
        return true;
    }

    private boolean processarTeleporteOutro(CommandSender sender, String input) {
        if (!sender.hasPermission("core.moderator")) {
            enviarMensagemErro(sender, PERMISSAO_OUTROS);
            return true;
        }

        String[] partes = input.split(":", 2);
        if (partes.length < 2 || partes[1].trim().isEmpty() || partes[1].contains(" ")) {
            enviarMensagemErro(sender, SINTAXE_TELEPORTE_OUTRO);
            return true;
        }

        String warpIdentifier = partes[0].trim();
        String nomeAlvo = partes[1].trim();

        if (sender instanceof Player && ((Player) sender).getName().equalsIgnoreCase(nomeAlvo)) {
            enviarMensagemErro(sender, AUTO_TELEPORTE);
            return true;
        }

        String warpName = warpManager.getInternalWarpName(warpIdentifier);
        if (warpName == null) {
            enviarMensagemErro(sender, String.format(WARP_NAO_ENCONTRADA, warpIdentifier));
            return true;
        }

        String warpPermission = warpManager.getPermission(warpName);
        String displayName = warpManager.getDisplayName(warpName);

        if (warpPermission != null && !warpPermission.isEmpty() && !sender.hasPermission(warpPermission)) {
            if (warpName.equalsIgnoreCase("vip")) {
                enviarMensagemErro(sender, SEM_PERMISSAO_WARP_VIP);
            } else {
                enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP, displayName));
            }
            return true;
        }

        Location warpLocation = warpManager.getWarpLocation(warpName);
        if (warpLocation == null || warpLocation.getWorld() == null) {
            enviarMensagemErro(sender, MUNDO_WARP_INVALIDO);
            return true;
        }

        Player alvo = Bukkit.getPlayerExact(nomeAlvo);
        if (alvo != null) {
            String nomeFormatadoAlvo = playerNameUtils.getFormattedName(alvo);
            if (warpPermission != null && !warpPermission.isEmpty() && !alvo.hasPermission(warpPermission)) {
                if (warpName.equalsIgnoreCase("vip")) {
                    enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP_VIP_OUTRO, nomeFormatadoAlvo));
                } else {
                    enviarMensagemErro(sender, String.format(SEM_PERMISSAO_WARP_OUTRO, nomeFormatadoAlvo));
                }
                return true;
            }
            String nomeFormatadoRemetente = getNomeFormatadoRemetente(sender);
            alvo.sendMessage(String.format(NOTIF_TELEPORTE, displayName, nomeFormatadoRemetente));
            alvo.teleport(warpLocation);
            soundUtils.reproduzirSucesso(alvo);
            enviarMensagemSucesso(sender, String.format(SUCESSO_TELEPORTE_OUTRO, nomeFormatadoAlvo, displayName));
            if (sender instanceof Player) soundUtils.reproduzirSucesso((Player) sender);
            return true;
        } else {
            playerNameUtils.getFormattedOfflineName(nomeAlvo).thenAccept(nomeFormatado -> {
                if (nomeFormatado == null) {
                    enviarMensagemErro(sender, String.format(JOGADOR_INEXISTENTE, nomeAlvo));
                } else {
                    enviarMensagemErro(sender, String.format(JOGADOR_OFFLINE, nomeFormatado));
                }
            });
            return true;
        }
    }

    private void listarWarps(CommandSender sender) {
        Set<String> warpKeys = warpManager.getAllWarpKeys();
        if (warpKeys.isEmpty()) {
            enviarMensagemErro(sender, ChatColor.RED + "Não existe nenhuma warp.");
            return;
        }

        if (sender instanceof Player) {
            soundUtils.reproduzirSucesso((Player) sender);
        }

        TextComponent cabecalho = new TextComponent(LISTA_WARPS);
        cabecalho.setColor(ChatColor.GOLD);

        List<BaseComponent> componentes = new ArrayList<>();
        componentes.add(cabecalho);
        componentes.add(new TextComponent(" ")); // Espaço

        int contador = 0;
        int totalWarps = warpKeys.size();
        boolean isPlayer = sender instanceof Player;
        Player playerSender = isPlayer ? (Player) sender : null;

        for (String warpKey : warpKeys) {
            contador++;
            String displayName = warpManager.getDisplayName(warpKey);
            String warpPermission = warpManager.getPermission(warpKey);
            boolean hasPermission = warpPermission == null || warpPermission.isEmpty() ||
                    (isPlayer && playerSender.hasPermission(warpPermission));

            TextComponent container = new TextComponent();
            TextComponent nomeComponent = new TextComponent(displayName);
            nomeComponent.setColor(hasPermission ? ChatColor.GREEN : ChatColor.RED);
            container.addExtra(nomeComponent);

            if (warpPermission != null && !warpPermission.isEmpty() && !hasPermission) {
                TextComponent cadeado = new TextComponent(" \uD83D\uDD12");
                cadeado.setColor(ChatColor.RED);
                container.addExtra(cadeado);
            }

            String hoverMessage;
            if (warpPermission != null && !warpPermission.isEmpty() && !hasPermission) {
                if (warpKey.equalsIgnoreCase("vip")) {
                    hoverMessage = "Para acessar esta área você precisa\ndo grupo Campeão ou superior!";
                } else {
                    hoverMessage = "Você não pode se teleportar para a warp " + displayName + "!";
                }
            } else {
                hoverMessage = "Clique para se teleportar até a warp " + displayName + "!";
            }

            container.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(hoverMessage).color(ChatColor.YELLOW).create()
            ));
            container.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND,
                    "/warp " + warpKey
            ));
            componentes.add(container);

            if (contador < totalWarps) {
                componentes.add(new TextComponent(ChatColor.YELLOW + ", "));
            }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            player.spigot().sendMessage(componentes.toArray(new BaseComponent[0]));
        } else {
            StringBuilder builder = new StringBuilder();
            for (BaseComponent comp : componentes) {
                builder.append(comp.toPlainText());
            }
            sender.sendMessage(builder.toString());
        }
    }

    private String getNomeFormatadoRemetente(CommandSender sender) {
        return sender instanceof Player ?
                playerNameUtils.getFormattedName((Player) sender) :
                "Console";
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