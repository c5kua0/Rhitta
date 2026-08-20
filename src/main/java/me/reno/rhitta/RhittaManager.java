package me.reno.rhitta;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RhittaManager {

    public static final String OWNER_NAME = "_ToshiroCyMc";

    private final RhittaPlugin plugin;
    private final Map<UUID, Integer> defense = new HashMap<>();

    private final NamespacedKey rhittaKey;
    private final NamespacedKey rhittaIdKey;
    private final NamespacedKey resurrectionKey;

    private UUID owner;
    private long rhittaCounter = 0L;

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;
        this.rhittaKey = new NamespacedKey(plugin, "rhitta");
        this.rhittaIdKey = new NamespacedKey(plugin, "rhitta_id");
        this.resurrectionKey = new NamespacedKey(plugin, "resurrection_used");
    }

    // ----------------------------------------------------------------
    // Ownership
    // ----------------------------------------------------------------

    public boolean isAllowedOwner(Player player) {
        if (owner != null) {
            return player.getUniqueId().equals(owner);
        }
        // First-time setup: whoever holds the configured owner name claims it.
        if (player.getName().equalsIgnoreCase(OWNER_NAME)) {
            setOwner(player.getUniqueId());
            return true;
        }
        return false;
    }

    public boolean isOwner(Player player) {
        return owner != null && player.getUniqueId().equals(owner);
    }

    public void setOwner(UUID uuid) {
        this.owner = uuid;
    }

    public UUID getOwner() {
        return owner;
    }

    // ----------------------------------------------------------------
    // Item creation / identification
    // ----------------------------------------------------------------

    public ItemStack createRhitta() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("\u00a76\u00a7lRHITTA");
        meta.setUnbreakable(true);

        rhittaCounter++;
        String id = "RHITTA-" + rhittaCounter;

        meta.getPersistentDataContainer().set(rhittaKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(rhittaIdKey, PersistentDataType.STRING, id);

        item.setItemMeta(meta);
        return item;
    }

    public boolean isRhitta(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte flag = meta.getPersistentDataContainer().get(rhittaKey, PersistentDataType.BYTE);
        return flag != null && flag == (byte) 1;
    }

    public void makeUnbreakable(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
    }

    // ----------------------------------------------------------------
    // Give / enforce single copy
    // ----------------------------------------------------------------

    public boolean hasRhitta(Player player) {
        PlayerInventory inv = player.getInventory();
        for (ItemStack stack : inv.getContents()) {
            if (isRhitta(stack)) return true;
        }
        return isRhitta(inv.getItemInOffHand());
    }

    /**
     * Removes every Rhitta copy currently in the player's inventory
     * (main contents + off hand). Used to guarantee we never end up
     * holding more than one, no matter how a dupe was attempted.
     */
    public int stripAllRhitta(Player player) {
        int removed = 0;
        PlayerInventory inv = player.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (isRhitta(contents[i])) {
                contents[i] = null;
                removed++;
            }
        }
        inv.setContents(contents);

        if (isRhitta(inv.getItemInOffHand())) {
            inv.setItemInOffHand(null);
            removed++;
        }
        return removed;
    }

    public void giveRhitta(Player player) {
        // Guarantee we never stack a second copy on top of an existing one.
        stripAllRhitta(player);

        PlayerInventory inv = player.getInventory();
        ItemStack rhitta = createRhitta();

        if (inv.getItemInOffHand() == null || inv.getItemInOffHand().getType() == Material.AIR) {
            inv.setItemInOffHand(rhitta);
            return;
        }

        HashMap<Integer, ItemStack> overflow = inv.addItem(rhitta);
        if (!overflow.isEmpty()) {
            // Inventory was full - drop it at the player's feet rather than lose/dupe it.
            player.getWorld().dropItemNaturally(player.getLocation(), rhitta);
        }
    }

    /**
     * Called on join/respawn: ensures the owner ends up with exactly
     * one Rhitta, never zero, never more than one.
     */
    public void forceOneRhitta(Player player) {
        if (!isOwner(player)) {
            // Non-owners should never be holding Rhitta at all.
            stripAllRhitta(player);
            return;
        }

        int count = countRhitta(player);
        if (count == 0) {
            giveRhitta(player);
        } else if (count > 1) {
            stripAllRhitta(player);
            giveRhitta(player);
        }
    }

    private int countRhitta(Player player) {
        int count = 0;
        PlayerInventory inv = player.getInventory();
        for (ItemStack stack : inv.getContents()) {
            if (isRhitta(stack)) count++;
        }
        if (isRhitta(inv.getItemInOffHand())) count++;
        return count;
    }

    // ----------------------------------------------------------------
    // Defense stacking
    // ----------------------------------------------------------------

    public int getDefense(Player player) {
        return defense.getOrDefault(player.getUniqueId(), 0);
    }

    public void addDefense(Player player, int amount) {
        int current = getDefense(player);
        defense.put(player.getUniqueId(), current + amount);
    }

    public void resetDefense(Player player) {
        defense.remove(player.getUniqueId());
    }

    // ----------------------------------------------------------------
    // Resurrection (once-per-life)
    // ----------------------------------------------------------------

    public boolean hasUsedResurrection(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte value = meta.getPersistentDataContainer().get(resurrectionKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public void markResurrectionUsed(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(resurrectionKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public void resetResurrection(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        meta.getPersistentDataContainer().set(resurrectionKey, PersistentDataType.BYTE, (byte) 0);
        item.setItemMeta(meta);
    }
}