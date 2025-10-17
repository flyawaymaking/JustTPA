package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TPCancelCommand implements CommandExecutor {
    private final JustTPA plugin;

    public TPCancelCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("justtpa.tpcancel")) {
            player.sendMessage(Component.text("У вас нет прав для использования этой команды!", NamedTextColor.RED));
            return true;
        }

        // Обработка команды без аргументов
        if (args.length == 0) {
            JustTPA.TPARequest request = findLatestSentRequest(player);
            if (request == null) {
                player.sendMessage(Component.text("У вас нет активных запросов телепортации!", NamedTextColor.RED));
                return true;
            }

            return processCancel(request, player);
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Использование: /tpcancel [ник]", NamedTextColor.RED));
            return true;
        }

        // Обработка команды с указанием ника
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Игрок не найден!", NamedTextColor.RED));
            return true;
        }

        JustTPA.TPARequest request = plugin.getPendingRequests().get(target.getUniqueId());

        if (request == null || !request.getSender().equals(player.getUniqueId())) {
            player.sendMessage(Component.text("У вас нет активного запроса к этому игроку!", NamedTextColor.RED));
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

        // Уведомления
        sender.sendMessage(Component.text()
                .append(Component.text("✓ ", NamedTextColor.YELLOW))
                .append(Component.text("Вы отменили запрос телепортации к ", NamedTextColor.GRAY))
                .append(Component.text(targetPlayer != null ? targetPlayer.getName() : "игроку", NamedTextColor.YELLOW))
                .build());

        if (targetPlayer != null) {
            targetPlayer.sendMessage(Component.text()
                    .append(Component.text("✕ ", NamedTextColor.RED))
                    .append(Component.text(sender.getName(), NamedTextColor.YELLOW))
                    .append(Component.text(" отменил запрос телепортации!", NamedTextColor.GRAY))
                    .build());
        }

        // Удаляем запрос
        plugin.getPendingRequests().remove(request.getTarget());

        return true;
    }
}
