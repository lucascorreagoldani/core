package com.realmmc.core.utils;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class playerNameUtils {

    private static LuckPerms luckPerms;
    private static final Map<UUID, String> nameCache = new ConcurrentHashMap<>();
    private static final Map<UUID, String> formattedNameCache = new ConcurrentHashMap<>();
    private static final Map<UUID, String> prefixCache = new ConcurrentHashMap<>();
    private static final Map<UUID, String> suffixCache = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    static {
        // Limpar cache a cada 5 minutos
        scheduler.scheduleAtFixedRate(playerNameUtils::clearCache, 5, 5, TimeUnit.MINUTES);
    }

    private playerNameUtils() {
        throw new AssertionError("Esta classe não deve ser instanciada.");
    }

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
        nameCache.clear();
        formattedNameCache.clear();
        prefixCache.clear();
        suffixCache.clear();
    }

    public static void clearCache(UUID playerId) {
        nameCache.remove(playerId);
        formattedNameCache.remove(playerId);
        prefixCache.remove(playerId);
        suffixCache.remove(playerId);
    }

    public static Player getOnlinePlayer(String name) {
        if (name == null) return null;
        return Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public static boolean playerExists(String name) {
        if (name == null) return false;
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .anyMatch(player -> name.equalsIgnoreCase(player.getName()));
    }

    public static String getCorrectName(String name) {
        if (name == null) return null;
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> name.equalsIgnoreCase(player.getName()))
                .findFirst()
                .map(OfflinePlayer::getName)
                .orElse(name);
    }

    public static Optional<UUID> getPlayerUUID(String name) {
        if (name == null) return Optional.empty();
        return Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> name.equalsIgnoreCase(player.getName()))
                .findFirst()
                .map(OfflinePlayer::getUniqueId);
    }

    public static String getFormattedName(Player player) {
        if (player == null) return "";
        return formattedNameCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            if (luckPerms == null) return player.getName();

            User user = luckPerms.getUserManager().getUser(uuid);
            if (user == null) return player.getName();

            CachedMetaData metaData = user.getCachedData().getMetaData(QueryOptions.nonContextual());
            String prefix = Optional.ofNullable(metaData.getPrefix()).orElse("");
            String suffix = Optional.ofNullable(metaData.getSuffix()).orElse("");

            return ChatColor.translateAlternateColorCodes('&', prefix + player.getName() + suffix);
        });
    }

    public static String getPrefix(Player player) {
        if (player == null) return "";
        return prefixCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            if (luckPerms == null) return "";

            User user = luckPerms.getUserManager().getUser(uuid);
            if (user == null) return "";

            return Optional.ofNullable(user.getCachedData()
                    .getMetaData(QueryOptions.nonContextual())
                    .getPrefix())
                    .orElse("");
        });
    }

    public static String getSuffix(Player player) {
        if (player == null) return "";
        return suffixCache.computeIfAbsent(player.getUniqueId(), uuid -> {
            if (luckPerms == null) return "";

            User user = luckPerms.getUserManager().getUser(uuid);
            if (user == null) return "";

            return Optional.ofNullable(user.getCachedData()
                    .getMetaData(QueryOptions.nonContextual())
                    .getSuffix())
                    .orElse("");
        });
    }

    public static CompletableFuture<String> getFormattedOfflineName(String name) {
        if (name == null) return CompletableFuture.completedFuture("");

        // Tenta encontrar o jogador online primeiro
        Player onlinePlayer = getOnlinePlayer(name);
        if (onlinePlayer != null) {
            return CompletableFuture.completedFuture(getFormattedName(onlinePlayer));
        }

        // Verifica o cache
        Optional<UUID> uuidOpt = getPlayerUUID(name);
        if (!uuidOpt.isPresent()) {
            return CompletableFuture.completedFuture(name);
        }

        UUID uuid = uuidOpt.get();
        if (formattedNameCache.containsKey(uuid)) {
            return CompletableFuture.completedFuture(formattedNameCache.get(uuid));
        }

        return CompletableFuture.supplyAsync(() -> {
            // Tenta carregar do cache novamente (pode ter sido carregado em outra thread)
            if (formattedNameCache.containsKey(uuid)) {
                return formattedNameCache.get(uuid);
            }

            // Carrega o jogador offline
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                return name;
            }

            // Usa o LuckPerms para obter o nome formatado
            if (luckPerms != null) {
                User user = luckPerms.getUserManager().loadUser(uuid).join();
                if (user != null) {
                    CachedMetaData metaData = user.getCachedData().getMetaData(QueryOptions.nonContextual());
                    String prefix = Optional.ofNullable(metaData.getPrefix()).orElse("");
                    String suffix = Optional.ofNullable(metaData.getSuffix()).orElse("");
                    String playerName = Optional.ofNullable(offlinePlayer.getName()).orElse(name);
                    String formatted = ChatColor.translateAlternateColorCodes('&', prefix + playerName + suffix);
                    formattedNameCache.put(uuid, formatted);
                    return formatted;
                }
            }

            return Optional.ofNullable(offlinePlayer.getName()).orElse(name);
        }).exceptionally(ex -> {
            ex.printStackTrace();
            return name;
        });
    }

    private static org.bukkit.plugin.Plugin pluginInstance;

    public static void setPluginInstance(org.bukkit.plugin.Plugin plugin) {
        pluginInstance = plugin;
    }

    public static void getFormattedOfflineNameAsync(String name, Consumer<String> callback) {
        if (pluginInstance == null) {
            throw new IllegalStateException("Plugin instance not set. Call playerNameUtils.setPluginInstance(yourPlugin) first.");
        }
        getFormattedOfflineName(name).thenAcceptAsync(callback, Bukkit.getScheduler().getMainThreadExecutor(pluginInstance));
    }

    public static CompletableFuture<String> getFormattedNameWithPlaceholders(Player player, String placeholderFormat) {
        if (player == null) return CompletableFuture.completedFuture("");
        return CompletableFuture.supplyAsync(() -> {
            String formattedName = getFormattedName(player);
            return placeholderFormat
                    .replace("{name}", player.getName())
                    .replace("{displayname}", player.getDisplayName())
                    .replace("{formatted}", formattedName)
                    .replace("{prefix}", getPrefix(player))
                    .replace("{suffix}", getSuffix(player));
        });
    }

    public static List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    public static List<String> getOnlinePlayerFormattedNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(playerNameUtils::getFormattedName)
                .collect(Collectors.toList());
    }

    public static Map<UUID, String> getAllCachedNames() {
        return Collections.unmodifiableMap(nameCache);
    }

    public static Map<UUID, String> getAllCachedFormattedNames() {
        return Collections.unmodifiableMap(formattedNameCache);
    }

    public static String getBestName(OfflinePlayer player) {
        if (player == null) return "null";
        if (player.isOnline()) {
            return getFormattedName(player.getPlayer());
        }
        return Optional.ofNullable(player.getName()).orElse("Desconhecido");
    }
}