package com.flyaway.justtpa;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;

        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public String getMessage(String key) {
        String message = config.getString("messages." + key, "<red>message." + key + " not-found");
        return message.replaceAll("\\s+$", "");
    }

    public String getPrefix() {
        return config.getString("prefix", "<gray>[<aqua>JustTPA</aqua>]</gray>");
    }

    public FileConfiguration getConfig() {
        return config;
    }
}
