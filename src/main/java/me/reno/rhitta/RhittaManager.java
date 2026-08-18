package me.reno.rhitta;

import org.bukkit.NamespacedKey;
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

    private final Set<UUID> resurrected = new HashSet<>();
    private final Set<UUID> resurrectionPending = new HashSet<>();

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;

        this.rhittaKey = new NamespacedKey(plugin, "rhitta");
        this.ownerKey = new NamespacedKey(plugin, "rhitta_owner");
    }

    public void markAsRhitta(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(
                rhittaKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        item.setItemMeta(meta);
    }

    public boolean isRhitta(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        Byte value = item.getItemMeta()
                .getPersistentDataContainer()
                .get(rhittaKey, PersistentDataType.BYTE);

        return value != null && value == (byte) 1;
    }

    public void setOwner(ItemStack item, Player player) {
        if (item == null || player == null) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        meta.getPersistentDataContainer().set(
                ownerKey,
                PersistentDataType.STRING,
                player.getUniqueId().toString()
        );

        item.setItemMeta(meta);
    }

    public boolean isOwner(Player player) {
        if (player == null) {
            return false;
        }

        return player.getInventory().containsAtLeast(
                createRhitta(),
                1
        );
    }

    public ItemStack createRhitta() {
        org.bukkit.Material material = org.bukkit.Material.NETHERITE_SWORD;

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§lRhitta");

            meta.getPersistentDataContainer().set(
                    rhittaKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean canResurrect(Player player) {
        if (player == null) {
            return false;
        }

        return !resurrected.contains(player.getUniqueId());
    }

    public void prepareResurrection(Player player) {
        if (player == null) {
            return;
        }

        resurrectionPending.add(player.getUniqueId());
    }

    public boolean isResurrectionPending(Player player) {
        if (player == null) {
            return false;
        }

        return resurrectionPending.contains(player.getUniqueId());
    }

    public void completeResurrection(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        resurrectionPending.remove(uuid);
        resurrected.add(uuid);
    }

    public void resetResurrection(Player player) {
        if (player == null) {
            return;
        }

        UUID uuid = player.getUniqueId();

        resurrected.remove(uuid);
        resurrectionPending.remove(uuid);
    }
    }
