package com.realmmc.core.commands;

import com.google.common.collect.ImmutableMap;
import com.realmmc.core.utils.playerNameUtils;
import com.realmmc.core.utils.soundUtils;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Optional;

public final class gamemode implements CommandExecutor {

    private static final Map<String, GameMode> MODOS_JOGO = ImmutableMap.<String, GameMode>builder()
        .put("0", GameMode.SURVIVAL)
        .put("survival", GameMode.SURVIVAL)
        .put("sobrevivencia", GameMode.SURVIVAL)
        .put("1", GameMode.CREATIVE)
        .put("creative", GameMode.CREATIVE)
        .put("criativo", GameMode.CREATIVE)
        .put("2", GameMode.ADVENTURE)
        .put("adventure", GameMode.ADVENTURE)
        .put("aventura", GameMode.ADVENTURE)
        .put("3", GameMode.SPECTATOR)
        .put("spectator", GameMode.SPECTATOR)
        .put("espectador", GameMode.SPECTATOR)
        .build();

    private static final String SEM_PERMISSAO = ChatColor.RED + "Apenas Administrador ou superior pode utilizar esse comando!";
    private static final String SEM_PERMISSAO_OUTROS = ChatColor.RED + "Apenas Gerente ou superior pode utilizar esse comando!";
    private static final String USO_CORRETO = ChatColor.RED + "Utilize: /gamemode <survival|creative|adventure|spectator> [jogador]";
    private static final String APENAS_JOGADORES = ChatColor.RED + "Apenas jogadores podem mudar seu próprio modo de jogo.";
    private static final String JOGADOR_INEXISTENTE = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " nunca entrou no servidor!";
    private static final String JOGADOR_OFFLINE = ChatColor.RED + "O jogador " + "%s" + ChatColor.RED + " está offline no momento!";
    private static final String SUCESSO_PROPRIO = ChatColor.GREEN + "Você alterou o seu modo de jogo para %s.";
    private static final String SUCESSO_OUTRO = ChatColor.GREEN + "Você alterou o modo de jogo do jogador " + "%s" + ChatColor.GREEN + " para %s.";
    private static final String AUTO_GAMEMODE = ChatColor.RED + "Você não pode alterar o seu próprio modo de jogo!";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!verificarPermissao(sender)) {
            return true;
        }

        if (!validarArgumentos(sender, args)) {
            return true;
        }

        Optional<GameMode> modo = parseModoJogo(args[0]);
        if (!modo.isPresent()) {
            enviarMensagemErro(sender, USO_CORRETO);
            return true;
        }

        return args.length == 1 
            ? alterarModoProprio(sender, modo.get())
            : alterarModoOutro(sender, modo.get(), args[1]);
    }

    private boolean verificarPermissao(CommandSender sender) {
        if (!sender.hasPermission("core.administrator")) {
            enviarMensagemErro(sender, SEM_PERMISSAO);
            return false;
        }
        return true;
    }

    private boolean validarArgumentos(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) {
            enviarMensagemErro(sender, USO_CORRETO);
            return false;
        }
        return true;
    }

    private boolean alterarModoProprio(CommandSender sender, GameMode modo) {
        if (!(sender instanceof Player)) {
            enviarMensagemErro(sender, APENAS_JOGADORES);
            return true;
        }

        Player jogador = (Player) sender;
        jogador.setGameMode(modo);
        enviarMensagemSucesso(jogador, String.format(SUCESSO_PROPRIO, getNomeModo(modo)));
        soundUtils.reproduzirSucesso(jogador);
        return true;
    }

    private boolean alterarModoOutro(CommandSender sender, GameMode modo, String nomeAlvo) {
        if (!sender.hasPermission("core.manager")) {
            enviarMensagemErro(sender, SEM_PERMISSAO_OUTROS);
            return true;
        }
        if (sender instanceof Player) {
            Player jogador = (Player) sender;
            if (jogador.getName().equalsIgnoreCase(nomeAlvo)) {
                enviarMensagemErro(sender, AUTO_GAMEMODE);
                return true;
            }
        }

        Player alvo = playerNameUtils.getOnlinePlayer(nomeAlvo);
        if (alvo == null) {
            tratarJogadorOffline(sender, nomeAlvo);
            return true;
        }

        alvo.setGameMode(modo);
        notificarAlteracaoModo(sender, alvo, modo);
        return true;
    }

    private void tratarJogadorOffline(CommandSender sender, String nomeJogador) {
        String nomeCorreto = playerNameUtils.getCorrectName(nomeJogador);
        if (!playerNameUtils.playerExists(nomeCorreto)) {
            enviarMensagemErro(sender, String.format(JOGADOR_INEXISTENTE, nomeCorreto));
        } else {
            playerNameUtils.getFormattedOfflineName(nomeCorreto).thenAccept(nomeFormatado ->
                enviarMensagemErro(sender, String.format(JOGADOR_OFFLINE, nomeFormatado))
            );
        }
    }

    private void notificarAlteracaoModo(CommandSender remetente, Player alvo, GameMode modo) {
        String nomeModo = getNomeModo(modo);
        String nomeAlvo = playerNameUtils.getFormattedName(alvo);
        String nomeRemetente = getNomeRemetente(remetente);

        if (remetente instanceof Player && ((Player) remetente).equals(alvo)) {
            enviarMensagemSucesso(remetente, String.format(SUCESSO_PROPRIO, nomeModo));
        } else {
            enviarMensagemSucesso(remetente, String.format(SUCESSO_OUTRO, nomeAlvo, nomeModo));
        }

        soundUtils.reproduzirSucesso(alvo);
        if (remetente instanceof Player) {
            soundUtils.reproduzirSucesso((Player) remetente);
        }
    }

    private String getNomeRemetente(CommandSender remetente) {
        return remetente instanceof Player ? 
            playerNameUtils.getFormattedName((Player) remetente) : 
            "Console";
    }

    private Optional<GameMode> parseModoJogo(String entrada) {
        return Optional.ofNullable(MODOS_JOGO.get(entrada.toLowerCase()));
    }

    private String getNomeModo(GameMode modo) {
        switch (modo) {
            case SURVIVAL: return "sobrevivência";
            case CREATIVE: return "criativo";
            case ADVENTURE: return "aventura";
            case SPECTATOR: return "espectador";
            default: return modo.name().toLowerCase();
        }
    }

    private void enviarMensagemErro(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
        if (sender instanceof Player) {
            soundUtils.reproduzirErro((Player) sender);
        }
    }

    private void enviarMensagemSucesso(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
    }
}