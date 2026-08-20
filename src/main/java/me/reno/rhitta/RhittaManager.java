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

        rhittaKey =
                new NamespacedKey(plugin, "rhitta");

        rhittaIdKey =
                new NamespacedKey(plugin, "rhitta_id");

        resurrectionKey =
                new NamespacedKey(plugin, "resurrection_used");
    }

    // =====================================================
    // CREATE RHITTA
    // =====================================================

    public ItemStack createRhitta() {

        ItemStack item =
                new ItemStack(
                        Material.NETHERITE_SWORD,
                        1
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                "§6§lRHITTA"
        );

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

    // =====================================================
    // CHECK RHITTA
    // =====================================================

    public boolean isRhitta(ItemStack item) {

        if (item == null ||
                item.getType() == Material.AIR) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

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

    // =====================================================
    // OWNER
    // =====================================================

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

            owner =
                    player.getUniqueId();

            return true;
        }

        return owner.equals(
                player.getUniqueId()
        );
    }

    public void setOwner(UUID uuid) {

        owner = uuid;
    }

    public UUID getOwner() {

        return owner;
    }

    // =====================================================
    // GIVE / FORCE ONE RHITTA
    // =====================================================

    public void giveRhitta(Player player) {

        if (!isAllowedOwner(player)) {
            return;
        }

        owner = player.getUniqueId();

        /*
         * FORCE EXACTLY ONE.
         */
        forceOneRhitta(player);
    }

    public void forceOneRhitta(Player player) {

        if (player == null ||
                !isAllowedOwner(player)) {
            return;
        }

        boolean found = false;

        /*
         * MAIN INVENTORY + HOTBAR
         */
        ItemStack[] contents =
                player.getInventory()
                        .getContents();

        for (int slot = 0;
             slot < contents.length;
             slot++) {

            Item