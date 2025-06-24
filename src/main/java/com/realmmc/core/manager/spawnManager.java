package com.realmmc.core.manager;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Objects;

/**
 * Gerencia o spawn do servidor: definição e obtenção.
 */
public class spawnManager {
    private final FileConfiguration config;

    public spawnManager(FileConfiguration config) {
        this.config = Objects.requireNonNull(config);
    }

    /**
     * Obtém a localização do spawn se configurado e válida.
     * @return Localização do spawn, ou null se não configurado/corrompido.
     */
    public Location getSpawnLocation() {
        if (!config.contains("spawn.world")) return null;
        String worldName = config.getString("spawn.world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;
        return new Location(
                world,
                config.getDouble("spawn.x"),
                config.getDouble("spawn.y"),
                config.getDouble("spawn.z"),
                (float) config.getDouble("spawn.yaw"),
                (float) config.getDouble("spawn.pitch")
        );
    }

    /**
     * Define o spawn no local fornecido.
     */
    public void setSpawn(Location location) {
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
    }

    /**
     * Verifica se o spawn está configurado.
     */
    public boolean isSpawnSet() {
        return config.contains("spawn.world");
    }
}