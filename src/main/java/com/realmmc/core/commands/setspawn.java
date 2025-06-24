package com.realmmc.core.commands;

import com.realmmc.core.utils.soundUtils;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class setspawn implements CommandExecutor {

    private final FileConfiguration config;
    private final Plugin plugin;

    private static final String APENAS_JOGADORES = ChatColor.RED + "Apenas jogadores podem executar esse comando.";
    private static final String PERMISSAO_NECESSARIA = ChatColor.RED + "Apenas Gerente ou superiores podem executar esse comando!";
    private static final String MUNDO_INVALIDO = ChatColor.RED + "Mundo inválido! Não foi possível definir o spawn.";
    private static final String MUNDO_INEXISTENTE = ChatColor.RED + "Este mundo não existe mais no servidor! Spawn não definido.";
    private static final String ERRO_SALVAR = ChatColor.RED + "Erro ao salvar configuração: ";
    private static final String SUCESSO = ChatColor.GREEN + "Spawn definido com sucesso no mundo '%s'!";

    public setspawn(FileConfiguration config, Plugin plugin) {
        this.config = config;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            enviarMensagemErro(sender, APENAS_JOGADORES);
            return true;
        }

        Player jogador = (Player) sender;

        if (!verificarPermissao(jogador)) {
            return true;
        }

        Location localizacao = jogador.getLocation();
        if (!validarLocalizacao(jogador, localizacao)) {
            return true;
        }

        definirSpawn(localizacao);

        if (!salvarConfiguracao(jogador)) {
            return true;
        }

        enviarMensagemSucesso(jogador, String.format(SUCESSO, localizacao.getWorld().getName()));
        soundUtils.reproduzirSucesso(jogador);

        return true;
    }

    private boolean verificarPermissao(Player jogador) {
        if (!jogador.hasPermission("core.manager")) {
            enviarMensagemErro(jogador, PERMISSAO_NECESSARIA);
            soundUtils.reproduzirErro(jogador);
            return false;
        }
        return true;
    }

    private boolean validarLocalizacao(Player jogador, Location localizacao) {
        World mundo = localizacao.getWorld();
        
        if (mundo == null) {
            enviarMensagemErro(jogador, MUNDO_INVALIDO);
            soundUtils.reproduzirErro(jogador);
            return false;
        }

        if (Bukkit.getWorld(mundo.getName()) == null) {
            enviarMensagemErro(jogador, MUNDO_INEXISTENTE);
            soundUtils.reproduzirErro(jogador);
            return false;
        }

        return true;
    }

    private void definirSpawn(Location localizacao) {
        config.set("spawn.world", localizacao.getWorld().getName());
        config.set("spawn.x", localizacao.getX());
        config.set("spawn.y", localizacao.getY());
        config.set("spawn.z", localizacao.getZ());
        config.set("spawn.yaw", localizacao.getYaw());
        config.set("spawn.pitch", localizacao.getPitch());
    }

    private boolean salvarConfiguracao(Player jogador) {
        try {
            plugin.saveConfig();
            return true;
        } catch (Exception e) {
            enviarMensagemErro(jogador, ERRO_SALVAR + e.getMessage());
            soundUtils.reproduzirErro(jogador);
            return false;
        }
    }

    private void enviarMensagemErro(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
    }

    private void enviarMensagemSucesso(CommandSender sender, String mensagem) {
        sender.sendMessage(mensagem);
    }
}