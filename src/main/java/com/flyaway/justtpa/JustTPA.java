package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JustTPA extends JavaPlugin {

    private final Map<UUID, TPARequest> pendingRequests = new HashMap<>();
    private BukkitTask cleanupTask;
    private ConfigManager configManager;
    private MiniMessage miniMessage;

    @Override
    public void onEnable() {
        // Инициализация MiniMessage и ConfigManager
        this.miniMessage = MiniMessage.miniMessage();
        this.configManager = new ConfigManager(this);

        // Регистрируем команды
        TPACommand tpaCommand = new TPACommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        TPAHereCommand tpahereCommand = new TPAHereCommand(this);
        getCommand("tpahere").setExecutor(tpahereCommand);
        getCommand("tpahere").setTabCompleter(tpahereCommand);

        getCommand("tpaccept").setExecutor(new TPAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TPDenyCommand(this));
        getCommand("tpcancel").setExecutor(new TPCancelCommand(this));
        getCommand("justtpa").setExecutor(new JustTPAReloadCommand(this));

        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredRequests();
            }
        }.runTaskTimer(this, 1200L, 1200L);

        getLogger().info("JustTPA успешно запущен!");
    }

    @Override
    public void onDisable() {
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
        }
        int requestsCount = pendingRequests.size();
        pendingRequests.clear();
        getLogger().info("JustTPA выключен! Очищено " + requestsCount + " запросов TPA");
    }

    private void cleanupExpiredRequests() {
        for (Map.Entry<UUID, TPARequest> entry : pendingRequests.entrySet()) {
            if (entry.getValue().isExpired()) {
                pendingRequests.remove(entry.getKey());

                Player target = Bukkit.getPlayer(entry.getKey());
                Player sender = Bukkit.getPlayer(entry.getValue().getSender());

                if (target != null) {
                    sendMessage(target, getMessage("request.expired.target"));
                }
                if (sender != null) {
                    sendMessage(sender, getMessage("request.expired.sender"));
                }
            }
        }
    }

    public Component getMessage(String key) {
        return miniMessage.deserialize(configManager.getMessage(key));
    }

    public void sendMessage(CommandSender sender, Component message) {
        Component prefix = miniMessage.deserialize(configManager.getPrefix() + " ");
        sender.sendMessage(prefix.append(message));
    }

    public Map<UUID, TPARequest> getPendingRequests() {
        return pendingRequests;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    // Класс для хранения запросов на телепортацию
    public static class TPARequest {
        private final UUID sender;
        private final UUID target;
        private final long timestamp;
        private final boolean isTPHere;

        public TPARequest(UUID sender, UUID target) {
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
            this.isTPHere = false;
        }

        public TPARequest(UUID sender, UUID target, boolean isTPHere) {
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
            this.isTPHere = isTPHere;
        }

        public UUID getSender() {
            return sender;
        }

        public UUID getTarget() {
            return target;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000; // 60 секунд
        }

        public boolean isTPHere() {
            return isTPHere;
        }
    }
}
