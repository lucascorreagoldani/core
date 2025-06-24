package com.realmmc.core.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

/**
 * Gerenciador central de warps do servidor.
 * Responsável por buscar, criar, atualizar e validar warps.
 * 
 * @author github.com/lucascorreagoldani
 */
public class warpManager {
    private final FileConfiguration config;

    public warpManager(FileConfiguration config) {
        this.config = config;
    }

    /**
     * Busca a localização de uma warp pelo nome interno.
     */
    public Location getWarpLocation(String warpName) {
        String path = "warps." + warpName.toLowerCase();
        if (!config.contains(path + ".world")) return null;
        World world = Bukkit.getWorld(config.getString(path + ".world"));
        if (world == null) return null;
        return new Location(
            world,
            config.getDouble(path + ".x"),
            config.getDouble(path + ".y"),
            config.getDouble(path + ".z"),
            (float) config.getDouble(path + ".yaw"),
            (float) config.getDouble(path + ".pitch")
        );
    }

    /**
     * Busca o nome interno de uma warp a partir do nome digitado ou displayName.
     */
    public String getInternalWarpName(String identifier) {
        String id = identifier.toLowerCase();
        if (config.contains("warps." + id)) return id;
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section == null) return null;
        for (String key : section.getKeys(false)) {
            String display = config.getString("warps." + key + ".display");
            if (display != null && display.equalsIgnoreCase(identifier)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Retorna o displayName ou o nome interno caso não exista.
     */
    public String getDisplayName(String warpName) {
        String display = config.getString("warps." + warpName + ".display");
        return display != null ? display : warpName;
    }

    /**
     * Retorna a permissão da warp (null se livre, ou core.champion para VIP).
     */
    public String getPermission(String warpName) {
        if ("vip".equalsIgnoreCase(warpName)) {
            return "core.champion";
        }
        return config.getString("warps." + warpName + ".permission");
    }

    /**
     * Lista todos os nomes internos de warp existentes.
     */
    public Set<String> getAllWarpKeys() {
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section == null) return Collections.emptySet();
        return section.getKeys(false);
    }

    /**
     * Cria ou atualiza uma warp.
     */
    public void setWarp(String warpName, String displayName, String permission, Location location) {
        String path = "warps." + warpName;
        config.set(path + ".world", location.getWorld().getName());
        config.set(path + ".x", location.getX());
        config.set(path + ".y", location.getY());
        config.set(path + ".z", location.getZ());
        config.set(path + ".yaw", location.getYaw());
        config.set(path + ".pitch", location.getPitch());
        config.set(path + ".display", displayName);
        config.set(path + ".permission", permission);
    }

    /**
     * Verifica se já existe displayName em outra warp.
     */
    public String existsDisplayName(String displayName, String warpName) {
        ConfigurationSection section = config.getConfigurationSection("warps");
        if (section == null) return null;
        for (String key : section.getKeys(false)) {
            String display = config.getString("warps." + key + ".display");
            if (display != null && display.equalsIgnoreCase(displayName) && !key.equalsIgnoreCase(warpName)) {
                return key;
            }
        }
        return null;
    }
}