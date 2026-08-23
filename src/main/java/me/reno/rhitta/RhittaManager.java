package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
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

    private final Map<UUID, Integer> defense =
            new HashMap<>();

    private final Map<UUID, String> activeAbilities =
            new HashMap<>();

    private final Map<String, Long> cooldowns =
            new HashMap<>();

    private final NamespacedKey rhittaKey;
    private final NamespacedKey rhittaIdKey;

    private UUID owner;
    private long rhittaCounter = 0L;

    private boolean buffsEnabled;

    public RhittaManager(RhittaPlugin plugin) {

        this.plugin = plugin;

        rhittaKey =
                new NamespacedKey(
                        plugin,
                        "rhitta"
                );

        rhittaIdKey =
                new NamespacedKey(
                        plugin,
                        "rhitta_id"
                );

        buffsEnabled =
                plugin.getConfig()
                        .getBoolean(
                                "buffs.enabled-by-default",
                                true
                        );
    }

    // ============================================================
    // OWNER
    // ============================================================

    public boolean isAllowedOwner(Player player) {

        if (owner != null) {
            return player.getUniqueId()
                    .equals(owner);
        }

        if (player.getName()
                .equalsIgnoreCase(OWNER_NAME)) {

            owner = player.getUniqueId();

            return true;
        }

        return false;
    }

    public boolean isOwner(Player player) {

        return owner != null &&
                player.getUniqueId()
                        .equals(owner);
    }

    public UUID getOwner() {
        return owner;
    }

    // ============================================================
    // RHITTA ITEM
    // ============================================================

    public ItemStack createRhitta() {

        ItemStack item =
                new ItemStack(
                        Material.NETHERITE_SWORD
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                ChatColor.GOLD +
                "" +
                ChatColor.BOLD +
                "RHITTA"
        );

        meta.setUnbreakable(true);

        rhittaCounter++;

        String id =
                "RHITTA-" +
                rhittaCounter +
                "-" +
                UUID.randomUUID();

        meta.getPersistentDataContainer()
                .set(
                        rhittaKey,
                        PersistentDataType.BYTE,
                        (byte) 1
                );

        meta.getPersistentDataContainer()
                .set(
                        rhittaIdKey,
                        PersistentDataType.STRING,
                        id
                );

        item.setItemMeta(meta);

        return item;
    }

    public boolean isRhitta(ItemStack item) {

        if (item == null ||
                item.getType().isAir()) {

            return false;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte flag =
                meta.getPersistentDataContainer()
                        .get(
                                rhittaKey,
                                PersistentDataType.BYTE
                        );

        return flag != null &&
                flag == (byte) 1;
    }

    // ============================================================
    // INVENTORY
    // ============================================================

    public boolean hasRhitta(Player player) {

        for (ItemStack item :
                player.getInventory()
                        .getContents()) {

            if (isRhitta(item)) {
                return true;
            }
        }

        return isRhitta(
                player.getInventory()
                        .getItemInOffHand()
        );
    }

    public int countRhitta(Player player) {

        int count = 0;

        for (ItemStack item :
                player.getInventory()
                        .getContents()) {

            if (isRhitta(item)) {
                count++;
            }
        }

        if (isRhitta(
                player.getInventory()
                        .getItemInOffHand())) {

            count++;
        }

        return count;
    }

    public void giveRhitta(Player player) {

        if (!isOwner(player)) {
            return;
        }

        ItemStack rhitta =
                createRhitta();

        PlayerInventory inventory =
                player.getInventory();

        if (inventory.getItemInMainHand()
                .getType().isAir()) {

            inventory.setItemInMainHand(rhitta);
            return;
        }

        if (inventory.getItemInOffHand()
                .getType().isAir()) {

            inventory.setItemInOffHand(rhitta);
            return;
        }

        inventory.addItem(rhitta);
    }

    public void forceOneRhitta(Player player) {

        if (!isOwner(player)) {
            return;
        }

        if (!hasRhitta(player)) {
            giveRhitta(player);
        }
    }

    // ============================================================
    // MANUAL DUPE REMOVER
    // ============================================================

    public int removeDuplicates() {

        int removed = 0;

        for (Player player :
                plugin.getServer()
                        .getOnlinePlayers()) {

            if (isOwner(player)) {
                removed +=
                        removeExtraCopies(player);
            } else {
                removed +=
                        removeAllRhitta(player);
            }
        }

        // Remove dropped copies
        for (World world :
                plugin.getServer().getWorlds()) {

            for (Entity entity :
                    world.getEntities()) {

                if (!(entity instanceof Item)) {
                    continue;
                }

                Item dropped =
                        (Item) entity;

                if (!isRhitta(
                        dropped.getItemStack())) {

                    continue;
                }

                removed +=
                        dropped.getItemStack()
                                .getAmount();

                dropped.remove();
            }
        }

        return removed;
    }

    private int removeExtraCopies(Player player) {

        int removed = 0;
        boolean kept = false;

        PlayerInventory inventory =
                player.getInventory();

        ItemStack[] contents =
                inventory.getContents();

        for (int i = 0;
                i < contents.length;
                i++) {

            ItemStack item = contents[i];

            if (!isRhitta(item)) {
                continue;
            }

            if (!kept) {
                kept = true;
                continue;
            }

            removed += item.getAmount();

            contents[i] = null;
        }

        inventory.setContents(contents);

        ItemStack offhand =
                inventory.getItemInOffHand();

        if (isRhitta(offhand)) {

            if (!kept) {
                kept = true;
            } else {

                removed +=
                        offhand.getAmount();

                inventory.setItemInOffHand(null);
            }
        }

        // Ender Chest
        ItemStack[] ender =
                player.getEnderChest()
                        .getContents();

        for (int i = 0;
                i < ender.length;
                i++) {

            if (!isRhitta(ender[i])) {
                continue;
            }

            removed +=
                    ender[i].getAmount();

            ender[i] = null;
        }

        player.getEnderChest()
                .setContents(ender);

        if (!kept) {
            giveRhitta(player);
        }

        return removed;
    }

    private int removeAllRhitta(Player player) {

        int removed = 0;

        PlayerInventory inventory =
                player.getInventory();

        ItemStack[] contents =
                inventory.getContents();

        for (int i = 0;
                i < contents.length;
                i++) {

            if (!isRhitta(contents[i])) {
                continue;
            }

            removed +=
                    contents[i].getAmount();

            contents[i] = null;
        }

        inventory.setContents(contents);

        ItemStack offhand =
                inventory.getItemInOffHand();

        if (isRhitta(offhand)) {

            removed +=
                    offhand.getAmount();

            inventory.setItemInOffHand(null);
        }

        ItemStack[] ender =
                player.getEnderChest()
                        .getContents();

        for (int i = 0;
                i < ender.length;
                i++) {

            if (!isRhitta(ender[i])) {
                continue;
            }

            removed +=
                    ender[i].getAmount();

            ender[i] = null;
        }

        player.getEnderChest()
                .setContents(ender);

        return removed;
    }

    // ============================================================
    // DEFENSE
    // ============================================================

    public int getDefense(Player player) {

        return defense.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    public void addDefense(
            Player player,
            int amount) {

        if (amount <= 0) {
            return;
        }

        defense.merge(
                player.getUniqueId(),
                amount,
                Integer::sum
        );
    }

    // ============================================================
    // BUFFS
    // ============================================================

    public boolean isBuffsEnabled() {
        return buffsEnabled;
    }

    public void setBuffsEnabled(
            boolean enabled) {

        buffsEnabled = enabled;
    }

    // ============================================================
    // ABILITIES
    // ============================================================

    public void setAbilityActive(
            Player player,
            String ability) {

        activeAbilities.put(
                player.getUniqueId(),
                ability
        );
    }

    public String getActiveAbility(
            Player player) {

        return activeAbilities.get(
                player.getUniqueId()
        );
    }

    public void clearAbility(
            Player player) {

        activeAbilities.remove(
                player.getUniqueId()
        );
    }

    // ============================================================
    // COOLDOWN
    // ============================================================

    private String cooldownKey(
            Player player,
            String ability) {

        return player.getUniqueId()
                .toString()
                + ":"
                + ability.toUpperCase();
    }

    public boolean isOnCooldown(
            Player player,
            String ability) {

        Long end =
                cooldowns.get(
                        cooldownKey(
                                player,
                                ability
                        )
                );

        return end != null &&
                System.currentTimeMillis()
                        < end;
    }

    public void setCooldown(
            Player player,
            String ability,
            long milliseconds) {

        cooldowns.put(
                cooldownKey(
                        player,
                        ability
                ),
                System.currentTimeMillis()
                        + milliseconds
        );
    }

    public long getCooldownRemaining(
            Player player,
            String ability) {

        Long end =
                cooldowns.get(
                        cooldownKey(
                                player,
                                ability
                        )
                );

        if (end == null) {
            return 0;
        }

        return Math.max(
                0,
                end -
                System.currentTimeMillis()
        );
    }
}