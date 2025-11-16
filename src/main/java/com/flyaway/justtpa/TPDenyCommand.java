package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPDenyCommand implements CommandExecutor {
    private final JustTPA plugin;

    public TPDenyCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            plugin.sendMessage(sender, plugin.getMessage("error.only-players"));
            return true;
        }

        if (!target.hasPermission("justtpa.tpdeny")) {
            plugin.sendMessage(target, plugin.getMessage("error.no-permission"));
            return true;
        }

        // Обработка команды без аргументов
        if (args.length == 0) {
            JustTPA.TPARequest request = findLatestRequest(target);
            if (request == null) {
                plugin.sendMessage(target, plugin.getMessage("error.no-requests"));
                return true;
            }

            return processDeny(request, target);
        }

        if (args.length != 1) {
            plugin.sendMessage(target, plugin.getMessage("command.tpdeny.usage"));
            return true;
        }

        // Обработка команды с указанием ника
        JustTPA.TPARequest request = plugin.getPendingRequests().get(target.getUniqueId());

        if (request == null || request.isExpired()) {
            plugin.sendMessage(target, plugin.getMessage("error.request-not-found"));
            if (request != null) {
                plugin.getPendingRequests().remove(target.getUniqueId());
            }
            return true;
        }

        // Проверяем, что запрос от указанного игрока
        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer == null || !senderPlayer.getName().equalsIgnoreCase(args[0])) {
            plugin.sendMessage(target, plugin.getMessage("error.specific-request-not-found"));
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

        // Уведомление получателю
        if (senderPlayer != null) {
            plugin.sendMessage(target, plugin.getMessage("tpdeny.denied")
                    .replaceText(builder -> builder.matchLiteral("{sender}").replacement(senderPlayer.getName())));
        } else {
            plugin.sendMessage(target, plugin.getMessage("tpdeny.denied-generic"));
        }

        // Уведомление отправителю
        if (senderPlayer != null) {
            plugin.sendMessage(senderPlayer, plugin.getMessage("tpdeny.received")
                    .replaceText(builder -> builder.matchLiteral("{target}").replacement(target.getName())));
        }

        // Удаляем запрос
        plugin.getPendingRequests().remove(target.getUniqueId());

        return true;
    }
}
