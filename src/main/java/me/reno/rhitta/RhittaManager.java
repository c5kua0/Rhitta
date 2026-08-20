package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RhittaCommand implements CommandExecutor {

    private final RhittaPlugin plugin;

    public RhittaCommand(RhittaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player player)) {

            sender.sendMessage(
                    ChatColor.RED
                            + "Only players can use this command."
            );

            return true;
        }

        if (!player.getName()
                .equalsIgnoreCase("_ToshiroCyMc")) {

            player.sendMessage(
                    ChatColor.RED
                            + "You are not the owner of Rhitta."
            );

            return true;
        }

        plugin.getRhittaManager()
                .giveRhitta(player);

        player.sendMessage(
                ChatColor.GOLD
                        + "Rhitta has been restored."
        );

        return true;
    }
};