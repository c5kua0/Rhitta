package me.reno.rhitta;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

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
            String[] args
    ) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        RhittaManager manager = plugin.getRhittaManager();

        if (!manager.isOwner(player)) {
            player.sendMessage("§cRhitta does not recognize you as its owner.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§6Rhitta §7commands:");
            player.sendMessage("§e/rhitta give §7- Obtain Rhitta");
            return true;
        }

        if (args[0].equalsIgnoreCase("give")) {

            ItemStack rhitta = createRhitta();

            manager.markAsRhitta(rhitta);

            player.getInventory().addItem(rhitta);

            player.sendMessage("§6§lRHITTA §ehas chosen you.");
            player.sendMessage("§7The sacred axe has awakened.");

            return true;
        }

        player.sendMessage("§cUnknown Rhitta command.");
        return true;
    }

    private ItemStack createRhitta() {

        ItemStack item = new ItemStack(Material.NETHERITE_AXE);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName("§6§lRHITTA");

            meta.setLore(java.util.List.of(
                    "§7The sacred axe of the chosen one.",
                    "",
                    "§c♥ Life Steal",
                    "§b◆ Growing Defense",
                    "§e★ One-Time Resurrection",
                    "§6✦ 200% Power Awakening",
                    "",
                    "§8Owner: .ToshiroCyMc"
            ));

            meta.setUnbreakable(true);

            item.setItemMeta(meta);
        }

        return item;
    }
}
