package me.reno.rhitta;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class RhittaListener implements Listener {

    private static final String OWNER = "_ToshiroCyMc";

    private static final long FIREBALL_COOLDOWN = 3000L;

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    private long lastFireballTime = 0L;

    public RhittaListener(
            RhittaPlugin plugin,
            RhittaManager manager
    ) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // =========================
    // JOIN
    // =========================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        manager.giveRhitta(player);

        makeRhittaUnbreakable(player);

        applySunBuff(player);

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> manager.removeDuplicateRhittas(player),
                1L
        );
    }

    // =========================
    // RESPAWN
    // =========================

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {

                    if (!player.isOnline()) {
                        return;
                    }

                    manager.giveRhitta(player);

                    makeRhittaUnbreakable(player);

                    applySunBuff(player);

                },
                2L
        );
    }

    // =========================
    // HIT
    // =========================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!isOwner(player)) {
            return;
        }

        ItemStack weapon =
                player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(weapon)) {
            return;
        }

        makeUnbreakable(weapon);

        // 20% LIFE STEAL
        double heal =
                Math.min(
                        event.getFinalDamage() * 0.20,
                        4.0
                );

        double newHealth =
                Math.min(
                        player.getHealth() + heal,
                        getMaxHealth(player)
                );

        player.setHealth(newHealth);

        // DEFENSE INCREASES EVERY HIT
        manager.addDefense(player);
    }

    // =========================
    // FIREBALL
    // =========================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        ItemStack item =
                player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        long now = System.currentTimeMillis();

        if (now - lastFireballTime < FIREBALL_COOLDOWN) {
            return;
        }

        lastFireballTime = now;

        launchFireball(player);

        event.setCancelled(true);
    }

    private void launchFireball(Player player) {

        Vector direction =
                player.getEyeLocation()
                        .getDirection()
                        .normalize();

        Fireball fireball =
                player.getWorld().spawn(
                        player.getEyeLocation()
                                .add(direction.clone().multiply(1.5)),
                        Fireball.class
                );

        fireball.setShooter(player);

        fireball.setDirection(direction);

        // NO BLOCK DESTRUCTION
        fireball.setYield(0.0F);

        // NO FIRE
        fireball.setIsIncendiary(false);
    }

    // =========================
    // FIREBALL DAMAGE
    // PLAYERS + MOBS
    // =========================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFireballDamage(
            EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Fireball fireball)) {
            return;
        }

        if (!(fireball.getShooter() instanceof Player player)) {
            return;
        }

        if (!isOwner(player)) {
            return;
        }

        Entity target = event.getEntity();

        if (!(target instanceof LivingEntity)) {
            event.setCancelled(true);
            return;
        }

        // 30% ARMOR PENETRATION APPROXIMATION
        double damage = event.getDamage();

        double penetration = damage * 0.30;

        event.setDamage(damage + penetration);
    }

    // =========================
    // PICKUP
    // =========================

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack item =
                event.getItem().getItemStack();

        if (!manager.isRhitta(item)) {
            return;
        }

        if (!isOwner(player)) {

            event.setCancelled(true);

            player.setHealth(0.0);

            return;
        }

        manager.removeDuplicateRhittas(player);
    }

    // =========================
    // DROP
    // =========================

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {

        ItemStack item =
                event.getItemDrop().getItemStack();

        if (!manager.isRhitta(item)) {
            return;
        }

        event.setCancelled(true);

        event.getItemDrop().remove();
    }

    // =========================
    // INVENTORY CLICK
    // =========================

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        boolean currentRhitta =
                manager.isRhitta(current);

        boolean cursorRhitta =
                manager.isRhitta(cursor);

        if (!currentRhitta && !cursorRhitta) {
            return;
        }

        // Block storage
        if (event.getView()
                .getTopInventory()
                .getType() != InventoryType.CRAFTING
                && event.getView()
                .getTopInventory()
                .getType() != InventoryType.PLAYER) {

            event.setCancelled(true);

            manager.removeDuplicateRhittas(player);

            return;
        }

        // Prevent manipulation/duplication.
        event.setCancelled(true);

        manager.removeDuplicateRhittas(player);
    }

    // =========================
    // INVENTORY DRAG
    // =========================

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event) {

        if (!manager.isRhitta(event.getOldCursor())) {
            return;
        }

        event.setCancelled(true);

        if (event.getWhoClicked() instanceof Player player) {
            manager.removeDuplicateRhittas(player);
        }
    }

    // =========================
    // DEATH
    // =========================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (!isOwner(player)) {
            return;
        }

        // NEVER DROP RHITTA
        event.getDrops().removeIf(
                manager::isRhitta
        );

        manager.removeDuplicateRhittas(player);

        /*
         * DEATH #1
         * Resurrection + buff.
         */
        if (!manager.hasUsedResurrection(player)) {

            manager.markResurrectionUsed(player);

            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> resurrect(player),
                    2L
            );
        }

        /*
         * DEATH #2+
         * Normal death.
         *
         * Rhitta is restored by onRespawn().
         */
    }

    // =========================
    // RESURRECTION
    // =========================

    private void resurrect(Player player) {

        if (!player.isOnline()) {
            return;
        }

        player.spigot().respawn();

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {

                    if (!player.isOnline()) {
                        return;
                    }

                    manager.giveRhitta(player);

                    makeRhittaUnbreakable(player);

                    // STRENGTH VII - 60 SECONDS
                    player.addPotionEffect(
                            new PotionEffect(
                                    PotionEffectType.STRENGTH,
                                    20 * 60,
                                    6,
                                    false,
                                    false,
                                    true
                            )
                    );

                    AttributeInstance maxHealth =
                            player.getAttribute(
                                    Attribute.MAX_HEALTH
                            );

                    if (maxHealth != null) {
                        player.setHealth(
                                maxHealth.getValue()
                        );
                    }

                },
                2L
        );
    }

    // =========================
    // SUNRISE / DAYTIME
    // =========================

    private void applySunBuff(Player player) {

        if (!isOwner(player)) {
            return;
        }

        long time =
                player.getWorld().getTime();

        /*
         * 0    = Sunrise
         * 6000 = Noon
         * 12000 = Sunset
         */

        if (time >= 0 && time < 12000) {

            // STRENGTH V
            player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.STRENGTH,
                            20 * 30,
                            4,
                            false,
                            false,
                            true
                    )
            );

            // RESISTANCE III
            player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.RESISTANCE,
                            20 * 30,
                            2,
                            false,
                            false,
                            true
                    )
            );
        }
    }

    // =========================
    // HELPERS
    // =========================

    private boolean isOwner(Player player) {

        return player != null
                && player.getName()
                .equalsIgnoreCase(OWNER);
    }

    private void makeRhittaUnbreakable(Player player) {

        for (ItemStack item :
                player.getInventory().getContents()) {

            if (manager.isRhitta(item)) {
                makeUnbreakable(item);
            }
        }

        ItemStack offhand =
                player.getInventory().getItemInOffHand();

        if (manager.isRhitta(offhand)) {
            makeUnbreakable(offhand);
        }
    }

    private void makeUnbreakable(ItemStack item) {

        if (item == null
                || !manager.isRhitta(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.setUnbreakable(true);

        item.setItemMeta(meta);
    }

    private double getMaxHealth(Player player) {

        AttributeInstance attribute =
                player.getAttribute(
                        Attribute.MAX_HEALTH
                );

        return attribute != null
                ? attribute.getValue()
                : 20.0;
    }
}