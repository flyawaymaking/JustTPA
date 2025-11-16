package com.flyaway.justtpa;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class JustTPAReloadCommand implements CommandExecutor {
    private final JustTPA plugin;

    public JustTPAReloadCommand(JustTPA plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("justtpa.reload")) {
            plugin.sendMessage(sender, plugin.getMessage("reload.no-permission"));
            return true;
        }

        if (args.length != 1 && !args[0].equals("reload")) {
            plugin.sendMessage(sender, plugin.getMessage("command.reload.usage"));
            return true;
        }

        plugin.getConfigManager().reloadConfig();
        plugin.sendMessage(sender, plugin.getMessage("reload.success"));
        return true;
    }
}
