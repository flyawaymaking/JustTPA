package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TPDenyCommand implements CommandExecutor {
    private final JustTPA plugin;

    public TPDenyCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
            return true;
        }

        Player target = (Player) sender;

        if (!target.hasPermission("justtpa.tpdeny")) {
            target.sendMessage(Component.text("У вас нет прав для использования этой команды!", NamedTextColor.RED));
            return true;
        }

        // Обработка команды без аргументов
        if (args.length == 0) {
            JustTPA.TPARequest request = findLatestRequest(target);
            if (request == null) {
                target.sendMessage(Component.text("У вас нет активных запросов на телепортацию!", NamedTextColor.RED));
                return true;
            }

            return processDeny(request, target);
        }

        if (args.length != 1) {
            target.sendMessage(Component.text("Использование: /tpdeny [ник]", NamedTextColor.RED));
            return true;
        }

        // Обработка команды с указанием ника
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

        return processDeny(request, target);
    }

    // Поиск последнего запроса для получателя
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

    // Обработка отклонения запроса
    private boolean processDeny(JustTPA.TPARequest request, Player target) {
        Player senderPlayer = Bukkit.getPlayer(request.getSender());

        // Уведомления
        target.sendMessage(Component.text()
                .append(Component.text("✕ ", NamedTextColor.RED))
                .append(Component.text("Вы отклонили запрос телепортации от ", NamedTextColor.GRAY))
                .append(Component.text(senderPlayer != null ? senderPlayer.getName() : "игрока", NamedTextColor.YELLOW))
                .build());

        if (senderPlayer != null) {
            senderPlayer.sendMessage(Component.text()
                    .append(Component.text("✕ ", NamedTextColor.RED))
                    .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" отклонил ваш запрос телепортации!", NamedTextColor.GRAY))
                    .build());
        }

        // Удаляем запрос
        plugin.getPendingRequests().remove(target.getUniqueId());

        return true;
    }
}
