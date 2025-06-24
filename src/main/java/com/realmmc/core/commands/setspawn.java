package com.realmmc.core.commands;

import com.realmmc.core.manager.spawnManager;
import com.realmmc.core.utils.soundUtils;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Comando para definir o spawn global do servidor.
 */
public final class setspawn implements CommandExecutor {

    private final spawnManager spawnManager;
    private final Plugin plugin;

    private static final String APENAS_JOGADORES = ChatColor.RED + "Apenas jogadores podem executar esse comando.";
    private static final String PERMISSAO_NECESSARIA = ChatColor.RED + "Apenas Gerente ou superiores podem executar esse comando!";
    private static final String MUNDO_INVALIDO = ChatColor.RED + "Mundo inválido! Não foi possível definir o spawn.";
    private static final String MUNDO_INEXISTENTE = ChatColor.RED + "Este mundo não existe mais no servidor! Spawn não definido.";
    private static final String ERRO_SALVAR = ChatColor.RED + "Erro ao salvar configuração: ";
    private static final String SUCESSO = ChatColor.GREEN + "Spawn definido com sucesso no mundo '%s'!";

    public setspawn(spawnManager spawnManager, Plugin plugin) {
        this.spawnManager = spawnManager;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            enviarMensagemErro(sender, APENAS_JOGADORES);
            return true;
        }

        Player jogador = (Player) sender;
        if (!jogador.hasPermission("core.manager")) {
            enviarMensagemErro(jogador, PERMISSAO_NECESSARIA);
            soundUtils.reproduzirErro(jogador);
            return true;
        }

        Location localizacao = jogador.getLocation();
        World mundo = localizacao.getWorld();
        if (mundo == null) {
            enviarMensagemErro(jogador, MUNDO_INVALIDO);
            soundUtils.reproduzirErro(jogador);
            return true;
        }
        if (Bukkit.getWorld(mundo.getName()) == null) {
            enviarMensagemErro(jogador, MUNDO_INEXISTENTE);
            soundUtils.reproduzirErro(jogador);
            return true;
        }

        spawnManager.setSpawn(localizacao);

        try {
            plugin.saveConfig();
        } catch (Exception e) {
            enviarMensagemErro(jogador, ERRO_SALVAR + e.getMessage());
            soundUtils.reproduzirErro(jogador);
            return true;
        }

        enviarMensagemSucesso(jogador, String.format(SUCESSO, localizacao.getWorld().getName()));
        soundUtils.reproduzirSucesso(jogador);
        return true;
    }

    private void enviarMensagemErro(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
    }

    private void enviarMensagemSucesso(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
    }
}