package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RhittaCommand implements CommandExecutor, TabCompleter {

    private final RhittaManager manager;

    public RhittaCommand(RhittaPlugin plugin) {
        this.manager = plugin.getRhittaManager();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(
                    ChatColor.RED +
                    "Only players can use this command."
            );
            return true;
        }

        Player player = (Player) sender;

        if (!manager.isAllowedOwner(player)) {
            player.sendMessage(
                    ChatColor.RED +
                    "You are not the owner of Rhitta."
            );
            return true;
        }

        if (args.length == 0 ||
                args[0].equalsIgnoreCase("help")) {

            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {

            case "0":
                activateFireball(player);
                return true;

            case "buffs":
                toggleBuffs(player, args);
                return true;

            case "remove":
                removeDupes(player, args);
                return true;

            case "ad":
            case "ka":
            case "ue":
            case "pp":
            case "pj":
            case "kau":

                activateAbility(
                        player,
                        args[0].toUpperCase()
                );

                return true;

            default:

                player.sendMessage(
                        ChatColor.RED +
                        "Unknown Rhitta command."
                );

                player.sendMessage(
                        ChatColor.YELLOW +
                        "Use /rhitta help"
                );

                return true;
        }
    }

    // ============================================================
    // FIREBALL
    // ============================================================

    private void activateFireball(Player player) {

        manager.setAbilityActive(player, "0");

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "RHITTA"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Fireball Mode: " +
                ChatColor.GREEN +
                "ON"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Right-click with Rhitta to fire."
        );
    }

    // ============================================================
    // BUFFS
    // ============================================================

    private void toggleBuffs(
            Player player,
            String[] args) {

        if (args.length < 2) {

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Usage: /rhitta buffs true|false"
            );

            return;
        }

        if (!args[1].equalsIgnoreCase("true")
                && !args[1].equalsIgnoreCase("false")) {

            player.sendMessage(
                    ChatColor.RED +
                    "Use true or false."
            );

            return;
        }

        boolean enabled =
                Boolean.parseBoolean(args[1]);

        manager.setBuffsEnabled(enabled);

        player.sendMessage(
                ChatColor.GOLD +
                "Rhitta buffs: " +
                (enabled
                        ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
        );
    }

    // ============================================================
    // REMOVE DUPES
    // ============================================================

    private void removeDupes(
            Player player,
            String[] args) {

        if (args.length < 2 ||
                !args[1].equalsIgnoreCase("dupes")) {

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Usage: /rhitta remove dupes"
            );

            return;
        }

        int removed =
                manager.removeDuplicates();

        player.sendMessage(
                ChatColor.GOLD +
                "Rhitta dupe cleanup complete."
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Removed copies: " +
                ChatColor.WHITE +
                removed
        );
    }

    // ============================================================
    // PRIDE ABILITIES
    // ============================================================

    private void activateAbility(
            Player player,
            String ability) {

        manager.setAbilityActive(
                player,
                ability
        );

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "RHITTA"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Active ability: " +
                ChatColor.WHITE +
                ability
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Previous ability has been turned off."
        );
    }

    // ============================================================
    // HELP
    // ============================================================

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "========== RHITTA =========="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta help"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Shows this help menu."
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta 0"
                + ChatColor.GRAY +
                " - Fireball"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta AD"
                + ChatColor.GRAY +
                " - Pride Ability"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta KA"
                + ChatColor.GRAY +
                " - Pride Ability"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta UE"
                + ChatColor.GRAY +
                " - Pride Ability"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta PP"
                + ChatColor.GRAY +
                " - Pride Ability"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta PJ"
                + ChatColor.GRAY +
                " - Pride Ability"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta KAU"
                + ChatColor.GRAY +
                " - Pride Ability"
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buffs true"
                + ChatColor.GRAY +
                " - Enable buffs"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buffs false"
                + ChatColor.GRAY +
                " - Disable buffs"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta remove dupes"
                + ChatColor.GRAY +
                " - Remove duplicate Rhittas"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "============================"
        );
    }

    // ============================================================
    // TAB COMPLETION
    // ============================================================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {

        if (args.length == 1) {

            List<String> commands =
                    Arrays.asList(
                            "help",
                            "0",
                            "AD",
                            "KA",
                            "UE",
                            "PP",
                            "PJ",
                            "KAU",
                            "buffs",
                            "remove"
                    );

            List<String> result =
                    new ArrayList<>();

            for (String value : commands) {

                if (value.toLowerCase()
                        .startsWith(
                                args[0].toLowerCase()
                        )) {

                    result.add(value);
                }
            }

            return result;
        }

        if (args.length == 2 &&
                args[0].equalsIgnoreCase("buffs")) {

            return Arrays.asList(
                    "true",
                    "false"
            );
        }

        if (args.length == 2 &&
                args[0].equalsIgnoreCase("remove")) {

            return Arrays.asList(
                    "dupes"
            );
        }

        return new ArrayList<>();
    }
}