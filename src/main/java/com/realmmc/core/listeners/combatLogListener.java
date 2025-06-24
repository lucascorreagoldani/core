package com.realmmc.core.listeners;

import com.realmmc.core.utils.soundUtils;
import com.realmmc.core.utils.teleportUtils;
import com.realmmc.core.manager.combatLogManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Listener principal de combatLog: integra com CombatLogManager, teleportUtils etc.
 */
public class combatLogListener implements Listener {

    private final combatLogManager combatManager;
    private final Plugin plugin;

    public combatLogListener(combatLogManager manager, Plugin plugin) {
        this.combatManager = manager;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();
        Player damager = getDamager(event.getDamager());
        if (damager == null || damaged == damager) return;

        combatManager.handleCombat(damager, damaged);
        combatManager.handleCombat(damaged, damager);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        combatManager.handleDeath(event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (!combatManager.isPlayerInCombat(player.getUniqueId()) || player.hasPermission("core.moderator")) return;
        String cmd = event.getMessage().split(" ")[0].replace("/", "").toLowerCase();
        if (combatManager.isAllowedCommand(cmd)) return;
        event.setCancelled(true);
        player.sendMessage(combatLogManager.COMMAND_BLOCKED);
        soundUtils.reproduzirErro(player);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        combatManager.handleDisconnect(event.getPlayer());
        teleportUtils.cancelarTeleporte(event.getPlayer()); // cancela teleporte quando sair
    }

    @EventHandler
    public void onKick(PlayerKickEvent event) {
        combatManager.handleDisconnect(event.getPlayer());
        teleportUtils.cancelarTeleporte(event.getPlayer());
    }

    private Player getDamager(org.bukkit.entity.Entity entity) {
        if (entity instanceof Player) return (Player) entity;
        if (entity instanceof Projectile) {
            Projectile projectile = (Projectile) entity;
            if (projectile.getShooter() instanceof Player) {
                return (Player) projectile.getShooter();
            }
        }
        return null;
    }
}