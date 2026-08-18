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

        RhittaManager manager = plugin.getRhittaManager();

        // Only players can use the command
        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        // /rhitta
        if (args.length == 0) {
            sendStatus(player, manager);
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {

            // /rhitta status
            case "status" -> sendStatus(player, manager);

            // /rhitta give
            case "give" -> {

                if (!manager.isAllowedOwner(player)) {
                    player.sendMessage(
                            ChatColor.RED + "Only _ToshiroCyMc can receive Rhitta."
                    );
                    return true;
                }

                manager.giveRhitta(player);

                player.sendMessage(
                        ChatColor.GREEN + "Rhitta has been given to you."
                );
            }

            // /rhitta remove PlayerName
            case "remove" -> {

                // Only the Rhitta owner can remove it
                if (!manager.isAllowedOwner(player)) {
                    player.sendMessage(
                            ChatColor.RED + "Only the Rhitta owner can use this command."
                    );
                    return true;
                }

                if (args.length < 2) {
                    player.sendMessage(
                            ChatColor.RED + "Usage: /rhitta remove PlayerName"
                    );
                    return true;
                }

                Player target = plugin.getServer()
                        .getPlayerExact(args[1]);

                if (target == null) {
                    player.sendMessage(
                            ChatColor.RED + "That player is not online."
                    );
                    return true;
                }

                int removed = manager.removeRhitta(target);

                if (removed > 0) {
                    player.sendMessage(
                            ChatColor.GREEN
                                    + "Removed "
                                    + removed
                                    + " Rhitta item(s) from "
                                    + target.getName()
                                    + "."
                    );

                    target.sendMessage(
                            ChatColor.RED
                                    + "Rhitta was removed from your inventory."
                    );
                } else {
                    player.sendMessage(
                            ChatColor.YELLOW
                                    + target.getName()
                                    + " does not have Rhitta."
                    );
                }
            }

            default -> player.sendMessage(
                    ChatColor.RED
                            + "Usage: /rhitta [status|give|remove PlayerName]"
            );
        }

        return true;
    }

    private void sendStatus(Player player, RhittaManager manager) {

        boolean hasIt = manager.hasRhitta(player);
        boolean owns = manager.isOwner(player);
        int defense = manager.getDefense(player);

        player.sendMessage(
                ChatColor.GOLD + "--- Rhitta Status ---"
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "Holding Rhitta: "
                        + (hasIt ? "Yes" : "No")
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "Owner: "
                        + (owns ? "You" : "Someone else")
        );

        player.sendMessage(
                ChatColor.YELLOW
                        + "Defense stacks: "
                        + defense
        );
    }
            }
