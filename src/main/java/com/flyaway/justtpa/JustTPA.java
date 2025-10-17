package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JustTPA extends JavaPlugin {

    private final Map<UUID, TPARequest> pendingRequests = new HashMap<>();
    private BukkitTask cleanupTask; // Добавляем поле для хранения задачи

    @Override
    public void onEnable() {
        // Регистрируем команды TPA
        TPACommand tpaCommand = new TPACommand(this);
        getCommand("tpa").setExecutor(tpaCommand);
        getCommand("tpa").setTabCompleter(tpaCommand);

        // РЕГИСТРИРУЕМ TPHERE КОМАНДУ
        TPAHereCommand tpahereCommand = new TPAHereCommand(this);
        getCommand("tpahere").setExecutor(tpahereCommand); //
        getCommand("tpahere").setTabCompleter(tpahereCommand);

        // Общие команды для обоих типов запросов
        getCommand("tpaccept").setExecutor(new TPAcceptCommand(this));
        getCommand("tpdeny").setExecutor(new TPDenyCommand(this));
        getCommand("tpcancel").setExecutor(new TPCancelCommand(this));

        this.cleanupTask = new BukkitRunnable() {
            @Override
            public void run() {
                cleanupExpiredRequests();
            }
        }.runTaskTimer(this, 1200L, 1200L); // Каждую минуту

        getLogger().info("JustTPA успешно запущен!");
    }

    @Override
    public void onDisable() {
        // Отменяем задачу очистки
        if (cleanupTask != null && !cleanupTask.isCancelled()) {
            cleanupTask.cancel();
            cleanupTask = null;
            getLogger().info("Задача очистки отменена");
        }

        // Очищаем все pending requests
        int requestsCount = pendingRequests.size();
        pendingRequests.clear();

        getLogger().info("JustTPA выключен! Очищено " + requestsCount + " запросов TPA");
    }

    private void cleanupExpiredRequests() {
        int cleaned = 0;
        for (Map.Entry<UUID, TPARequest> entry : pendingRequests.entrySet()) {
            if (entry.getValue().isExpired()) {
                pendingRequests.remove(entry.getKey());
                cleaned++;

                // Уведомляем игроков об истечении времени
                Player target = Bukkit.getPlayer(entry.getKey());
                Player sender = Bukkit.getPlayer(entry.getValue().getSender());

                if (target != null) {
                    target.sendMessage(Component.text()
                            .append(Component.text("⏰ ", NamedTextColor.YELLOW))
                            .append(Component.text("Запрос на телепортацию истек!", NamedTextColor.GRAY))
                            .build());
                }

                if (sender != null) {
                    sender.sendMessage(Component.text()
                            .append(Component.text("⏰ ", NamedTextColor.YELLOW))
                            .append(Component.text("Ваш запрос телепортации истек!", NamedTextColor.GRAY))
                            .build());
                }
            }
        }
        if (cleaned > 0) {
            getLogger().info("Очищено " + cleaned + " просроченных запросов TPA");
        }
    }

    public Map<UUID, TPARequest> getPendingRequests() {
        return pendingRequests;
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
            this.isTPHere = false; // По умолчанию обычный tpa
        }

        public TPARequest(UUID sender, UUID target, boolean isTPHere) {
            this.sender = sender;
            this.target = target;
            this.timestamp = System.currentTimeMillis();
            this.isTPHere = isTPHere; // Указываем тип запроса
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
