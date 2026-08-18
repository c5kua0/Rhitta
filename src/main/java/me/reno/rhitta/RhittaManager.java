package me.reno.rhitta;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class RhittaManager {

    private final RhittaPlugin plugin;

    private final NamespacedKey rhittaKey;
    private final NamespacedKey ownerKey;

    private static final String OWNER_NAME = ".ToshiroCyMc";

    private final Set<UUID> resurrectionUsed = new HashSet<>();
    private final Set<UUID> resurrectionPending = new HashSet<>();

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;

        rhittaKey = new NamespacedKey(plugin, "rhitta");
        ownerKey = new NamespacedKey(plugin, "rhitta_owner");
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

        return item.getItemMeta()
                .getPersistentDataContainer()
                .has(rhittaKey, PersistentDataType.BYTE);
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

    public void addDefense(Player player, int amount) {
        AttributeInstance attribute =
                player.getAttribute(Attribute.ARMOR);

        if (attribute == null) {
            return;
        }

        double newValue = Math.min(
                attribute.getBaseValue() + amount,
                40.0
        );

        attribute.setBaseValue(newValue);
    }

    public void repairRhitta(ItemStack item, int amount) {
        if (item == null || !isRhitta(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        if (!meta.isUnbreakable()) {
            int newDamage = Math.max(
                    0,
                    meta.getDamage() - amount
            );

            meta.setDamage(newDamage);
            item.setItemMeta(meta);
        }
    }

    public boolean hasResurrection(Player player) {
        return !resurrectionUsed.contains(player.getUniqueId());
    }

    public void prepareResurrection(Player player) {
        UUID uuid = player.getUniqueId();

        resurrectionUsed.add(uuid);
        resurrectionPending.add(uuid);
    }

    public boolean isResurrectionPending(Player player) {
        return resurrectionPending.contains(player.getUniqueId());
    }

    public void completeResurrection(Player player) {
        resurrectionPending.remove(player.getUniqueId());
    }

    public void resetResurrection(Player player) {
        resurrectionUsed.remove(player.getUniqueId());
        resurrectionPending.remove(player.getUniqueId());
    }

    public NamespacedKey getRhittaKey() {
        return rhittaKey;
    }

    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }
}
