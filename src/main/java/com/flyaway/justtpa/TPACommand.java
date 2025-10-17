package com.flyaway.justtpa;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TPACommand implements CommandExecutor, TabCompleter {
    private final JustTPA plugin;

    public TPACommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(Component.text("Эта команда только для игроков!", NamedTextColor.RED));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("justtpa.tpa")) {
            player.sendMessage(Component.text("У вас нет прав для использования этой команды!", NamedTextColor.RED));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(Component.text("Использование: /tpa <ник>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            player.sendMessage(Component.text("Игрок не найден или не в сети!", NamedTextColor.RED));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(Component.text("Нельзя отправить запрос самому себе!", NamedTextColor.RED));
            return true;
        }

        if (!isTeleportEnabled(target)) {
            player.sendMessage(Component.text("Этот игрок отключил запросы на телепортацию!", NamedTextColor.RED));
            return true;
        }

        // Проверяем, есть ли уже активный запрос
        JustTPA.TPARequest existingRequest = plugin.getPendingRequests().get(target.getUniqueId());
        if (existingRequest != null && existingRequest.getSender().equals(player.getUniqueId())) {
            if (!existingRequest.isExpired()) {
                player.sendMessage(Component.text("Вы уже отправили запрос этому игроку!", NamedTextColor.RED));
                return true;
            }
        }

        // Создаем новый запрос
        JustTPA.TPARequest request = new JustTPA.TPARequest(player.getUniqueId(), target.getUniqueId());
        plugin.getPendingRequests().put(target.getUniqueId(), request);

        // Сообщение отправителю
        Component senderMessage = Component.text()
                .append(Component.text("✓ ", NamedTextColor.GREEN))
                .append(Component.text("Запрос на телепортацию отправлен игроку ", NamedTextColor.GRAY))
                .append(Component.text(target.getName(), NamedTextColor.YELLOW))
                .append(Component.newline())
                .append(createCancelButton(target.getName()))
                .build();

        player.sendMessage(senderMessage);

        // Сообщение получателю
        Component targetMessage = Component.text()
                .append(Component.text("✈ ", NamedTextColor.AQUA))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" хочет телепортироваться к вам!", NamedTextColor.GRAY))
                .append(Component.newline())
                .append(createAcceptButton(player.getName()))
                .append(Component.text(" "))
                .append(createDenyButton(player.getName()))
                .build();

        target.sendMessage(targetMessage);

        return true;
    }

    private boolean isTeleportEnabled(Player player) {
        try {
            Object essentials = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials != null) {
                Object user = essentials.getClass().getMethod("getUser", Player.class).invoke(essentials, player);
                return (boolean) user.getClass().getMethod("isTeleportEnabled").invoke(user);
            }
        } catch (Exception ex) {
            // Если не можем проверить, считаем что tptoggle выключен
            plugin.getLogger().warning("Не удалось проверить tptoggle для игрока " + player.getName());
        }
        return false;
    }

    private Component createCancelButton(String targetName) {
        return Component.text()
                .append(Component.text("[✕ ОТМЕНИТЬ]", NamedTextColor.RED))
                .clickEvent(ClickEvent.runCommand("/tpcancel " + targetName))
                .hoverEvent(Component.text("Нажмите чтобы отменить запрос", NamedTextColor.GRAY))
                .build();
    }

    private Component createAcceptButton(String senderName) {
        return Component.text()
                .append(Component.text("[✓ ПРИНЯТЬ]", NamedTextColor.GREEN))
                .clickEvent(ClickEvent.runCommand("/tpaccept " + senderName))
                .hoverEvent(Component.text("Нажмите чтобы принять запрос", NamedTextColor.GRAY))
                .build();
    }

    private Component createDenyButton(String senderName) {
        return Component.text()
                .append(Component.text("[✕ ОТКЛОНИТЬ]", NamedTextColor.RED))
                .clickEvent(ClickEvent.runCommand("/tpdeny " + senderName))
                .hoverEvent(Component.text("Нажмите чтобы отклонить запрос", NamedTextColor.GRAY))
                .build();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());

            StringUtil.copyPartialMatches(args[0], playerNames, completions);
        }

        return completions;
    }
}
