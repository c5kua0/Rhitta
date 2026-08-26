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

    private final Map<UUID, Integer> defense = new HashMap<>();
    private final Map<UUID, String> activeAbilities = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    private final NamespacedKey rhittaKey;
    private final NamespacedKey rhittaIdKey;

    private long rhittaCounter = 0L;

    private boolean buffsEnabled;
    private boolean skillsEnabled = true;

    public RhittaManager(RhittaPlugin plugin) {

        this.plugin = plugin;

        rhittaKey = new NamespacedKey(plugin, "rhitta");
        rhittaIdKey = new NamespacedKey(plugin, "rhitta_id");

        buffsEnabled = plugin.getConfig()
                .getBoolean("buffs.enabled-by-default", true);
    }

    // ============================================================
    // OWNER
    // ============================================================

    public boolean isOwner(Player player) {
        return player.getName().equalsIgnoreCase(OWNER_NAME);
    }

    public boolean isAllowedOwner(Player player) {
        return isOwner(player);
    }

    public String getOwnerName() {
        return OWNER_NAME;
    }

    // ============================================================
    // RHITTA ITEM
    // ============================================================

    public ItemStack createRhitta() {

        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.setDisplayName(
                ChatColor.GOLD + "" +
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

        meta.getPersistentDataContainer().set(
                rhittaKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                rhittaIdKey,
                PersistentDataType.STRING,
                id
        );

        item.setItemMeta(meta);

        return item;
    }

    public boolean isRhitta(ItemStack item) {

        if (item == null || item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte flag = meta.getPersistentDataContainer().get(
                rhittaKey,
                PersistentDataType.BYTE
        );

        return flag != null && flag == (byte) 1;
    }

    // ============================================================
    // INVENTORY
    // ============================================================

    public boolean hasRhitta(Player player) {

        PlayerInventory inventory = player.getInventory();

        for (ItemStack item : inventory.getContents()) {
            if (isRhitta(item)) {
                return true;
            }
        }

        return isRhitta(inventory.getItemInOffHand());
    }

    public int countRhitta(Player player) {

        int count = 0;

        PlayerInventory inventory = player.getInventory();

        for (ItemStack item : inventory.getContents()) {
            if (isRhitta(item)) {
                count += item.getAmount();
            }
        }

        ItemStack offhand = inventory.getItemInOffHand();

        if (isRhitta(offhand)) {
            count += offhand.getAmount();
        }

        for (ItemStack item : player.getEnderChest().getContents()) {
            if (isRhitta(item)) {
                count += item.getAmount();
            }
        }

        return count;
    }

    public void giveRhitta(Player player) {

        if (!isOwner(player)) {
            return;
        }

        if (hasRhitta(player)) {
            return;
        }

        ItemStack rhitta = createRhitta();

        PlayerInventory inventory = player.getInventory();

        if (inventory.getItemInMainHand().getType().isAir()) {
            inventory.setItemInMainHand(rhitta);
            return;
        }

        if (inventory.getItemInOffHand().getType().isAir()) {
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
    // DUPLICATE REMOVAL
    // ============================================================

    public int removeDuplicates() {

        int removed = 0;

        for (Player player : plugin.getServer().getOnlinePlayers()) {

            if (isOwner(player)) {
                removed += removeExtraCopies(player);
            } else {
                removed += removeAllRhitta(player);
            }
        }

        for (World world : plugin.getServer().getWorlds()) {

            for (Entity entity : world.getEntities()) {

                if (!(entity instanceof Item)) {
                    continue;
                }

                Item dropped = (Item) entity;

                if (!isRhitta(dropped.getItemStack())) {
                    continue;
                }

                removed += dropped.getItemStack().getAmount();

                dropped.remove();
            }
        }

        return removed;
    }

    private int removeExtraCopies(Player player) {

        int removed = 0;
        boolean kept = false;

        PlayerInventory inventory = player.getInventory();

        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {

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

        ItemStack offhand = inventory.getItemInOffHand();

        if (isRhitta(offhand)) {

            if (!kept) {
                kept = true;
            } else {

                removed += offhand.getAmount();

                inventory.setItemInOffHand(null);
            }
        }

        ItemStack[] ender = player.getEnderChest().getContents();

        for (int i = 0; i < ender.length; i++) {

            if (!isRhitta(ender[i])) {
                continue;
            }

            removed += ender[i].getAmount();
            ender[i] = null;
        }

        player.getEnderChest().setContents(ender);

        if (!kept) {
            giveRhitta(player);
        }

        return removed;
    }

    private int removeAllRhitta(Player player) {

        int removed = 0;

        PlayerInventory inventory = player.getInventory();

        ItemStack[] contents = inventory.getContents();

        for (int i = 0; i < contents.length; i++) {

            if (!isRhitta(contents[i])) {
                continue;
            }

            removed += contents[i].getAmount();
            contents[i] = null;
        }

        inventory.setContents(contents);

        ItemStack offhand = inventory.getItemInOffHand();

        if (isRhitta(offhand)) {

            removed += offhand.getAmount();

            inventory.setItemInOffHand(null);
        }

        ItemStack[] ender = player.getEnderChest().getContents();

        for (int i = 0; i < ender.length; i++) {

            if (!isRhitta(ender[i])) {
                continue;
            }

            removed += ender[i].getAmount();
            ender[i] = null;
        }

        player.getEnderChest().setContents(ender);

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

    public void addDefense(Player player, int amount) {

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
    // SURVIVAL BUFFS
    // ============================================================

    public boolean isBuffsEnabled() {
        return buffsEnabled;
    }

    public void setBuffsEnabled(boolean enabled) {
        buffsEnabled = enabled;
    }

    // ============================================================
    // HOTBAR SKILLS
    // ============================================================

    public boolean areSkillsEnabled() {
        return skillsEnabled;
    }

    // Compatibility with RhittaCommand
    public boolean isSkillsEnabled() {
        return skillsEnabled;
    }

    public void setSkillsEnabled(boolean enabled) {
        skillsEnabled = enabled;
    }

    /*
     * Slot 1 = Fireball
     * Slot 2 = King's Aura
     * Slot 3 = Absolute Dominance
     * Slot 4 = Unbreakable Ego
     * Slot 5 = Punishment of the Proud
     * Slot 6 = Pride's Judgment
     * Slot 7 = King's Authority Ultimate
     */

    public String getAbilityForSlot(int slot) {

        switch (slot) {

            case 0:
                return "fireball";

            case 1:
                return "king_aura";

            case 2:
                return "absolute_dominance";

            case 3:
                return "unbreakable_ego";

            case 4:
                return "punishment_proud";

            case 5:
                return "prides_judgment";

            case 6:
                return "kings_authority";

            default:
                return null;
        }
    }

    public int getSlotForAbility(String ability) {

        if (ability == null) {
            return -1;
        }

        switch (ability.toLowerCase()) {

            case "fireball":
                return 0;

            case "king_aura":
                return 1;

            case "absolute_dominance":
                return 2;

            case "unbreakable_ego":
                return 3;

            case "punishment_proud":
                return 4;

            case "prides_judgment":
                return 5;

            case "kings_authority":
                return 6;

            default:
                return -1;
        }
    }

    // ============================================================
    // ACTIVE ABILITY
    // ============================================================

    public void setAbilityActive(Player player, String ability) {

        if (ability == null) {
            return;
        }

        activeAbilities.put(
                player.getUniqueId(),
                ability.toLowerCase()
        );
    }

    public String getActiveAbility(Player player) {

        return activeAbilities.get(
                player.getUniqueId()
        );
    }

    public void clearAbility(Player player) {

        activeAbilities.remove(
                player.getUniqueId()
        );
    }

    // ============================================================
    // COOLDOWNS
    // ============================================================

    private String cooldownKey(
            Player player,
            String ability) {

        return player.getUniqueId() +
                ":" +
                ability.toUpperCase();
    }

    public boolean isOnCooldown(
            Player player,
            String ability) {

        Long end = cooldowns.get(
                cooldownKey(player, ability)
        );

        return end != null &&
                System.currentTimeMillis() < end;
    }

    public void setCooldown(
            Player player,
            String ability,
            long milliseconds) {

        cooldowns.put(
                cooldownKey(player, ability),
                System.currentTimeMillis() + milliseconds
        );
    }

    public long getCooldownRemaining(
            Player player,
            String ability) {

        Long end = cooldowns.get(
                cooldownKey(player, ability)
        );

        if (end == null) {
            return 0;
        }

        return Math.max(
                0,
                end - System.currentTimeMillis()
        );
    }
}