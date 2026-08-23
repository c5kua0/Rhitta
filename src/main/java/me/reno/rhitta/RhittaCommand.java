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

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    public RhittaCommand(RhittaPlugin plugin) {
        this.plugin = plugin;
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

        String arg = args[0].toLowerCase();

        switch (arg) {

            // ====================================================
            // FIREBALL
            // ====================================================

            case "0":

                manager.setFireballMode(player, true);
                manager.clearAbility(player);

                player.sendMessage(
                        ChatColor.GOLD +
                        "🔥 Fireball Mode " +
                        ChatColor.GREEN +
                        "ENABLED"
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Right-click with Rhitta to fire."
                );

                return true;

            // ====================================================
            // BUFFS
            // ====================================================

            case "buffs":

                if (args.length < 2) {

                    player.sendMessage(
                            ChatColor.YELLOW +
                            "Usage: /rhitta buffs true|false"
                    );

                    return true;
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

                return true;

            // ====================================================
            // REMOVE DUPES
            // ====================================================

            case "remove":

                if (args.length < 2 ||
                        !args[1].equalsIgnoreCase("dupes")) {

                    player.sendMessage(
                            ChatColor.YELLOW +
                            "Usage: /rhitta remove dupes"
                    );

                    return true;
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

                return true;

            // ====================================================
            // PRIDE ABILITIES
            // ====================================================

            case "ad":
                activateAbility(player, "AD");
                return true;

            case "ka":
                activateAbility(player, "KA");
                return true;

            case "ue":
                activateAbility(player, "UE");
                return true;

            case "pp":
                activateAbility(player, "PP");
                return true;

            case "pj":
                activateAbility(player, "PJ");
                return true;

            case "kau":
                activateAbility(player, "KAU");
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
    // ABILITY ACTIVATION
    // ============================================================

    private void activateAbility(
            Player player,
            String ability) {

        /*
         * Only ONE ability can be active.
         *
         * Activating another ability automatically
         * turns Fireball Mode off.
         */
        manager.disableFireballMode(player);

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
                "Ability activated: " +
                ChatColor.WHITE +
                ability
        );

        // Actual ability effects will be added
        // to the listener in the next step.
    }

    // ============================================================
    // HELP
    // ============================================================

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "===== RHITTA ====="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta 0 " +
                ChatColor.GRAY +
                "- Fireball Mode"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta AD " +
                ChatColor.GRAY +
                "- Absolute Dominance"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta KA " +
                ChatColor.GRAY +
                "- King's Aura"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta UE " +
                ChatColor.GRAY +
                "- Unbreakable Ego"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta PP " +
                ChatColor.GRAY +
                "- Punishment of the Proud"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta PJ " +
                ChatColor.GRAY +
                "- Pride's Judgment"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta KAU " +
                ChatColor.GRAY +
                "- King's Authority Ultimate"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buffs true/false " +
                ChatColor.GRAY +
                "- Toggle buffs"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta remove dupes " +
                ChatColor.GRAY +
                "- Remove duplicate Rhittas"
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