package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TPCancelCommand implements CommandExecutor {
    private final JustTPA plugin;

    public TPCancelCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, plugin.getMessage("error.only-players"));
            return true;
        }

        if (!player.hasPermission("justtpa.tpcancel")) {
            plugin.sendMessage(player, plugin.getMessage("error.no-permission"));
            return true;
        }

        // Обработка команды без аргументов
        if (args.length == 0) {
            JustTPA.TPARequest request = findLatestSentRequest(player);
            if (request == null) {
                plugin.sendMessage(player, plugin.getMessage("tpcancel.no-requests"));
                return true;
            }

            return processCancel(request, player);
        }

        if (args.length != 1) {
            plugin.sendMessage(player, plugin.getMessage("command.tpcancel.usage"));
            return true;
        }

        // Обработка команды с указанием ника
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.sendMessage(player, plugin.getMessage("error.player-not-found"));
            return true;
        }

        JustTPA.TPARequest request = plugin.getPendingRequests().get(target.getUniqueId());

        if (request == null || !request.getSender().equals(player.getUniqueId())) {
            plugin.sendMessage(player, plugin.getMessage("tpcancel.no-request-to-player"));
            return true;
        }

        return processCancel(request, player);
    }

    // Поиск последнего отправленного запроса
    private JustTPA.TPARequest findLatestSentRequest(Player sender) {
        JustTPA.TPARequest latestRequest = null;
        long latestTimestamp = 0;

        for (JustTPA.TPARequest request : plugin.getPendingRequests().values()) {
            if (request.getSender().equals(sender.getUniqueId()) && !request.isExpired()) {
                if (request.getTimestamp() > latestTimestamp) {
                    latestTimestamp = request.getTimestamp();
                    latestRequest = request;
                }
            }
        }

        return latestRequest;
    }

    // Обработка отмены запроса
    private boolean processCancel(JustTPA.TPARequest request, Player sender) {
        Player targetPlayer = Bukkit.getPlayer(request.getTarget());

        // Уведомление отправителю
        if (targetPlayer != null) {
            plugin.sendMessage(sender, plugin.getMessage("tpcancel.cancelled")
                    .replaceText(builder -> builder.matchLiteral("{target}").replacement(targetPlayer.getName())));
        } else {
            plugin.sendMessage(sender, plugin.getMessage("tpcancel.cancelled-generic"));
        }

        // Уведомление получателю
        if (targetPlayer != null) {
            plugin.sendMessage(targetPlayer, plugin.getMessage("tpcancel.received")
                    .replaceText(builder -> builder.matchLiteral("{sender}").replacement(sender.getName())));
        }

        // Удаляем запрос
        plugin.getPendingRequests().remove(request.getTarget());

        return true;
    }
}
