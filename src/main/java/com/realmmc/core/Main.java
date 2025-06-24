package com.realmmc.core;

import com.realmmc.core.combatLog.*;
import com.realmmc.core.commands.*;
import com.realmmc.core.listeners.*;
import com.realmmc.core.utils.playerNameUtils;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Level;

public final class Main extends JavaPlugin {
    private FileConfiguration config;

    @Override
    public void onEnable() {
        getLogger().info("§a[Essentials] Iniciando plugin...");
        try {
            if (!getDataFolder().exists()) getDataFolder().mkdirs();
            carregarConfiguracao();
            inicializarLuckPerms();
            registrarComandos();
            registrarListeners();
            getLogger().info("Iniciando sistemas...");
            inicializarSistemas();
            getLogger().info("§a[Essentials] Plugin habilitado com sucesso!");
        } catch (Exception e) {
            getLogger().severe("§c[Essentials] Erro crítico durante a inicialização!");
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("§a[Essentials] Plugin desabilitado.");
        playerNameUtils.shutdown(); // Garante o shutdown correto
    }

    private void carregarConfiguracao() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (InputStream in = getResource("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                } else {
                    configFile.createNewFile();
                }
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Falha ao criar config.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void inicializarLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") != null) {
            RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
            if (provider != null) {
                playerNameUtils.init(provider.getProvider());
                playerNameUtils.setPluginInstance(this); // Configura a instância do plugin
            }
        }
    }

    private void registrarComandos() {
        try {
            getCommand("gamemode").setExecutor(new gamemode());
            getCommand("combatstats").setExecutor(new combatStats());
            getCommand("setwarp").setExecutor(new setwarp(config, this));
            getCommand("delwarp").setExecutor(new delwarp(config, this));
            getCommand("setspawn").setExecutor(new setspawn(config, this));
            
            // REGISTRO CORRETO DO TELEPORTE
            getCommand("tp").setExecutor(new teleport(this)); // Passa a instância do plugin

            spawn spawnCommand = new spawn(config, this);
            getCommand("spawn").setExecutor(spawnCommand);
            getServer().getPluginManager().registerEvents(spawnCommand, this);

            warp warpCommand = new warp(config, this);
            getCommand("warp").setExecutor(warpCommand);
            getServer().getPluginManager().registerEvents(warpCommand, this);
        } catch (Exception e) {
            getLogger().severe("Erro ao registrar comandos: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void registrarListeners() {
        try {
            getServer().getPluginManager().registerEvents(new combatLog(this), this);
            getServer().getPluginManager().registerEvents(new playerJoinListener(this), this);
        } catch (Exception e) {
            getLogger().severe("Erro ao registrar listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void inicializarSistemas() {
        combatStatsManager.getInstance();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public void saveConfig() {
        try {
            config.save(new File(getDataFolder(), "config.yml"));
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Falha ao salvar config.yml", e);
        }
    }
}