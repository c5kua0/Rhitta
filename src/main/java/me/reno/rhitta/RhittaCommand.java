package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RhittaCommand
        implements CommandExecutor {

    private final RhittaPlugin plugin;

    public RhittaCommand(
            RhittaPlugin plugin) {

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

        Player player =
                (Player) sender;

        RhittaManager manager =
                plugin.getRhittaManager();

        // ========================================================
        // HELP
        // ========================================================

        if (args.length == 0 ||
                args[0].equalsIgnoreCase("help")) {

            sendHelp(player);

            return true;
        }

        // ========================================================
        // STATUS
        // ========================================================

        if (args[0].equalsIgnoreCase("status")) {

            sendStatus(player);

            return true;
        }

        // ========================================================
        // BUFF
        // ========================================================

        if (args[0].equalsIgnoreCase("buff")) {

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
                        "Usage: /rhitta buff <true|false>"
                );

                return true;
            }

            boolean enabled =
                    Boolean.parseBoolean(
                            args[1]
                    );

            manager.setBuffsEnabled(
                    enabled
            );

            player.sendMessage(
                    ChatColor.GOLD +
                    "Rhitta buffs: " +
                    (enabled
                            ? ChatColor.GREEN +
                            "ENABLED"
                            : ChatColor.RED +
                            "DISABLED")
            );

            return true;
        }

        // ========================================================
        // GIVE
        // ========================================================

        if (args[0].equalsIgnoreCase("give")) {

            if (!manager.isOwner(player)) {

                player.sendMessage(
                        ChatColor.RED +
                        "You are not the owner of Rhitta."
                );

                return true;
            }

            manager.giveRhitta(player);

            player.sendMessage(
                    ChatColor.GOLD +
                    "Rhitta restored!"
            );

            return true;
        }

        // ========================================================
        // CLEAN
        // ========================================================

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
                    "Rhitta cleanup complete. " +
                    ChatColor.YELLOW +
                    removed +
                    ChatColor.GOLD +
                    " duplicate item(s) removed."
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
                ChatColor.YELLOW +
                "Use /rhitta help"
        );

        return true;
    }

    // ============================================================
    // HELP
    // ============================================================

    private void sendHelp(Player player) {

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "===== RHITTA HELP ====="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta status" +
                ChatColor.WHITE +
                " - View your Rhitta status."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta give" +
                ChatColor.WHITE +
                " - Restore Rhitta."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta clean" +
                ChatColor.WHITE +
                " - Remove duplicate Rhittas."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "/rhitta buff <true|false>" +
                ChatColor.WHITE +
                " - Toggle Rhitta buffs."
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "===== ABILITIES ====="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Left Click" +
                ChatColor.WHITE +
                " - Physical Attack +20, Life Steal and Defense Growth."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Right Click" +
                ChatColor.WHITE +
                " - Select the next ability."
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Shift + Right Click" +
                ChatColor.WHITE +
                " - Activate selected ability."
        );

        player.sendMessage("");

        player.sendMessage(
                ChatColor.GOLD +
                "🔥 Fireball" +
                ChatColor.WHITE +
                " - Fires a projectile that damages the target."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "♛ King Aura" +
                ChatColor.WHITE +
                " - Damages and weakens nearby hostile mobs."
        );

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "======================"
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
                "===== RHITTA STATUS ====="
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Owner: " +
                ChatColor.WHITE +
                RhittaManager.OWNER_NAME
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Rhitta: " +
                (manager.isOwner(player)
                        && manager.hasRhitta(player)
                        ? ChatColor.GREEN +
                        "Owned"
                        : ChatColor.RED +
                        "Not Owned")
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Physical Attack: " +
                ChatColor.WHITE +
                "+20"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Life Steal: " +
                ChatColor.WHITE +
                LIFE_STEAL_TEXT()
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Defense: " +
                ChatColor.WHITE +
                manager.getDefense(player)
        );

        String active =
                manager.getActiveAbility(player);

        player.sendMessage(
                ChatColor.YELLOW +
                "Selected Ability: " +
                ChatColor.WHITE +
                (active == null
                        ? "None"
                        : formatAbility(active))
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Buffs: " +
                (manager.isBuffsEnabled()
                        ? ChatColor.GREEN +
                        "Enabled"
                        : ChatColor.RED +
                        "Disabled")
        );

        long fireball =
                manager.getCooldownRemaining(
                        player,
                        "FIREBALL"
                );

        player.sendMessage(
                ChatColor.YELLOW +
                "Fireball: " +
                cooldownText(fireball)
        );

        long aura =
                manager.getCooldownRemaining(
                        player,
                        "KING_AURA"
                );

        player.sendMessage(
                ChatColor.YELLOW +
                "King Aura: " +
                cooldownText(aura)
        );

        player.sendMessage(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "========================"
        );
    }

    private String cooldownText(
            long milliseconds) {

        if (milliseconds <= 0) {

            return ChatColor.GREEN +
                    "Ready";
        }

        return ChatColor.RED +
                String.format(
                        "%.1fs",
                        milliseconds / 1000.0
                );
    }

    private String formatAbility(
            String ability) {

        if (ability == null) {
            return "None";
        }

        switch (ability.toUpperCase()) {

            case "FIREBALL":
                return "Fireball";

            case "KING_AURA":
                return "King Aura";

            default:
                return ability;
        }
    }

    private String LIFE_STEAL_TEXT() {

        return "4 HP";
    }
}