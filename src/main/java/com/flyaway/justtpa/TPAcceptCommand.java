package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TPAcceptCommand implements CommandExecutor {
    private final JustTPA plugin;

    public TPAcceptCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
            return true;
        }

        if (!target.hasPermission("justtpa.tpaccept")) {
            target.sendMessage(Component.text("У вас нет прав для использования этой команды!", NamedTextColor.RED));
            return true;
        }

        // 🔴 ДОБАВЛЯЕМ ОБРАБОТКУ КОМАНДЫ БЕЗ АРГУМЕНТОВ
        if (args.length == 0) {
            // Ищем последний/активный запрос для этого игрока
            JustTPA.TPARequest request = findLatestRequest(target);
            if (request == null) {
                target.sendMessage(Component.text("У вас нет активных запросов на телепортацию!", NamedTextColor.RED));
                return true;
            }

            return processRequest(request, target);
        }

        if (args.length != 1) {
            target.sendMessage(Component.text("Использование: /tpaccept [ник]", NamedTextColor.RED));
            return true;
        }

        // Оригинальная логика с аргументом
        JustTPA.TPARequest request = plugin.getPendingRequests().get(target.getUniqueId());

        if (request == null || request.isExpired()) {
            target.sendMessage(Component.text("Запрос на телепортацию не найден или истек!", NamedTextColor.RED));
            if (request != null) {
                plugin.getPendingRequests().remove(target.getUniqueId());
            }
            return true;
        }

        // Проверяем, что запрос от указанного игрока
        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer == null || !senderPlayer.getName().equalsIgnoreCase(args[0])) {
            target.sendMessage(Component.text("Запрос от указанного игрока не найден!", NamedTextColor.RED));
            return true;
        }

        return processRequest(request, target);
    }

    // ПОИСК ПОСЛЕДНЕГО ЗАПРОСА
    private JustTPA.TPARequest findLatestRequest(Player target) {
        JustTPA.TPARequest latestRequest = null;
        long latestTimestamp = 0;

        for (JustTPA.TPARequest request : plugin.getPendingRequests().values()) {
            if (request.getTarget().equals(target.getUniqueId()) && !request.isExpired()) {
                if (request.getTimestamp() > latestTimestamp) {
                    latestTimestamp = request.getTimestamp();
                    latestRequest = request;
                }
            }
        }

        return latestRequest;
    }

    private boolean processRequest(JustTPA.TPARequest request, Player target) {
        if (request.isExpired()) {
            target.sendMessage(Component.text("Запрос на телепортацию истек!", NamedTextColor.RED));
            plugin.getPendingRequests().remove(target.getUniqueId());
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer == null) {
            target.sendMessage(Component.text("Игрок вышел с сервера!", NamedTextColor.RED));
            plugin.getPendingRequests().remove(target.getUniqueId());
            return true;
        }

        try {
            if (request.isTPHere()) {
                // 🔴 ТЕЛЕПОРТАЦИЯ ДЛЯ TPHERE: получатель -> отправитель
                boolean teleportSuccess = target.teleport(senderPlayer.getLocation());

                if (!teleportSuccess) {
                    throw new Exception("Teleport failed");
                }

                // Уведомления для tphere
                target.sendMessage(Component.text()
                        .append(Component.text("✓ ", NamedTextColor.GREEN))
                        .append(Component.text("Вы приняли запрос телепортации к игроку ", NamedTextColor.GRAY))
                        .append(Component.text(senderPlayer.getName(), NamedTextColor.YELLOW))
                        .build());

                senderPlayer.sendMessage(Component.text()
                        .append(Component.text("✓ ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" принял ваш запрос телепортации к вам!", NamedTextColor.GRAY))
                        .build());

            } else {
                // 🔴 ТЕЛЕПОРТАЦИЯ ДЛЯ TPA: отправитель -> получатель
                boolean teleportSuccess = senderPlayer.teleport(target.getLocation());

                if (!teleportSuccess) {
                    throw new Exception("Teleport failed");
                }

                // Уведомления для tpa
                target.sendMessage(Component.text()
                        .append(Component.text("✓ ", NamedTextColor.GREEN))
                        .append(Component.text("Вы приняли запрос телепортации от ", NamedTextColor.GRAY))
                        .append(Component.text(senderPlayer.getName(), NamedTextColor.YELLOW))
                        .build());

                senderPlayer.sendMessage(Component.text()
                        .append(Component.text("✓ ", NamedTextColor.GREEN))
                        .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                        .append(Component.text(" принял ваш запрос телепортации!", NamedTextColor.GRAY))
                        .build());
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при телепортации: " + e.getMessage());
            target.sendMessage(Component.text("Ошибка при телепортации!", NamedTextColor.RED));
            return true;
        }

        // Удаляем запрос
        plugin.getPendingRequests().remove(target.getUniqueId());
        return true;
    }
}
