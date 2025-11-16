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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TPAHereCommand implements CommandExecutor, TabCompleter {
    private final JustTPA plugin;

    public TPAHereCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.sendMessage(sender, plugin.getMessage("error.only-players"));
            return true;
        }

        if (!player.hasPermission("justtpa.tpahere")) {
            plugin.sendMessage(player, plugin.getMessage("error.no-permission"));
            return true;
        }

        if (args.length != 1) {
            plugin.sendMessage(player, plugin.getMessage("command.tpahere.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.sendMessage(player, plugin.getMessage("error.player-not-found"));
            return true;
        }

        if (target.equals(player)) {
            plugin.sendMessage(player, plugin.getMessage("error.self-request"));
            return true;
        }

        if (!isTeleportEnabled(target)) {
            plugin.sendMessage(player, plugin.getMessage("error.teleport-disabled"));
            return true;
        }

        // Проверяем, есть ли уже активный запрос
        JustTPA.TPARequest existingRequest = plugin.getPendingRequests().get(target.getUniqueId());
        if (existingRequest != null && existingRequest.getSender().equals(player.getUniqueId())) {
            if (!existingRequest.isExpired()) {
                plugin.sendMessage(player, plugin.getMessage("error.already-sent"));
                return true;
            }
        }

        // Создаем новый запрос (помечаем его как tpahere)
        JustTPA.TPARequest request = new JustTPA.TPARequest(player.getUniqueId(), target.getUniqueId(), true);
        plugin.getPendingRequests().put(target.getUniqueId(), request);

        // Сообщение отправителю
        Component senderMessage = plugin.getMessage("tpahere.sent")
                .replaceText(builder -> builder.matchLiteral("{target}").replacement(target.getName()))
                .append(Component.newline())
                .append(createCancelButton(target.getName()));

        plugin.sendMessage(player, senderMessage);

        // Сообщение получателю
        Component targetMessage = plugin.getMessage("tpahere.received")
                .replaceText(builder -> builder.matchLiteral("{sender}").replacement(player.getName()))
                .append(Component.newline())
                .append(createAcceptButton(player.getName()))
                .append(Component.space())
                .append(createDenyButton(player.getName()));

        plugin.sendMessage(target, targetMessage);

        return true;
    }

    private Component createCancelButton(String targetName) {
        return plugin.getMessage("button.cancel")
                .clickEvent(ClickEvent.runCommand("/tpcancel " + targetName))
                .hoverEvent(plugin.getMessage("button.hover.cancel"));
    }

    private Component createAcceptButton(String senderName) {
        return plugin.getMessage("button.accept")
                .clickEvent(ClickEvent.runCommand("/tpaccept " + senderName))
                .hoverEvent(plugin.getMessage("button.hover.accept"));
    }

    private Component createDenyButton(String senderName) {
        return plugin.getMessage("button.deny")
                .clickEvent(ClickEvent.runCommand("/tpdeny " + senderName))
                .hoverEvent(plugin.getMessage("button.hover.deny"));
    }

    private boolean isTeleportEnabled(Player player) {
        try {
            Object essentials = Bukkit.getServer().getPluginManager().getPlugin("Essentials");
            if (essentials != null) {
                Object user = essentials.getClass().getMethod("getUser", Player.class).invoke(essentials, player);
                return (boolean) user.getClass().getMethod("isTeleportEnabled").invoke(user);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Не удалось проверить tptoggle для игрока " + player.getName());
        }
        return true;
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
