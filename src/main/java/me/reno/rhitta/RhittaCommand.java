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

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        // HELP
        if (args[0].equalsIgnoreCase("help")) {
            sendHelp(player);
            return true;
        }

        // SKILLS
        if (args[0].equalsIgnoreCase("skills")) {

            if (!manager.isOwner(player)) {
                sendOwnerError(player);
                return true;
            }

            if (args.length < 2) {
                sendSkillStatus(player);
                return true;
            }

            switch (args[1].toLowerCase()) {

                case "on":

                    manager.setSkillsEnabled(true);

                    player.sendMessage(
                            ChatColor.GOLD +
                            "Rhitta Hotbar Skills: " +
                            ChatColor.GREEN +
                            "ENABLED"
                    );

                    player.sendMessage(
                            ChatColor.GRAY +
                            "Right Click while holding Rhitta to activate."
                    );

                    break;

                case "off":

                    manager.setSkillsEnabled(false);

                    player.sendMessage(
                            ChatColor.GOLD +
                            "Rhitta Hotbar Skills: " +
                            ChatColor.RED +
                            "DISABLED"
                    );

                    break;

                case "status":

                    sendSkillStatus(player);
                    break;

                default:

                    player.sendMessage(
                            ChatColor.YELLOW +
                            "Usage: /rhitta skills <on|off|status>"
                    );

                    break;
            }

            return true;
        }

        // ABILITY LIST
        if (args.length >= 2
                && args[0].equalsIgnoreCase("ability")
                && args[1].equalsIgnoreCase("list")) {

            sendAbilityList(player);
            return true;
        }

        // STATUS
        if (args[0].equalsIgnoreCase("status")) {

            sendStatus(player);
            return true;
        }

        // BUFFS
        if (args[0].equalsIgnoreCase("buffs")
                || args[0].equalsIgnoreCase("buff")) {

            if (!manager.isOwner(player)) {
                sendOwnerError(player);
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
                    "Rhitta Survival Buffs: " +
                    (enabled
                            ? ChatColor.GREEN + "ENABLED"
                            : ChatColor.RED + "DISABLED")
            );

            return true;
        }

        // REMOVE DUPES
        if (args.length >= 2
                && args[0].equalsIgnoreCase("remove")
                && args[1].equalsIgnoreCase("dupes")) {

            if (!manager.isOwner(player)) {
                sendOwnerError(player);
                return true;
            }

            int removed = manager.removeDuplicates();

            player.sendMessage(
                    ChatColor.GOLD +
                    "Rhitta duplicate cleanup complete."
            );

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Removed: " +
                    ChatColor.WHITE +
                    removed +
                    ChatColor.YELLOW +
                    " duplicate item(s)."
            );

            return true;
        }

        // GIVE
        if (args[0].equalsIgnoreCase("give")) {

            if (!manager.isOwner(player)) {
                sendOwnerError(player);
                return true;
            }

            manager.forceOneRhitta(player);

            player.sendMessage(
                    ChatColor.GOLD +
                    "Rhitta has been restored."
            );

            return true;
        }

        // CLEAN
        if (args[0].equalsIgnoreCase("clean")) {

            if (!manager.isOwner(player)) {
                sendOwnerError(player);
                return true;
            }

            int removed = manager.removeDuplicates();

            player.sendMessage(
                    ChatColor.GOLD +
                    "Cleanup complete. Removed " +
                    ChatColor.YELLOW +
                    removed +
                    ChatColor.GOLD +
                    " duplicate item(s)."
            );

            return true;
        }

        // DIRECT ABILITY SELECTION
        if (isAbility(args[0])) {

            if (!manager.isOwner(player)) {
                sendOwnerError(player);
                return true;
            }

            String ability = normalizeAbility(args[0]);

            manager.setAbilityActive(
                    player,
                    ability
            );

            player.sendMessage(
                    ChatColor.GOLD +
                    "Selected: " +
                    ChatColor.YELLOW +
                    abilityName(ability)
            );

            return true;
        }

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

    private void sendOwnerError(Player player) {

        player.sendMessage(
                ChatColor.RED +
                "Only " +
                RhittaManager.OWNER_NAME +
                " can use Rhitta."
        );
    }

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

        switch (input.toUpperCase()) {

            case "0":
                return "fireball";

            case "AD":
                return "absolute_dominance";

            case "KA":
                return "king_aura";

            case "UE":
                return "unbreakable_ego";

            case "PP":
                return "punishment_proud";

            case "PJ":
                return "prides_judgment";

            case "KAU":
                return "kings_authority";

            default:
                return input.toLowerCase();
        }
    }

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                ChatColor.BOLD +
                "========== RHITTA =========="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta skills on" +
                ChatColor.WHITE +
                " - Enable Hotbar Skills"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta skills off" +
                ChatColor.WHITE +
                " - Disable Hotbar Skills"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta skills status" +
                ChatColor.WHITE +
                " - Skill status"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta ability list" +
                ChatColor.WHITE +
                " - List abilities"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta status" +
                ChatColor.WHITE +
                " - Rhitta status"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buffs <true|false>" +
                ChatColor.WHITE +
                " - Survival buffs"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta remove dupes" +
                ChatColor.WHITE +
                " - Remove duplicates"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Right Click" +
                ChatColor.WHITE +
                " - Activate selected hotbar skill"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "============================"
        );
    }

    private void sendSkillStatus(Player player) {

        boolean enabled =
                plugin.getRhittaManager()
                        .isSkillsEnabled();

        player.sendMessage(
                ChatColor.GOLD +
                "Rhitta Hotbar Skills: " +
                (enabled
                        ? ChatColor.GREEN + "ENABLED"
                        : ChatColor.RED + "DISABLED")
        );

        if (enabled) {

            player.sendMessage(
                    ChatColor.GRAY +
                    "Hold Rhitta and Right Click."
            );
        }
    }

    private void sendAbilityList(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                ChatColor.BOLD +
                "========== RHITTA SKILLS =========="
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Slot 1 " +
                ChatColor.WHITE +
                "- " +
                ChatColor.YELLOW +
                "🔥 Fireball"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Slot 2 " +
                ChatColor.WHITE +
                "- " +
                ChatColor.YELLOW +
                "👑 King's Aura"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Slot 3 " +
                ChatColor.WHITE +
                "- " +
                ChatColor.YELLOW +
                "👑 Absolute Dominance"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Slot 4 " +
                ChatColor.WHITE +
                "- " +
                ChatColor.YELLOW +
                "🛡 Unbreakable Ego"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Slot 5 " +
                ChatColor.WHITE +
                "- " +
                ChatColor.YELLOW +
                "⚔ Punishment of the Proud"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Slot 6 " +
                ChatColor.WHITE +
                "- " +
                ChatColor.YELLOW +
                "⚖ Pride's Judgment"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Slot 7 " +
                ChatColor.WHITE +
                "- " +
                ChatColor.YELLOW +
                "☀ King's Authority Ultimate"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Hold Rhitta and Right Click to activate."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "=================================="
        );
    }

    private void sendStatus(Player player) {

        RhittaManager manager =
                plugin.getRhittaManager();

        player.sendMessage(
                ChatColor.GOLD +
                ChatColor.BOLD +
                "========== RHITTA STATUS =========="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Owner: " +
                ChatColor.WHITE +
                RhittaManager.OWNER_NAME
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Hotbar Skills: " +
                (manager.isSkillsEnabled()
                        ? ChatColor.GREEN + "ON"
                        : ChatColor.RED + "OFF")
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Defense: " +
                ChatColor.WHITE +
                manager.getDefense(player)
        );

        String selected =
                manager.getActiveAbility(player);

        player.sendMessage(
                ChatColor.YELLOW +
                "Selected Ability: " +
                ChatColor.WHITE +
                (selected == null
                        ? "None"
                        : abilityName(selected))
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Survival Buffs: " +
                (manager.isBuffsEnabled()
                        ? ChatColor.GREEN + "ENABLED"
                        : ChatColor.RED + "DISABLED")
        );

        player.sendMessage(
                ChatColor.GOLD +
                "=================================="
        );
    }

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