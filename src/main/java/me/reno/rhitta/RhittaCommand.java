package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class RhittaCommand implements CommandExecutor, TabCompleter {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    public RhittaCommand(RhittaPlugin plugin) {
        this.plugin = plugin;
        this.manager = plugin.getRhittaManager();
    }

    // ============================================================
    // COMMAND
    // ============================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {

        // --------------------------------------------------------
        // PLAYER ONLY
        // --------------------------------------------------------

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    ChatColor.RED +
                    "This command can only be used by a player."
            );

            return true;
        }

        Player player = (Player) sender;

        // --------------------------------------------------------
        // OWNER CHECK
        // --------------------------------------------------------

        if (!manager.isOwner(player)) {

            player.sendMessage(
                    ChatColor.RED +
                    "Only the Rhitta owner can use this command."
            );

            return true;
        }

        // --------------------------------------------------------
        // NO ARGUMENT
        // --------------------------------------------------------

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // ========================================================
        // HELP
        // ========================================================

        if (sub.equals("help")) {

            sendHelp(player);
            return true;
        }

        // ========================================================
        // ABILITY LIST
        // ========================================================

        if (sub.equals("ability")
                && args.length >= 2
                && args[1].equalsIgnoreCase("list")) {

            sendAbilityList(player);
            return true;
        }

        if (sub.equals("abilities")
                || sub.equals("abilitylist")) {

            sendAbilityList(player);
            return true;
        }

        // ========================================================
        // STATUS
        // ========================================================

        if (sub.equals("status")) {

            sendStatus(player);
            return true;
        }

        // ========================================================
        // SKILLS TRUE/FALSE
        //
        // /rhitta skills true
        // /rhitta skills false
        // ========================================================

        if (sub.equals("skills")) {

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.YELLOW +
                        "Rhitta skills are " +
                        (
                                manager.isSkillsEnabled()
                                        ? ChatColor.GREEN + "ENABLED"
                                        : ChatColor.RED + "DISABLED"
                        )
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Usage: /rhitta skills <true|false>"
                );

                return true;
            }

            // ENABLE
            if (args[1].equalsIgnoreCase("true")
                    || args[1].equalsIgnoreCase("on")
                    || args[1].equalsIgnoreCase("enable")) {

                manager.setSkillsEnabled(true);

                player.sendMessage(
                        ChatColor.GREEN +
                        "Rhitta hotbar skills ENABLED."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Select hotbar slot 1-7 and Right Click."
                );

                return true;
            }

            // DISABLE
            if (args[1].equalsIgnoreCase("false")
                    || args[1].equalsIgnoreCase("off")
                    || args[1].equalsIgnoreCase("disable")) {

                manager.setSkillsEnabled(false);

                player.sendMessage(
                        ChatColor.RED +
                        "Rhitta hotbar skills DISABLED."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.RED +
                    "Use: /rhitta skills true"
            );

            player.sendMessage(
                    ChatColor.RED +
                    "or: /rhitta skills false"
            );

            return true;
        }

        // ========================================================
        // BUFFS TRUE/FALSE
        //
        // Compatibility with old command.
        //
        // This controls the same hotbar skill system.
        // ========================================================

        if (sub.equals("buffs")) {

            if (args.length < 2) {

                player.sendMessage(
                        ChatColor.YELLOW +
                        "Rhitta skills are " +
                        (
                                manager.isSkillsEnabled()
                                        ? ChatColor.GREEN + "ENABLED"
                                        : ChatColor.RED + "DISABLED"
                        )
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Usage: /rhitta buffs <true|false>"
                );

                return true;
            }

            // ENABLE
            if (args[1].equalsIgnoreCase("true")
                    || args[1].equalsIgnoreCase("on")
                    || args[1].equalsIgnoreCase("enable")) {

                manager.setSkillsEnabled(true);

                player.sendMessage(
                        ChatColor.GREEN +
                        "Rhitta skills ENABLED."
                );

                return true;
            }

            // DISABLE
            if (args[1].equalsIgnoreCase("false")
                    || args[1].equalsIgnoreCase("off")
                    || args[1].equalsIgnoreCase("disable")) {

                manager.setSkillsEnabled(false);

                player.sendMessage(
                        ChatColor.RED +
                        "Rhitta skills DISABLED."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.RED +
                    "Usage: /rhitta buffs <true|false>"
            );

            return true;
        }

        // ========================================================
        // FIREBALL SHORTCUT
        // ========================================================

        if (sub.equals("0")) {

            player.sendMessage(
                    ChatColor.GOLD +
                    "Fireball is assigned to hotbar slot 1."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Hold Rhitta in your main hand, select slot 1, "
                    + "then Right Click."
            );

            return true;
        }

        // ========================================================
        // ABSOLUTE DOMINANCE
        // ========================================================

        if (sub.equals("ad")) {

            player.sendMessage(
                    ChatColor.RED +
                    "Absolute Dominance is assigned to hotbar slot 6."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Select slot 6 and Right Click."
            );

            return true;
        }

        // ========================================================
        // KING'S AURA
        // ========================================================

        if (sub.equals("ka")) {

            player.sendMessage(
                    ChatColor.GOLD +
                    "King's Aura is assigned to hotbar slot 2."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Select slot 2 and Right Click."
            );

            return true;
        }

        // ========================================================
        // UNBREAKABLE EGO
        // ========================================================

        if (sub.equals("ue")) {

            player.sendMessage(
                    ChatColor.BLUE +
                    "Unbreakable Ego is assigned to hotbar slot 3."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Select slot 3 and Right Click."
            );

            return true;
        }

        // ========================================================
        // PUNISHMENT OF THE PROUD
        // ========================================================

        if (sub.equals("pp")) {

            player.sendMessage(
                    ChatColor.RED +
                    "Punishment of the Proud is assigned to hotbar slot 4."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Select slot 4 and Right Click."
            );

            return true;
        }

        // ========================================================
        // PRIDE'S JUDGMENT
        // ========================================================

        if (sub.equals("pj")) {

            player.sendMessage(
                    ChatColor.DARK_PURPLE +
                    "Pride's Judgment is assigned to hotbar slot 5."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Select slot 5 and Right Click."
            );

            return true;
        }

        // ========================================================
        // KING'S AUTHORITY
        // ========================================================

        if (sub.equals("kau")) {

            player.sendMessage(
                    ChatColor.YELLOW +
                    "King's Authority Ultimate is assigned to hotbar slot 7."
            );

            player.sendMessage(
                    ChatColor.GRAY +
                    "Select slot 7 and Right Click."
            );

            return true;
        }

        // ========================================================
        // REMOVE
        // ========================================================

        if (sub.equals("remove")) {

            // ----------------------------------------------------
            // REMOVE DUPLICATES
            // ----------------------------------------------------

            if (args.length >= 2
                    && args[1].equalsIgnoreCase("dupes")) {

                player.sendMessage(
                        ChatColor.YELLOW +
                        "Scanning the server for duplicate Rhitta items..."
                );

                int removed =
                        manager.removeDuplicates();

                player.sendMessage(
                        ChatColor.GREEN +
                        "Rhitta duplicate cleanup complete."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Removed: "
                        + ChatColor.RED
                        + removed
                        + ChatColor.GRAY
                        + " duplicate Rhitta item(s)."
                );

                return true;
            }

            // ----------------------------------------------------
            // REMOVE RHITTA
            // ----------------------------------------------------

            if (args.length >= 2
                    && args[1].equalsIgnoreCase("rhitta")) {

                player.sendMessage(
                        ChatColor.YELLOW +
                        "Rhitta is permanently bound to its owner."
                );

                player.sendMessage(
                        ChatColor.GRAY +
                        "Use /rhitta remove dupes instead."
                );

                return true;
            }

            player.sendMessage(
                    ChatColor.RED +
                    "Usage: /rhitta remove dupes"
            );

            return true;
        }

        // ========================================================
        // UNKNOWN COMMAND
        // ========================================================

        player.sendMessage(
                ChatColor.RED +
                "Unknown Rhitta command."
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Use /rhitta help"
        );

        return true;
    }

    // ============================================================
    // HELP
    // ============================================================

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "             RHITTA"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "          Ability System"
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta ability list"
                + ChatColor.GRAY +
                " - Show all abilities"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta status"
                + ChatColor.GRAY +
                " - Check Rhitta status"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta skills true"
                + ChatColor.GRAY +
                " - Enable skills"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta skills false"
                + ChatColor.GRAY +
                " - Disable skills"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta remove dupes"
                + ChatColor.GRAY +
                " - Remove duplicate Rhittas"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta help"
                + ChatColor.GRAY +
                " - Show this menu"
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "HOTBAR CONTROLS"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Select slot 1-7, then "
                + ChatColor.YELLOW +
                "Right Click"
                + ChatColor.GRAY +
                " to activate."
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Rhitta must be held in your main hand."
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );
    }

    // ============================================================
    // ABILITY LIST
    // ============================================================

    private void sendAbilityList(Player player) {

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "          RHITTA ABILITIES"
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.RED +
                "1. FIREBALL"
                + ChatColor.GRAY +
                " - Powerful fire projectile"
        );

        player.sendMessage(
                ChatColor.RED +
                "2. KING'S AURA"
                + ChatColor.GRAY +
                " - Strengthens you and weakens enemies"
        );

        player.sendMessage(
                ChatColor.BLUE +
                "3. UNBREAKABLE EGO"
                + ChatColor.GRAY +
                " - Massive defensive power"
        );

        player.sendMessage(
                ChatColor.RED +
                "4. PUNISHMENT OF THE PROUD"
                + ChatColor.GRAY +
                " - Dash and destroy enemies"
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "5. PRIDE'S JUDGMENT"
                + ChatColor.GRAY +
                " - Divine straight-line attack"
        );

        player.sendMessage(
                ChatColor.RED +
                "6. ABSOLUTE DOMINANCE"
                + ChatColor.GRAY +
                " - Intimidates nearby enemies"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "7. KING'S AUTHORITY"
                + ChatColor.GRAY +
                " - Ultimate area attack"
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Select the corresponding hotbar slot."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Then Right Click to activate."
        );
    }

    // ============================================================
    // STATUS
    // ============================================================

    private void sendStatus(Player player) {

        boolean hasRhitta =
                manager.hasRhitta(player);

        boolean skillsEnabled =
                manager.isSkillsEnabled();

        int defense =
                manager.getDefense(player);

        int rhittaCount =
                manager.countRhitta(player);

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "           RHITTA STATUS"
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Owner: "
                + ChatColor.WHITE
                + player.getName()
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Rhitta: "
                + (
                        hasRhitta
                                ? ChatColor.GREEN + "YES"
                                : ChatColor.RED + "NO"
                )
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Rhitta Count: "
                + ChatColor.AQUA
                + rhittaCount
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Hotbar Skills: "
                + (
                        skillsEnabled
                                ? ChatColor.GREEN + "ENABLED"
                                : ChatColor.RED + "DISABLED"
                )
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Defense: "
                + ChatColor.AQUA
                + defense
                + ChatColor.GRAY
                + " points"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Damage Bonus: "
                + ChatColor.RED
                + "+20"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Life Steal: "
                + ChatColor.GREEN
                + "+4 HP"
        );

        player.sendMessage(
                ChatColor.DARK_PURPLE +
                "━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
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

        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        // --------------------------------------------------------
        // FIRST ARGUMENT
        // --------------------------------------------------------

        if (args.length == 1) {

            List<String> suggestions =
                    Arrays.asList(
                            "help",
                            "ability",
                            "abilities",
                            "abilitylist",
                            "status",
                            "skills",
                            "buffs",
                            "remove",
                            "0",
                            "AD",
                            "KA",
                            "UE",
                            "PP",
                            "PJ",
                            "KAU"
                    );

            return filter(
                    suggestions,
                    args[0]
            );
        }

        // --------------------------------------------------------
        // SECOND ARGUMENT
        // --------------------------------------------------------

        if (args.length == 2) {

            if (args[0].equalsIgnoreCase("ability")) {

                return filter(
                        Collections.singletonList("list"),
                        args[1]
                );
            }

            if (args[0].equalsIgnoreCase("skills")
                    || args[0].equalsIgnoreCase("buffs")) {

                return filter(
                        Arrays.asList(
                                "true",
                                "false"
                        ),
                        args[1]
                );
            }

            if (args[0].equalsIgnoreCase("remove")) {

                return filter(
                        Collections.singletonList("dupes"),
                        args[1]
                );
            }
        }

        return Collections.emptyList();
    }

    // ============================================================
    // TAB FILTER
    // ============================================================

    private List<String> filter(
            List<String> values,
            String input) {

        List<String> result =
                new ArrayList<>();

        String lower =
                input.toLowerCase();

        for (String value : values) {

            if (value.toLowerCase()
                    .startsWith(lower)) {

                result.add(value);
            }
        }

        return result;
    }
}