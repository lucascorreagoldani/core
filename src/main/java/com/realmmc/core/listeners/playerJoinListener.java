package com.realmmc.core.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class playerJoinListener implements Listener {

    private final JavaPlugin plugin;

    public playerJoinListener(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void aoJogadorEntrar(PlayerJoinEvent event) {
        Player jogador = event.getPlayer();
    }

    @EventHandler
    public void aoJogadorSair(PlayerQuitEvent event) {
        Player jogador = event.getPlayer();
    }
}