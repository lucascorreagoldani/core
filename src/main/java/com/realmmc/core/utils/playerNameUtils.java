package com.realmmc.core.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Classe utilitária para manipulação e formatação de nomes de jogadores.
 * Utiliza LuckPerms como base para obter prefixos e sufixos.
 *
 * <p>Recursos:</p>
 * <ul>
 *     <li>Integração com LuckPerms</li>
 *     <li>Cache de nomes formatados</li>
 *     <li>Suporte a prefixos e sufixos</li>
 *     <li>Formatação de nomes offline</li>
 *     <li>Gerenciamento de jogadores nunca vistos</li>
 * </ul>
 *
 * <p>Exemplo de uso:</p>
 * <pre>
 * {@code
 * playerNameUtils.init(luckPermsInstance);
 * String formattedName = playerNameUtils.getFormattedName(player);
 * }
 * </pre>
 *
 * @author Lucas Corrêa
 */
public final class playerNameUtils {

    private static LuckPerms luckPerms;
    private static final Map<UUID, String> formattedNameCache = new ConcurrentHashMap<>();
    private static final ExecutorService scheduler = Executors.newSingleThreadExecutor();

    private playerNameUtils() {
        throw new AssertionError("Esta classe não deve ser instanciada.");
    }

    /**
     * Inicializa a integração com o LuckPerms.
     *
     * @param lp Instância do LuckPerms.
     */
    public static void init(LuckPerms lp) {
        luckPerms = Objects.requireNonNull(lp, "[playerNameUtils] A instância do LuckPerms não pode ser nula");
    }

    /**
     * Finaliza os recursos utilizados.
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Limpa o cache de nomes formatados.
     */
    public static void clearCache() {
        formattedNameCache.clear();
    }

    /**
     * Limpa o cache para um jogador específico.
     *
     * @param playerId UUID do jogador.
     */
    public static void clearCache(UUID playerId) {
        formattedNameCache.remove(playerId);
    }

    /**
     * Obtém o nome formatado de um jogador online.
     *
     * @param player Jogador online.
     * @return Nome formatado.
     */
    public static String getFormattedName(Player player) {
        return getFormattedName(player.getUniqueId(), player.getName());
    }

    /**
     * Obtém o nome formatado de um jogador offline ou que nunca entrou.
     * <p>
     * Se o jogador nunca entrou no servidor (ou seja, não existe UUID conhecido ou nunca jogou),
     * retorna apenas o nome puro informado, sem prefixo ou sufixo.
     * Não tenta buscar dados no LuckPerms para jogadores desconhecidos.
     *
     * @param name Nome do jogador.
     * @return CompletableFuture com o nome formatado, ou apenas o nome caso não exista.
     */
    public static CompletableFuture<String> getFormattedOfflineName(String name) {
        if (name == null) return CompletableFuture.completedFuture("");
        // Tenta encontrar o jogador online primeiro
        Player onlinePlayer = getOnlinePlayer(name);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(getFormattedName(onlinePlayer));
        }
        // Busca o UUID do jogador offline
        Optional<UUID> uuidOpt = getPlayerUUID(name);
        if (!uuidOpt.isPresent()) {
            // Jogador nunca entrou: retorna nome puro, sem prefixo/sufixo
            return CompletableFuture.completedFuture(name);
        }
        UUID uuid = uuidOpt.get();
        if (formattedNameCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(formattedNameCache.get(uuid));
        }
        return CompletableFuture.supplyAsync(() -> {
            if (formattedNameCache.containsKey(uuid)) {
                return formattedNameCache.get(uuid);
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

            // Se o jogador realmente nunca jogou, retorna o nome puro
            if ((offlinePlayer.getName() == null || offlinePlayer.getName().isEmpty()) &&
                !offlinePlayer.hasPlayedBefore()) {
                return name;
            }
            return getFormattedName(uuid, offlinePlayer.getName());
        });
    }

    /**
     * Obtém o nome formatado com base no UUID e nome do jogador.
     *
     * @param uuid UUID do jogador.
     * @param name Nome do jogador.
     * @return Nome formatado.
     */
    private static String getFormattedName(UUID uuid, String name) {
        if (name == null) return "";
        return formattedNameCache.computeIfAbsent(uuid, id -> {
            User user = luckPerms.getUserManager().getUser(id);
            if (user == null) return name;
            String prefix = user.getCachedData().getMetaData().getPrefix();
            String suffix = user.getCachedData().getMetaData().getSuffix();
            return (prefix != null ? prefix : "") +
                   name +
                   (suffix != null ? suffix : "");
        });
    }

    /**
     * Obtém um jogador online pelo nome.
     *
     * @param name Nome do jogador.
     * @return Jogador online, ou null se não encontrado.
     */
    private static Player getOnlinePlayer(String name) {
        return Bukkit.getPlayer(name);
    }

    /**
     * Obtém o UUID de um jogador pelo nome.
     *
     * @param name Nome do jogador.
     * @return Optional com UUID do jogador, ou empty se não encontrado.
     */
    private static Optional<UUID> getPlayerUUID(String name) {
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        // isOnline = true ou hasPlayedBefore = true indica que existe algum dado
        return (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline())
               ? Optional.of(offlinePlayer.getUniqueId())
               : Optional.empty();
    }
}