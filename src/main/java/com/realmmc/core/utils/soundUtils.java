package com.realmmc.core.utils;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Classe utilitária para reprodução de sons customizados para jogadores e locais no servidor.
 * Segue padrão de métodos estáticos, validação e documentação explícita dos parâmetros.
 *
 * <p>Recursos:</p>
 * <ul>
 *     <li>Reprodução de sons de sucesso, erro, contagem e tick para jogadores</li>
 *     <li>Reprodução de qualquer som customizado para jogador ou localização</li>
 *     <li>Validação de parâmetros para evitar exceções</li>
 * </ul>
 *
 * <p>Exemplo de uso:</p>
 * <pre>
 * {@code
 * soundUtils.reproduzirSucesso(player);
 * soundUtils.reproduzir(player, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.5f);
 * }
 * </pre>
 *
 * @author Lucas Corrêa
 */
public final class soundUtils {

    // Constantes de volume e tom padrão para sons específicos
    private static final float VOLUME_PADRAO = 1.0f;
    private static final float TOM_SUCESSO = 2.0f;
    private static final float TOM_ERRO = 1.0f;
    private static final float TOM_CONTAGEM = 2.0f;
    private static final float VOLUME_TICK = 0.7f;
    private static final float TOM_TICK = 1.8f;

    /**
     * Construtor privado para evitar instanciação.
     */
    private soundUtils() {
        throw new AssertionError("Esta classe não deve ser instanciada.");
    }

    /**
     * Reproduz um som de sucesso para o jogador (ex: ação bem-sucedida).
     * @param jogador Jogador alvo
     */
    public static void reproduzirSucesso(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, VOLUME_PADRAO, TOM_SUCESSO);
    }

    /**
     * Reproduz um som de erro para o jogador (ex: ação inválida).
     * @param jogador Jogador alvo
     */
    public static void reproduzirErro(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, VOLUME_PADRAO, TOM_ERRO);
    }

    /**
     * Reproduz um som de contagem para o jogador (ex: contagem regressiva).
     * @param jogador Jogador alvo
     */
    public static void reproduzirContagem(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, VOLUME_PADRAO, TOM_CONTAGEM);
    }

    /**
     * Reproduz um som de tick para o jogador (ex: feedback rápido).
     * @param jogador Jogador alvo
     */
    public static void reproduzirTick(Player jogador) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), Sound.UI_BUTTON_CLICK, VOLUME_TICK, TOM_TICK);
    }

    /**
     * Reproduz qualquer som customizado para o jogador em sua localização atual.
     * @param jogador Jogador alvo
     * @param som Tipo de som (enum do Bukkit)
     * @param volume Volume do som
     * @param tom Tom/pitch do som
     */
    public static void reproduzir(Player jogador, Sound som, float volume, float tom) {
        validarJogador(jogador);
        jogador.playSound(jogador.getLocation(), som, volume, tom);
    }

    /**
     * Reproduz qualquer som customizado em uma localização específica do mundo.
     * @param localizacao Local de reprodução
     * @param som Tipo de som (enum do Bukkit)
     * @param volume Volume do som
     * @param tom Tom/pitch do som
     */
    public static void reproduzir(Location localizacao, Sound som, float volume, float tom) {
        validarLocalizacao(localizacao);
        World mundo = localizacao.getWorld();
        if (mundo != null) {
            mundo.playSound(localizacao, som, volume, tom);
        }
    }

    /**
     * Valida se o jogador não é nulo.
     * @param jogador Jogador a ser validado
     * @throws IllegalArgumentException se o jogador for nulo
     */
    private static void validarJogador(Player jogador) {
        if (jogador == null) {
            throw new IllegalArgumentException("[validarJogador] Jogador não pode ser nulo");
        }
    }

    /**
     * Valida se a localização e o mundo não são nulos.
     * @param localizacao Localização a ser validada
     * @throws IllegalArgumentException se a localização ou o mundo forem nulos
     */
    private static void validarLocalizacao(Location localizacao) {
        if (localizacao == null || localizacao.getWorld() == null) {
            throw new IllegalArgumentException("[validarLocalizacao] Localização inválida ou mundo nulo");
        }
    }
}