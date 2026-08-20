package me.reno.rhitta;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RhittaManager {

    private static final String OWNER_NAME = "_ToshiroCyMc";

    private final RhittaPlugin plugin;

    private final NamespacedKey rhittaKey;
    private final NamespacedKey rhittaIdKey;
    private final NamespacedKey resurrectionKey;

    private final Map<UUID, Integer> defense = new HashMap<>();

    private UUID owner;

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;

        rhittaKey = new NamespacedKey(plugin, "rhitta");
        rhittaIdKey = new NamespacedKey(plugin, "rhitta_id");
        resurrectionKey = new NamespacedKey(plugin, "resurrection_used");
    }

    public ItemStack createRhitta() {

        ItemStack item =
                new ItemStack(Material.NETHERITE_SWORD);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName("§6§lRHITTA");
        meta.setUnbreakable(true);

        meta.getPersistentDataContainer().set(
                rhittaKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                rhittaIdKey,
                PersistentDataType.STRING,
                "RHITTA-" + plugin.getName()
        );

        item.setItemMeta(meta);

        return item;
    }

    public boolean isRhitta(ItemStack item) {

        if (item == null ||
                item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value =
                meta.getPersistentDataContainer().get(
                        rhittaKey,
                        PersistentDataType.BYTE
                );

        String id =
                meta.getPersistentDataContainer().get(
                        rhittaIdKey,
                        PersistentDataType.STRING
                );

        return value != null
                && value == (byte) 1
                && ("RHITTA-" + plugin.getName())
                .equals(id);
    }

    public boolean isAllowedOwner(Player player) {

        return player != null
                && player.getName()
                .equalsIgnoreCase(OWNER_NAME);
    }

    public boolean isOwner(Player player) {

        if (!isAllowedOwner(player)) {
            return false;
        }

        if (owner == null) {
            owner = player.getUniqueId();
            return true;
        }

        return owner.equals(player.getUniqueId());
    }

    public void setOwner(UUID uuid) {
        owner = uuid;
    }

    public UUID getOwner() {
        return owner;
    }

    // =====================================================
    // RHITTA
    // =====================================================

    public void giveRhitta(Player player) {

        if (!isAllowedOwner(player)) {
            return;
        }

        removeDuplicateRhittas(player);

        if (!hasRhitta(player)) {

            player.getInventory().addItem(
                    createRhitta()
            );
        }

        owner = player.getUniqueId();
    }

    public boolean hasRhitta(Player player) {

        if (player == null) {
            return false;
        }

        for (ItemStack item :
                player.getInventory().getContents()) {

            if (isRhitta(item)) {
                return true;
            }
        }

        return isRhitta(
                player.getInventory()
                        .getItemInOffHand()
        );
    }

    // =====================================================
    // ANTI DUPE
    // =====================================================

    public void removeDuplicateRhittas(Player player) {

        if (player == null) {
            return;
        }

        boolean found = false;

        ItemStack[] contents =
                player.getInventory().getContents();

        for (int slot = 0;
             slot < contents.length;
             slot++) {

            ItemStack item = contents[slot];

            if (!isRhitta(item)) {
                continue;
            }

            if (!found) {

                found = true;

            } else {

                // Remove duplicate.
                player.getInventory()
                        .setItem(slot, null);
            }
        }

        ItemStack offhand =
                player.getInventory()
                        .getItemInOffHand();

        if (isRhitta(offhand)) {

            if (!found) {

                found = true;

            } else {

                player.getInventory()
                        .setItemInOffHand(null);
            }
        }
    }

    public int removeRhitta(Player player) {

        int removed = 0;

        ItemStack[] contents =
                player.getInventory().getContents();

        for (int slot = 0;
             slot < contents.length;
             slot++) {

            ItemStack item = contents[slot];

            if (isRhitta(item)) {

                removed += item.getAmount();

                player.getInventory()
                        .setItem(slot, null);
            }
        }

        ItemStack offhand =
                player.getInventory()
                        .getItemInOffHand();

        if (isRhitta(offhand)) {

            removed += offhand.getAmount();

            player.getInventory()
                    .setItemInOffHand(null);
        }

        return removed;
    }

    // =====================================================
    // DEFENSE
    // =====================================================

    public int getDefense(Player player) {

        return defense.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    public void addDefense(Player player) {

        UUID uuid = player.getUniqueId();

        int current =
                defense.getOrDefault(uuid, 0);

        defense.put(
                uuid,
                current + 1
        );
    }

    // =====================================================
    // RESURRECTION
    // =====================================================

    public boolean hasUsedResurrection(Player player) {

        Byte value =
                player.getPersistentDataContainer()
                        .get(
                                resurrectionKey,
                                PersistentDataType.BYTE
                        );

        return value != null
                && value == (byte) 1;
    }

    public void markResurrectionUsed(Player player) {

        player.getPersistentDataContainer()
                .set(
                        resurrectionKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );
    }

    public void resetResurrection(Player player) {

        player.getPersistentDataContainer()
                .remove(resurrectionKey);
    }
}