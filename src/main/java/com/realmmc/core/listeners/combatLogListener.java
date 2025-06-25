package com.realmmc.core.listeners;

import com.realmmc.core.utils.teleportUtils;
import com.realmmc.core.combatLog.combatLog;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;

public class combatLogListener implements Listener {

    private final combatLog combatLogInstance;
    private final Plugin plugin;

    public combatLogListener(combatLog combatLogInstance, Plugin plugin) {
        this.combatLogInstance = combatLogInstance;
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player damaged = (Player) event.getEntity();
        Player damager = getDamager(event.getDamager());
        if (damager == null || damaged == damager) return;

        combatLogInstance.addCombat(damager, damaged);
        combatLogInstance.addCombat(damaged, damager);

        // Agora cancela teleporte com motivo combate
        teleportUtils.cancelarTeleporte(damaged, teleportUtils.CancelReason.COMBAT);
        teleportUtils.cancelarTeleporte(damager, teleportUtils.CancelReason.COMBAT);
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