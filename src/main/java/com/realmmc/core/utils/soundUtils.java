package com.realmmc.core.utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.World;

public final class soundUtils {

    private soundUtils() {}

    private static final float VOLUME_PADRAO = 1.0f;
    private static final float TOM_SUCESSO = 2.0f;
    private static final float TOM_ERRO = 1.0f;
    private static final float TOM_CONTAGEM = 2.0f;
    private static final float VOLUME_TICK = 0.7f;
    private static final float TOM_TICK = 1.8f;

    public static void reproduzirSucesso(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, VOLUME_PADRAO, TOM_SUCESSO);
    }

    public static void reproduzirErro(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, VOLUME_PADRAO, TOM_ERRO);
    }

    public static void reproduzirContagem(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, VOLUME_PADRAO, TOM_CONTAGEM);
    }

    public static void reproduzirTick(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.UI_BUTTON_CLICK, VOLUME_TICK, TOM_TICK);
    }

    public static void reproduzir(Player jogador, Sound som, float volume, float tom) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), som, volume, tom);
    }

    public static void reproduzir(Location localizacao, Sound som, float volume, float tom) {
        validarLocalizacao(localizacao);
        World mundo = localizacao.getWorld();
        if (mundo != null) {
            mundo.playSound(localizacao, som, volume, tom);
        }
    }

    private static void validarJogador(Player jogador) {
        if (jogador == null) {
            throw new IllegalArgumentException("[validarJogador] Jogador não pode ser nulo");
        }
    }

    private static void validarLocalizacao(Location localizacao) {
        if (localizacao == null || localizacao.getWorld() == null) {
            throw new IllegalArgumentException("[validarLocalizacao] Localização inválida ou mundo nulo");
        }
    }
}