package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPAcceptCommand implements CommandExecutor {
    private final JustTPA plugin;

    public TPAcceptCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player target)) {
            plugin.sendMessage(sender, plugin.getMessage("error.only-players"));
            return true;
        }

        if (!target.hasPermission("justtpa.tpaccept")) {
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
            return processRequest(request, target);
        }

        if (args.length != 1) {
            plugin.sendMessage(target, plugin.getMessage("command.tpaccept.usage"));
            return true;
        }

        // Оригинальная логика с аргументом
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

        return processRequest(request, target);
    }

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
            plugin.sendMessage(target, plugin.getMessage("error.request-expired"));
            plugin.getPendingRequests().remove(target.getUniqueId());
            return true;
        }

        Player senderPlayer = Bukkit.getPlayer(request.getSender());
        if (senderPlayer == null) {
            plugin.sendMessage(target, plugin.getMessage("error.sender-offline"));
            plugin.getPendingRequests().remove(target.getUniqueId());
            return true;
        }

        try {
            if (request.isTPHere()) {
                // Телепортация для TPHERE: получатель -> отправитель
                boolean teleportSuccess = target.teleport(senderPlayer.getLocation());

                if (!teleportSuccess) {
                    throw new Exception("Teleport failed");
                }

                // Уведомления для tphere
                plugin.sendMessage(target, plugin.getMessage("tpahere.accepted.target")
                        .replaceText(builder -> builder.matchLiteral("{sender}").replacement(senderPlayer.getName())));

                plugin.sendMessage(senderPlayer, plugin.getMessage("tpahere.accepted.sender")
                        .replaceText(builder -> builder.matchLiteral("{target}").replacement(target.getName())));

            } else {
                // Телепортация для TPA: отправитель -> получатель
                boolean teleportSuccess = senderPlayer.teleport(target.getLocation());

                if (!teleportSuccess) {
                    throw new Exception("Teleport failed");
                }

                // Уведомления для tpa
                plugin.sendMessage(target, plugin.getMessage("tpa.accepted.target")
                        .replaceText(builder -> builder.matchLiteral("{sender}").replacement(senderPlayer.getName())));

                plugin.sendMessage(senderPlayer, plugin.getMessage("tpa.accepted.sender")
                        .replaceText(builder -> builder.matchLiteral("{target}").replacement(target.getName())));
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Ошибка при телепортации: " + e.getMessage());
            plugin.sendMessage(target, plugin.getMessage("error.teleport-failed"));
            return true;
        }

        // Удаляем запрос
        plugin.getPendingRequests().remove(target.getUniqueId());
        return true;
    }
}
