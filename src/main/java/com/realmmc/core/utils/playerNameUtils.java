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
 * Utilitário para nomes de jogadores com integração LuckPerms.
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
     */
    public static void init(LuckPerms lp) {
        luckPerms = Objects.requireNonNull(lp, "[playerNameUtils] A instância do LuckPerms não pode ser nula");
    }

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

    public static void clearCache() {
        formattedNameCache.clear();
    }

    public static void clearCache(UUID playerId) {
        formattedNameCache.remove(playerId);
    }

    /** 
     * Retorna apenas o nome limpo do jogador (sem prefixo/sufixo). Nunca null.
     */
    public static String getCleanName(Player player) {
        if (player == null) return "";
        return player.getName() != null ? player.getName() : "";
    }

    /**
     * Retorna o nome formatado do jogador online (com prefixo/sufixo se houver).
     */
    public static String getFormattedName(Player player) {
        if (player == null) return "";
        return getFormattedName(player.getUniqueId(), player.getName());
    }

    /**
     * Retorna o nome do jogador já formatado (com prefixo/sufixo se houver).
     * Se não existir LuckPerms para o UUID, retorna nome puro.
     */
    public static String getFormattedName(UUID uuid, String name) {
        if (uuid == null || name == null) return name != null ? name : "";
        return formattedNameCache.computeIfAbsent(uuid, id -> {
            User user = luckPerms != null ? luckPerms.getUserManager().getUser(id) : null;
            if (user == null) return name;
            String prefix = user.getCachedData().getMetaData().getPrefix();
            String suffix = user.getCachedData().getMetaData().getSuffix();
            return (prefix != null ? prefix : "") + name + (suffix != null ? suffix : "");
        });
    }

    /**
     * Busca nome formatado para jogador offline.
     * - Se jogador nunca entrou: retorna nome puro.
     * - Se já jogou, tenta buscar prefixo/sufixo via LuckPerms.
     * - Se online, utiliza getFormattedName(Player).
     */
    public static CompletableFuture<String> getFormattedOfflineName(String name) {
        if (name == null) return CompletableFuture.completedFuture("");
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) return CompletableFuture.completedFuture(getFormattedName(onlinePlayer));

        Optional<UUID> uuidOpt = getPlayerUUID(name);
        if (!uuidOpt.isPresent()) return CompletableFuture.completedFuture(name);

        UUID uuid = uuidOpt.get();
        if (formattedNameCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(formattedNameCache.get(uuid));
        }
        return CompletableFuture.supplyAsync(() -> {
            if (formattedNameCache.containsKey(uuid)) {
                return formattedNameCache.get(uuid);
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if ((offlinePlayer.getName() == null || offlinePlayer.getName().isEmpty())
                    && !offlinePlayer.hasPlayedBefore()) {
                // Jogador nunca visto
                return name;
            }
            return getFormattedName(uuid, offlinePlayer.getName() != null ? offlinePlayer.getName() : name);
        });
    }

    /**
     * Tenta pegar só o nome limpo de um jogador offline (sem prefixo/sufixo).
     * Retorna "" se não encontrar.
     */
    public static String getCleanOfflineName(String name) {
        if (name == null) return "";
        Player onlinePlayer = Bukkit.getPlayerExact(name);
        if (onlinePlayer != null) return onlinePlayer.getName();
        Optional<UUID> uuidOpt = getPlayerUUID(name);
        if (uuidOpt.isPresent()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuidOpt.get());
            String offlineName = offlinePlayer.getName();
            if (offlineName != null && !offlineName.isEmpty()) return offlineName;
        }
        return name;
    }

    /**
     * Retorna um nome puro para jogador inexistente (garante nunca retornar prefixo/sufixo).
     */
    public static String getNonExistentPlayerName(String name) {
        return name == null ? "" : name;
    }

    /**
     * Obtém um jogador online pelo nome (case sensitive).
     */
    public static Player getOnlinePlayer(String name) {
        return Bukkit.getPlayerExact(name);
    }

    /**
     * Tenta obter UUID de jogador pelo nome.
     */
    private static Optional<UUID> getPlayerUUID(String name) {
        if (name == null) return Optional.empty();
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
        // hasPlayedBefore ou isOnline = true indica que existe algum dado
        return (offlinePlayer.hasPlayedBefore() || offlinePlayer.isOnline())
                ? Optional.of(offlinePlayer.getUniqueId())
                : Optional.empty();
    }
}