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

    // ============================================================
    // OWNER
    // ============================================================

    public static final String OWNER_NAME = "_ToshiroCyMc";

    private final RhittaPlugin plugin;

    // ============================================================
    // DATA
    // ============================================================

    private final Map<UUID, Integer> defense = new HashMap<>();
    private final Map<UUID, String> activeAbilities = new HashMap<>();
    private final Map<String, Long> cooldowns = new HashMap<>();

    /*
     * Players who recently respawned.
     *
     * This is used to prevent AngelChest/death-chest restoration
     * from leaving the owner with two Rhittas.
     */
    private final Map<UUID, Long> respawnProtection =
            new HashMap<>();

    // ============================================================
    // KEYS
    // ============================================================

    private final NamespacedKey rhittaKey;
    private final NamespacedKey rhittaIdKey;

    private long rhittaCounter = 0L;

    // ============================================================
    // SETTINGS
    // ============================================================

    private boolean buffsEnabled;
    private boolean skillsEnabled = true;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public RhittaManager(RhittaPlugin plugin) {

        this.plugin = plugin;

        rhittaKey = new NamespacedKey(
                plugin,
                "rhitta"
        );

        rhittaIdKey = new NamespacedKey(
                plugin,
                "rhitta_id"
        );

        buffsEnabled = plugin.getConfig().getBoolean(
                "buffs.enabled-by-default",
                true
        );
    }

    // ============================================================
    // OWNER
    // ============================================================

    public boolean isOwner(Player player) {

        if (player == null) {
            return false;
        }

        return player.getName().equalsIgnoreCase(
                OWNER_NAME
        );
    }

    public boolean isAllowedOwner(Player player) {
        return isOwner(player);
    }

    public String getOwnerName() {
        return OWNER_NAME;
    }

    // ============================================================
    // CREATE RHITTA
    // ============================================================

    public ItemStack createRhitta() {

        ItemStack item = new ItemStack(
                Material.NETHERITE_SWORD
        );

        ItemMeta meta = item.getItemMeta();

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

        // --------------------------------------------------------
        // UNIQUE RHITTA ID
        // --------------------------------------------------------

        rhittaCounter++;

        String id =
                "RHITTA-" +
                rhittaCounter +
                "-" +
                UUID.randomUUID();

        // --------------------------------------------------------
        // RHITTA FLAG
        // --------------------------------------------------------

        meta.getPersistentDataContainer().set(
                rhittaKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        // --------------------------------------------------------
        // RHITTA ID
        // --------------------------------------------------------

        meta.getPersistentDataContainer().set(
                rhittaIdKey,
                PersistentDataType.STRING,
                id
        );

        item.setItemMeta(meta);

        return item;
    }

    // ============================================================
    // CHECK RHITTA
    // ============================================================

    public boolean isRhitta(ItemStack item) {

        if (item == null) {
            return false;
        }

        if (item.getType().isAir()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte flag =
                meta.getPersistentDataContainer().get(
                        rhittaKey,
                        PersistentDataType.BYTE
                );

        return flag != null &&
                flag == (byte) 1;
    }

    // ============================================================
    // RHITTA ID
    // ============================================================

    public String getRhittaId(ItemStack item) {

        if (!isRhitta(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        return meta.getPersistentDataContainer().get(
                rhittaIdKey,
                PersistentDataType.STRING
        );
    }

    // ============================================================
    // INVENTORY CHECK
    // ============================================================

    public boolean hasRhitta(Player player) {

        if (player == null) {
            return false;
        }

        PlayerInventory inventory =
                player.getInventory();

        for (ItemStack item :
                inventory.getContents()) {

            if (isRhitta(item)) {
                return true;
            }
        }

        return isRhitta(
                inventory.getItemInOffHand()
        );
    }

    // ============================================================
    // COUNT RHITTA
    // ============================================================

    public int countRhitta(Player player) {

        if (player == null) {
            return 0;
        }

        int count = 0;

        PlayerInventory inventory =
                player.getInventory();

        // --------------------------------------------------------
        // INVENTORY
        // --------------------------------------------------------

        for (ItemStack item :
                inventory.getContents()) {

            if (isRhitta(item)) {
                count += item.getAmount();
            }
        }

        // --------------------------------------------------------
        // OFFHAND
        // --------------------------------------------------------

        ItemStack offhand =
                inventory.getItemInOffHand();

        if (isRhitta(offhand)) {
            count += offhand.getAmount();
        }

        // --------------------------------------------------------
        // ENDER CHEST
        // --------------------------------------------------------

        for (ItemStack item :
                player.getEnderChest().getContents()) {

            if (isRhitta(item)) {
                count += item.getAmount();
            }
        }

        return count;
    }

    // ============================================================
    // GIVE RHITTA
    // ============================================================

    public void giveRhitta(Player player) {

        if (player == null) {
            return;
        }

        if (!isOwner(player)) {
            return;
        }

        /*
         * NEVER create a new Rhitta if one already exists.
         */
        if (hasRhitta(player)) {
            return;
        }

        ItemStack rhitta =
                createRhitta();

        PlayerInventory inventory =
                player.getInventory();

        // --------------------------------------------------------
        // MAIN HAND
        // --------------------------------------------------------

        if (inventory.getItemInMainHand()
                .getType()
                .isAir()) {

            inventory.setItemInMainHand(
                    rhitta
            );

            return;
        }

        // --------------------------------------------------------
        // OFFHAND
        // --------------------------------------------------------

        if (inventory.getItemInOffHand()
                .getType()
                .isAir()) {

            inventory.setItemInOffHand(
                    rhitta
            );

            return;
        }

        // --------------------------------------------------------
        // NORMAL INVENTORY
        // --------------------------------------------------------

        Map<Integer, ItemStack> leftover =
                inventory.addItem(rhitta);

        // --------------------------------------------------------
        // INVENTORY FULL
        // --------------------------------------------------------

        if (!leftover.isEmpty()) {

            player.sendMessage(
                    ChatColor.YELLOW +
                    "Your inventory is full. " +
                    "Rhitta will remain protected."
            );

            plugin.getServer()
                    .getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> {

                                if (!player.isOnline()) {
                                    return;
                                }

                                if (!isOwner(player)) {
                                    return;
                                }

                                /*
                                 * Check again before creating.
                                 */
                                if (!hasRhitta(player)) {
                                    giveRhitta(player);
                                }

                            },
                            20L
                    );
        }
    }

    // ============================================================
    // FORCE ONE RHITTA
    // ============================================================

    public void forceOneRhitta(Player player) {

        if (player == null) {
            return;
        }

        if (!isOwner(player)) {
            return;
        }

        /*
         * First clean any existing duplicates.
         */
        if (countRhitta(player) > 1) {

            removeExtraCopies(player);
        }

        /*
         * Only give one if completely missing.
         */
        if (!hasRhitta(player)) {

            giveRhitta(player);
        }
    }

    // ============================================================
    // SCHEDULED RESPAWN DUPLICATE PROTECTION
    // ============================================================

    public void scheduleRespawnCleanup(Player player) {

        if (player == null) {
            return;
        }

        if (!isOwner(player)) {
            return;
        }

        UUID uuid =
                player.getUniqueId();

        respawnProtection.put(
                uuid,
                System.currentTimeMillis()
        );

        /*
         * Immediate cleanup.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            forceOneRhitta(player);
                        }
                );

        /*
         * AngelChest may restore the inventory slightly later.
         *
         * Check several times after respawn.
         */

        scheduleCleanup(
                player,
                5L
        );

        scheduleCleanup(
                player,
                20L
        );

        scheduleCleanup(
                player,
                40L
        );

        scheduleCleanup(
                player,
                80L
        );

        scheduleCleanup(
                player,
                120L
        );

        scheduleCleanup(
                player,
                200L
        );
    }

    private void scheduleCleanup(
            Player player,
            long delay) {

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            if (!isOwner(player)) {
                                return;
                            }

                            forceOneRhitta(player);

                        },
                        delay
                );
    }

    // ============================================================
    // REMOVE DUPLICATES
    // ============================================================

    public int removeDuplicates() {

        int removed = 0;

        // --------------------------------------------------------
        // PLAYERS
        // --------------------------------------------------------

        for (Player player :
                plugin.getServer().getOnlinePlayers()) {

            if (isOwner(player)) {

                removed += removeExtraCopies(
                        player
                );

            } else {

                removed += removeAllRhitta(
                        player
                );
            }
        }

        // --------------------------------------------------------
        // DROPPED ITEMS
        // --------------------------------------------------------

        for (World world :
                plugin.getServer().getWorlds()) {

            for (Entity entity :
                    world.getEntities()) {

                if (!(entity instanceof Item)) {
                    continue;
                }

                Item dropped =
                        (Item) entity;

                ItemStack item =
                        dropped.getItemStack();

                if (!isRhitta(item)) {
                    continue;
                }

                removed += item.getAmount();

                dropped.remove();
            }
        }

        return removed;
    }

    // ============================================================
    // REMOVE EXTRA COPIES
    // ============================================================

    private int removeExtraCopies(Player player) {

        if (player == null) {
            return 0;
        }

        int removed = 0;
        boolean kept = false;

        PlayerInventory inventory =
                player.getInventory();

        // --------------------------------------------------------
        // MAIN INVENTORY
        // --------------------------------------------------------

        ItemStack[] contents =
                inventory.getContents();

        for (int i = 0;
             i < contents.length;
             i++) {

            ItemStack item =
                    contents[i];

            if (!isRhitta(item)) {
                continue;
            }

            /*
             * Keep the first Rhitta.
             */
            if (!kept) {

                /*
                 * If somehow the stack contains more than one
                 * Rhitta, split it down to exactly one.
                 */
                if (item.getAmount() > 1) {

                    removed +=
                            item.getAmount() - 1;

                    item.setAmount(1);
                }

                kept = true;

                continue;
            }

            /*
             * Every additional Rhitta is removed.
             */
            removed += item.getAmount();

            contents[i] = null;
        }

        inventory.setContents(contents);

        // --------------------------------------------------------
        // OFFHAND
        // --------------------------------------------------------

        ItemStack offhand =
                inventory.getItemInOffHand();

        if (isRhitta(offhand)) {

            if (!kept) {

                if (offhand.getAmount() > 1) {

                    removed +=
                            offhand.getAmount() - 1;

                    offhand.setAmount(1);
                }

                kept = true;

            } else {

                removed += offhand.getAmount();

                inventory.setItemInOffHand(
                        null
                );
            }
        }

        // --------------------------------------------------------
        // ENDER CHEST
        // --------------------------------------------------------

        ItemStack[] ender =
                player.getEnderChest()
                        .getContents();

        for (int i = 0;
             i < ender.length;
             i++) {

            ItemStack item =
                    ender[i];

            if (!isRhitta(item)) {
                continue;
            }

            /*
             * We already keep one Rhitta in the player's normal
             * inventory/offhand.
             *
             * Therefore every Rhitta inside the Ender Chest
             * is an extra copy.
             */
            removed += item.getAmount();

            ender[i] = null;
        }

        player.getEnderChest()
                .setContents(ender);

        // --------------------------------------------------------
        // GIVE IF MISSING
        // --------------------------------------------------------

        if (!kept) {
            giveRhitta(player);
        }

        return removed;
    }

    // ============================================================
    // REMOVE ALL RHITTA FROM PLAYER
    // ============================================================

    private int removeAllRhitta(Player player) {

        if (player == null) {
            return 0;
        }

        int removed = 0;

        PlayerInventory inventory =
                player.getInventory();

        // --------------------------------------------------------
        // INVENTORY
        // --------------------------------------------------------

        ItemStack[] contents =
                inventory.getContents();

        for (int i = 0;
             i < contents.length;
             i++) {

            ItemStack item =
                    contents[i];

            if (!isRhitta(item)) {
                continue;
            }

            removed += item.getAmount();

            contents[i] = null;
        }

        inventory.setContents(contents);

        // --------------------------------------------------------
        // OFFHAND
        // --------------------------------------------------------

        ItemStack offhand =
                inventory.getItemInOffHand();

        if (isRhitta(offhand)) {

            removed += offhand.getAmount();

            inventory.setItemInOffHand(
                    null
            );
        }

        // --------------------------------------------------------
        // ENDER CHEST
        // --------------------------------------------------------

        ItemStack[] ender =
                player.getEnderChest()
                        .getContents();

        for (int i = 0;
             i < ender.length;
             i++) {

            ItemStack item =
                    ender[i];

            if (!isRhitta(item)) {
                continue;
            }

            removed += item.getAmount();

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

        if (player == null) {
            return 0;
        }

        return defense.getOrDefault(
                player.getUniqueId(),
                0
        );
    }

    public void addDefense(
            Player player,
            int amount) {

        if (player == null) {
            return;
        }

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
    // SKILLS
    // ============================================================

    public boolean areSkillsEnabled() {
        return skillsEnabled;
    }

    public boolean isSkillsEnabled() {
        return skillsEnabled;
    }

    public void setSkillsEnabled(
            boolean enabled) {

        skillsEnabled = enabled;
    }

    // ============================================================
    // ABILITY SLOTS
    // ============================================================

    /*
     * Slot 1 = Fireball
     * Slot 2 = King's Aura
     * Slot 3 = Unbreakable Ego
     * Slot 4 = Punishment of the Proud
     * Slot 5 = Pride's Judgment
     * Slot 6 = Absolute Dominance
     * Slot 7 = King's Authority
     */

    public String getAbilityForSlot(int slot) {

        switch (slot) {

            case 0:
                return "fireball";

            case 1:
                return "king_aura";

            case 2:
                return "unbreakable_ego";

            case 3:
                return "punishment_proud";

            case 4:
                return "prides_judgment";

            case 5:
                return "absolute_dominance";

            case 6:
                return "kings_authority";

            default:
                return null;
        }
    }

    // ============================================================
    // ABILITY → SLOT
    // ============================================================

    public int getSlotForAbility(
            String ability) {

        if (ability == null) {
            return -1;
        }

        switch (ability.toLowerCase()) {

            case "fireball":
                return 0;

            case "king_aura":
                return 1;

            case "unbreakable_ego":
                return 2;

            case "punishment_proud":
                return 3;

            case "prides_judgment":
                return 4;

            case "absolute_dominance":
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

    public void setAbilityActive(
            Player player,
            String ability) {

        if (player == null) {
            return;
        }

        if (ability == null) {
            return;
        }

        activeAbilities.put(
                player.getUniqueId(),
                ability.toLowerCase()
        );
    }

    public String getActiveAbility(
            Player player) {

        if (player == null) {
            return null;
        }

        return activeAbilities.get(
                player.getUniqueId()
        );
    }

    public void clearAbility(
            Player player) {

        if (player == null) {
            return;
        }

        activeAbilities.remove(
                player.getUniqueId()
        );
    }

    // ============================================================
    // COOLDOWN KEY
    // ============================================================

    private String cooldownKey(
            Player player,
            String ability) {

        return player.getUniqueId()
                + ":"
                + ability.toUpperCase();
    }

    // ============================================================
    // COOLDOWN CHECK
    // ============================================================

    public boolean isOnCooldown(
            Player player,
            String ability) {

        if (player == null) {
            return false;
        }

        if (ability == null) {
            return false;
        }

        Long end =
                cooldowns.get(
                        cooldownKey(
                                player,
                                ability
                        )
                );

        return end != null &&
                System.currentTimeMillis() < end;
    }

    // ============================================================
    // SET COOLDOWN
    // ============================================================

    public void setCooldown(
            Player player,
            String ability,
            long milliseconds) {

        if (player == null) {
            return;
        }

        if (ability == null) {
            return;
        }

        cooldowns.put(
                cooldownKey(
                        player,
                        ability
                ),
                System.currentTimeMillis()
                        + milliseconds
        );
    }

    // ============================================================
    // COOLDOWN REMAINING
    // ============================================================

    public long getCooldownRemaining(
            Player player,
            String ability) {

        if (player == null) {
            return 0;
        }

        if (ability == null) {
            return 0;
        }

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
                end - System.currentTimeMillis()
        );
    }
                } 
