package me.reno.rhitta;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RhittaManager {

    private final RhittaPlugin plugin;

    private final NamespacedKey rhittaKey;
    private final NamespacedKey defenseKey;
    private final NamespacedKey resurrectionKey;

    private static final String OWNER = ".ToshiroCyMc";

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;

        rhittaKey = new NamespacedKey(plugin, "rhitta_weapon");
        defenseKey = new NamespacedKey(plugin, "rhitta_defense");
        resurrectionKey = new NamespacedKey(plugin, "rhitta_resurrection_used");
    }

    public boolean isOwner(Player player) {
        return player.getName().equalsIgnoreCase(OWNER);
    }

    public ItemStack createRhitta() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);

        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§6§lRHITTA");

        meta.getPersistentDataContainer().set(
                rhittaKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);

        return item;
    }

    public boolean isRhitta(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        Byte value = meta.getPersistentDataContainer().get(
                rhittaKey,
                PersistentDataType.BYTE
        );

        return value != null && value == (byte) 1;
    }

    public boolean hasRhitta(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (isRhitta(item)) {
                return true;
            }
        }

        return false;
    }

    public void giveRhitta(Player player) {
        if (!isOwner(player)) {
            return;
        }

        if (!hasRhitta(player)) {
            player.getInventory().addItem(createRhitta());
        }
    }

    public int getDefense(Player player) {
        Integer value = player.getPersistentDataContainer().get(
                defenseKey,
                PersistentDataType.INTEGER
        );

        return value == null ? 0 : value;
    }

    public void addDefense(Player player) {
        int defense = getDefense(player);

        if (defense < 50) {
            defense++;
        }

        player.getPersistentDataContainer().set(
                defenseKey,
                PersistentDataType.INTEGER,
                defense
        );
    }

    public boolean hasUsedResurrection(Player player) {
        Byte value = player.getPersistentDataContainer().get(
                resurrectionKey,
                PersistentDataType.BYTE
        );

        return value != null && value == (byte) 1;
    }

    public void markResurrectionUsed(Player player) {
        player.getPersistentDataContainer().set(
                resurrectionKey,
                PersistentDataType.BYTE,
                (byte) 1
        );
    }

    public NamespacedKey getRhittaKey() {
        return rhittaKey;
    }

    public String getOwnerName() {
        return OWNER;
    }
}
