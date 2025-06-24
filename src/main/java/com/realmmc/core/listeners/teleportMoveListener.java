package com.realmmc.core.listeners;

import com.realmmc.core.utils.teleportUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

/**
 * Listener para cancelar teleporte caso o jogador se mova de bloco.
 */
public class teleportMoveListener implements Listener {
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!event.getFrom().getBlock().equals(event.getTo().getBlock())) {
            teleportUtils.cancelarTeleporte(player);
        }
    }
}