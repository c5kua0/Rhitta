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

        if (!(sender instanceof Player)) {
            sender.sendMessage(
                    ChatColor.RED +
                    "Only players can use Rhitta commands."
            );
            return true;
        }

        Player player = (Player) sender;
        RhittaManager manager = plugin.getRhittaManager();

        // /rhitta
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        // /rhitta help
        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        // /rhitta ability list
        if (args.length >= 2
                && args[0].equalsIgnoreCase("ability")
                && args[1].equalsIgnoreCase("list")) {

            sendAbilityList(player);
            return true;
        }

        // /rhitta status
        if (args[0].equalsIgnoreCase("status")) {
            sendStatus(player);
            return true;
        }

        // /rhitta buffs true/false
        if (args[0].equalsIgnoreCase("buffs")
                || args[0].equalsIgnoreCase("buff")) {

            if (!manager.isOwner(player)) {
                player.sendMessage(
                        ChatColor.RED +
                        "You are not the owner of Rhitta."
                );
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(
                        ChatColor.YELLOW +
                        "Usage: /rhitta buffs <true|false>"
                );
                return true;
            }

            if (!args[1].equalsIgnoreCase("true")
                    && !args[1].equalsIgnoreCase("false")) {

                player.sendMessage(
                        ChatColor.RED +
                        "Use true or false."
                );
                return true;
            }

            boolean enabled =
                    Boolean.parseBoolean(args[1]);

            manager.setBuffsEnabled(enabled);

            player.sendMessage(
                    ChatColor.GOLD +
                    "Rhitta Survival Buffs: "
                    + (enabled
                    ? ChatColor.GREEN + "ENABLED"
                    : ChatColor.RED + "DISABLED")
            );

            return true;
        }

        // /rhitta remove dupes
        if (args.length >= 2
                && args[0].equalsIgnoreCase("remove")
                && args[1].equalsIgnoreCase("dupes")) {

            if (!manager.isOwner(player)) {
                player.sendMessage(
                        ChatColor.RED +
                        "You are not the owner of Rhitta."
                );
                return true;
            }

            int removed =
                    manager.removeDuplicates();

            player.sendMessage(
                    ChatColor.GOLD +
                    "Rhitta duplicate cleanup complete."
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Removed: "
                    + ChatColor.WHITE
                    + removed
                    + ChatColor.YELLOW
                    + " duplicate item(s)."
            );

            return true;
        }

        // /rhitta give
        if (args[0].equalsIgnoreCase("give")) {

            if (!manager.isOwner(player)) {
                player.sendMessage(
                        ChatColor.RED +
                        "You are not the owner of Rhitta."
                );
                return true;
            }

            manager.forceOneRhitta(player);

            player.sendMessage(
                    ChatColor.GOLD +
                    "Rhitta has been restored."
            );

            return true;
        }

        // /rhitta clean
        if (args[0].equalsIgnoreCase("clean")) {

            if (!manager.isOwner(player)) {
                player.sendMessage(
                        ChatColor.RED +
                        "You are not the owner of Rhitta."
                );
                return true;
            }

            int removed =
                    manager.removeDuplicates();

            player.sendMessage(
                    ChatColor.GOLD +
                    "Cleanup complete. Removed "
                    + ChatColor.YELLOW
                    + removed
                    + ChatColor.GOLD
                    + " duplicate item(s)."
            );

            return true;
        }

        // /rhitta 0 / AD / KA / UE / PP / PJ / KAU
        if (isAbility(args[0])) {

            if (!manager.isOwner(player)) {
                player.sendMessage(
                        ChatColor.RED +
                        "Only "
                        + RhittaManager.OWNER_NAME
                        + " can use Rhitta."
                );
                return true;
            }

            String ability =
                    normalizeAbility(args[0]);

            manager.setAbilityActive(
                    player,
                    ability
            );

            player.sendMessage(
                    ChatColor.GOLD +
                    "Selected: "
                    + ChatColor.YELLOW
                    + abilityName(ability)
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Right-click with Rhitta to activate."
            );

            return true;
        }

        player.sendMessage(
                ChatColor.RED +
                "Unknown Rhitta command."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Use /rhitta ability list"
        );

        return true;
    }

    // ============================================================
    // ABILITY CHECK
    // ============================================================

    private boolean isAbility(String input) {

        return input.equalsIgnoreCase("0")
                || input.equalsIgnoreCase("AD")
                || input.equalsIgnoreCase("KA")
                || input.equalsIgnoreCase("UE")
                || input.equalsIgnoreCase("PP")
                || input.equalsIgnoreCase("PJ")
                || input.equalsIgnoreCase("KAU");
    }

    private String normalizeAbility(String input) {

        if (input.equalsIgnoreCase("0")) {
            return "fireball";
        }

        if (input.equalsIgnoreCase("AD")) {
            return "absolute_dominance";
        }

        if (input.equalsIgnoreCase("KA")) {
            return "king_aura";
        }

        if (input.equalsIgnoreCase("UE")) {
            return "unbreakable_ego";
        }

        if (input.equalsIgnoreCase("PP")) {
            return "punishment_proud";
        }

        if (input.equalsIgnoreCase("PJ")) {
            return "prides_judgment";
        }

        if (input.equalsIgnoreCase("KAU")) {
            return "kings_authority";
        }

        return input.toLowerCase();
    }

    // ============================================================
    // HELP
    // ============================================================

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "========== RHITTA 2.0 =========="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta ability list"
                + ChatColor.WHITE +
                " - All abilities"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta status"
                + ChatColor.WHITE +
                " - Rhitta status"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buffs <true|false>"
                + ChatColor.WHITE +
                " - Survival Buffs"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta remove dupes"
                + ChatColor.WHITE +
                " - Remove duplicates"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta give"
                + ChatColor.WHITE +
                " - Restore Rhitta"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "================================"
        );
    }

    // ============================================================
    // ABILITY LIST
    // ============================================================

    private void sendAbilityList(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "========== RHITTA 2.0 =========="
        );

        player.sendMessage(
                ChatColor.GOLD +
                "🔥 /rhitta 0"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "Fireball"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Launches a powerful fireball from Rhitta."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "👑 /rhitta AD"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "Absolute Dominance"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Overwhelms nearby enemies with intimidating presence."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "👑 /rhitta KA"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "King's Aura"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Strengthens the user and weakens nearby enemies."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "🛡 /rhitta UE"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "Unbreakable Ego"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Greatly increases the user's defenses."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "⚔ /rhitta PP"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "Punishment of the Proud"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Empowers Rhitta to deal devastating damage."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "⚖ /rhitta PJ"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "Pride's Judgment"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Unleashes a powerful judgment attack."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "☀ /rhitta KAU"
        );

        player.sendMessage(
                ChatColor.WHITE +
                "King's Authority Ultimate"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Releases Rhitta's ultimate power over a large area."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "---------- OTHER COMMANDS ----------"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buffs true"
                + ChatColor.WHITE +
                " - Enable Survival Buffs."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buffs false"
                + ChatColor.WHITE +
                " - Disable Survival Buffs."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta remove dupes"
                + ChatColor.WHITE +
                " - Remove duplicate Rhittas."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta status"
                + ChatColor.WHITE +
                " - Show Rhitta status."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "===================================="
        );
    }

    // ============================================================
    // STATUS
    // ============================================================

    private void sendStatus(Player player) {

        RhittaManager manager =
                plugin.getRhittaManager();

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "========== RHITTA STATUS =========="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Owner: "
                + ChatColor.WHITE
                + RhittaManager.OWNER_NAME
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Your Rhitta: "
                + (manager.isOwner(player)
                && manager.hasRhitta(player)
                ? ChatColor.GREEN + "YES"
                : ChatColor.RED + "NO")
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Rhitta Copies: "
                + ChatColor.WHITE
                + manager.countRhitta(player)
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Physical Attack: "
                + ChatColor.WHITE
                + "+20"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Life Steal: "
                + ChatColor.WHITE
                + "4 HP"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Defense: "
                + ChatColor.WHITE
                + manager.getDefense(player)
        );

        String selected =
                manager.getActiveAbility(player);

        player.sendMessage(
                ChatColor.YELLOW +
                "Selected Ability: "
                + ChatColor.WHITE
                + (selected == null
                ? "None"
                : abilityName(selected))
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Survival Buffs: "
                + (manager.isBuffsEnabled()
                ? ChatColor.GREEN + "ENABLED"
                : ChatColor.RED + "DISABLED")
        );

        player.sendMessage(
                ChatColor.GOLD +
                "=================================="
        );
    }

    // ============================================================
    // ABILITY NAMES
    // ============================================================

    private String abilityName(String ability) {

        if (ability == null) {
            return "None";
        }

        switch (ability.toLowerCase()) {

            case "fireball":
                return "Fireball";

            case "absolute_dominance":
                return "Absolute Dominance";

            case "king_aura":
                return "King's Aura";

            case "unbreakable_ego":
                return "Unbreakable Ego";

            case "punishment_proud":
                return "Punishment of the Proud";

            case "prides_judgment":
                return "Pride's Judgment";

            case "kings_authority":
                return "King's Authority Ultimate";

            default:
                return ability;
        }
    }
}