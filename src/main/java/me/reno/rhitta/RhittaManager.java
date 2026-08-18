package me.reno.rhitta;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class RhittaManager {

    private final RhittaPlugin plugin;

    private final NamespacedKey rhittaKey;
    private final NamespacedKey ownerKey;

    // EXACT OWNER
    private static final String OWNER_NAME = ".ToshiroCyMc";

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;

        this.rhittaKey = new NamespacedKey(plugin, "rhitta");
        this.ownerKey = new NamespacedKey(plugin, "rhitta_owner");
    }

    public boolean isOwner(Player player) {
        return player.getName().equalsIgnoreCase(OWNER_NAME);
    }

    public String getOwnerName() {
        return OWNER_NAME;
    }

    public boolean isRhitta(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        return meta.getPersistentDataContainer().has(
                rhittaKey,
                PersistentDataType.BYTE
        );
    }

    public void markAsRhitta(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(
                rhittaKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                ownerKey,
                PersistentDataType.STRING,
                OWNER_NAME
        );

        item.setItemMeta(meta);
    }

    public boolean belongsToOwner(ItemStack item) {
        if (!isRhitta(item) || !item.hasItemMeta()) {
            return false;
        }

        String owner = item.getItemMeta()
                .getPersistentDataContainer()
                .get(ownerKey, PersistentDataType.STRING);

        return OWNER_NAME.equalsIgnoreCase(owner);
    }

    public NamespacedKey getRhittaKey() {
        return rhittaKey;
    }

    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }
            }
