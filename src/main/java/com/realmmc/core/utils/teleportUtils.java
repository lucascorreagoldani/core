package com.realmmc.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import com.realmmc.core.combatLog.combatLog;

import java.util.*;

public final class teleportUtils {
    private static final String PERMISSAO_BYPASS = "core.vip";
    private static final int TEMPO_COOLDOWN = 5; // segundos

    private static final String MSG_EM_COMBATE = ChatColor.RED + "Você não pode se teleportar em combate!";
    private static final String MSG_TELEPORTE_ATIVO = ChatColor.RED + "Você já tem um teleporte em andamento!";
    private static final String MSG_COOLDOWN = ChatColor.RED + "Aguarde %d segundos para se teleportar novamente!";
    private static final String MSG_CANCELADO_MOVIMENTO = ChatColor.RED + "Seu teleporte foi cancelado porque você se moveu!";
    private static final String MSG_CANCELADO_COMBATE = ChatColor.RED + "Seu teleporte foi cancelado porque você entrou em combate!";
    private static final String MSG_DELAY = ChatColor.YELLOW + "Você será teleportado em %d segundos. Não se mova!";
    private static final String MSG_SUCESSO = ChatColor.GREEN + "Teleportado com sucesso!";

    // Jogadores aguardando teleporte
    private static final Set<UUID> teleportando = Collections.synchronizedSet(new HashSet<>());
    // Cooldowns de teleporte
    private static final Map<UUID, Long> cooldowns = Collections.synchronizedMap(new HashMap<>());
    // Bloco inicial de cada teleporte
    private static final Map<UUID, Location> blocosIniciais = Collections.synchronizedMap(new HashMap<>());

    private teleportUtils() { throw new AssertionError("Não instancie!"); }

    public static void iniciarTeleporte(Plugin plugin, Player jogador, Location destino, combatLog combatLog) {
        UUID uuid = jogador.getUniqueId();

        // 1. Verifica se está em combate
        if (combatLog != null && combatLog.isPlayerInCombat(uuid)) {
            jogador.sendMessage(MSG_EM_COMBATE);
            soundUtils.reproduzirErro(jogador);
            return;
        }

        // 2. Verifica teleporte ativo
        if (teleportando.contains(uuid)) {
            jogador.sendMessage(MSG_TELEPORTE_ATIVO);
            soundUtils.reproduzirErro(jogador);
            return;
        }

        // 3. Verifica cooldown (se não for VIP)
        if (!jogador.hasPermission(PERMISSAO_BYPASS)) {
            long agora = System.currentTimeMillis();
            if (cooldowns.containsKey(uuid)) {
                long restante = (cooldowns.get(uuid) - agora) / 1000;
                if (restante > 0) {
                    jogador.sendMessage(String.format(MSG_COOLDOWN, restante));
                    soundUtils.reproduzirErro(jogador);
                    return;
                }
            }
        }

        // 4. Salva o bloco inicial (apenas X/Y/Z inteiros, ignora rotação)
        Location blocoInicial = jogador.getLocation().clone();
        blocoInicial.setX(Math.floor(blocoInicial.getX()));
        blocoInicial.setY(Math.floor(blocoInicial.getY()));
        blocoInicial.setZ(Math.floor(blocoInicial.getZ()));
        blocosIniciais.put(uuid, blocoInicial);
        teleportando.add(uuid);

        jogador.sendMessage(String.format(MSG_DELAY, TEMPO_COOLDOWN));
        soundUtils.reproduzirContagem(jogador);

        // 5. Espera o delay e verifica se o jogador não andou de bloco
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!teleportando.contains(uuid)) return; // já cancelado

                Location atual = jogador.getLocation().clone();
                atual.setX(Math.floor(atual.getX()));
                atual.setY(Math.floor(atual.getY()));
                atual.setZ(Math.floor(atual.getZ()));

                Location inicial = blocosIniciais.get(uuid);

                if (!mesmoBloco(atual, inicial)) {
                    cancelarTeleporte(jogador, CancelReason.MOVEMENT);
                    return;
                }

                // Teleporta
                teleportando.remove(uuid);
                blocosIniciais.remove(uuid);
                jogador.teleport(destino);
                jogador.sendMessage(MSG_SUCESSO);
                soundUtils.reproduzirSucesso(jogador);

                // Define cooldown se não for VIP
                if (!jogador.hasPermission(PERMISSAO_BYPASS)) {
                    cooldowns.put(uuid, System.currentTimeMillis() + (TEMPO_COOLDOWN * 1000L));
                }
            }
        }.runTaskLater(plugin, TEMPO_COOLDOWN * 20L);
    }

    /** Motivos possíveis de cancelamento */
    public enum CancelReason {
        MOVEMENT, COMBAT
    }

    public static void cancelarTeleporte(Player jogador) {
        cancelarTeleporte(jogador, CancelReason.MOVEMENT);
    }

    public static void cancelarTeleporte(Player jogador, CancelReason reason) {
        UUID uuid = jogador.getUniqueId();
        if (teleportando.contains(uuid)) {
            teleportando.remove(uuid);
            blocosIniciais.remove(uuid);
            if (reason == CancelReason.COMBAT) {
                jogador.sendMessage(MSG_CANCELADO_COMBATE);
            } else {
                jogador.sendMessage(MSG_CANCELADO_MOVIMENTO);
            }
            soundUtils.reproduzirErro(jogador);
        }
    }

    public static boolean estaTeleportando(Player jogador) {
        return teleportando.contains(jogador.getUniqueId());
    }

    private static boolean mesmoBloco(Location loc1, Location loc2) {
        return loc1.getWorld().equals(loc2.getWorld())
                && loc1.getBlockX() == loc2.getBlockX()
                && loc1.getBlockY() == loc2.getBlockY()
                && loc1.getBlockZ() == loc2.getBlockZ();
    }
}