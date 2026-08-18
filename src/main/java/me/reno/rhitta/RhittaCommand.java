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
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        RhittaManager manager = plugin.getRhittaManager();

        if (args.length == 0) {
            sendStatus(player, manager);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "status" -> sendStatus(player, manager);

            case "give" -> {
                if (!manager.isOwner(player)) {
                    player.sendMessage(ChatColor.RED + "Only the current owner of Rhitta can do that.");
                    return true;
                }
                manager.giveRhitta(player);
                player.sendMessage(ChatColor.GREEN + "Rhitta has been given to you.");
            }

            default -> player.sendMessage(ChatColor.RED + "Usage: /rhitta [status|give]");
        }

        return true;
    }

    private void sendStatus(Player player, RhittaManager manager) {
        boolean hasIt = manager.hasRhitta(player);
        boolean owns = manager.isOwner(player);
        int defense = manager.getDefense(player);

        player.sendMessage(ChatColor.GOLD + "--- Rhitta Status ---");
        player.sendMessage(ChatColor.YELLOW + "Holding Rhitta: " + (hasIt ? "Yes" : "No"));
        player.sendMessage(ChatColor.YELLOW + "Owner: " + (owns ? "You" : "Someone else"));
        player.sendMessage(ChatColor.YELLOW + "Defense stacks: " + defense);
    }
}
