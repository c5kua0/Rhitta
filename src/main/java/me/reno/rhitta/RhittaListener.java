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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

    /*
     * Fireball cooldown:
     * 3 seconds
     */
    private static final long FIREBALL_COOLDOWN = 3000L;

    /*
     * Low-health trigger:
     *
     * 10 HP = 5 hearts
     */
    private static final double LOW_HEALTH = 10.0;

    /*
     * Low-health ability lasts:
     * 60 seconds
     */
    private static final int BUFF_DURATION = 20 * 60;

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    private long lastFireball = 0L;

    public RhittaListener(
            RhittaPlugin plugin,
            RhittaManager manager) {

        this.plugin = plugin;
        this.manager = manager;
    }

    // =====================================================
    // JOIN
    // =====================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        /*
         * FORCE EXACTLY ONE RHITTA.
         */
        manager.forceOneRhitta(player);

        makeRhittaUnbreakable(player);
    }

    // =====================================================
    // RESPAWN
    //
    // NO RESURRECTION HERE.
    //
    // We simply restore exactly one Rhitta after
    // a normal death.
    // =====================================================

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            /*
                             * Normal respawn.
                             *
                             * Rhitta is restored to exactly ONE.
                             */
                            manager.forceOneRhitta(player);

                            makeRhittaUnbreakable(player);

                        },
                        2L
                );
    }

    // =====================================================
    // LOW HEALTH ABILITY
    //
    // No death.
    // No resurrection.
    //
    // When damage would bring Rhitta's owner to
    // 5 hearts or lower:
    //
    // "Who decided that?"
    // Full HP
    // Strength X
    // Resistance X
    // 60 seconds
    //
    // ONE TIME ONLY.
    // =====================================================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onLowHealth(
            EntityDamageEvent event) {

        if (!(event.getEntity()
                instanceof Player player)) {

            return;
        }

        if (!isOwner(player)) {
            return;
        }

        /*
         * Rhitta must be present.
         */
        if (!manager.hasRhitta(player)) {
            return;
        }

        /*
         * Don't activate after the ability
         * has already been used.
         */
        if (manager.hasUsedResurrection(player)) {
            return;
        }

        /*
         * Calculate health AFTER damage.
         */
        double damage = event.getFinalDamage();

        double remainingHealth =
                player.getHealth() - damage;

        /*
         * If the damage would bring the player
         * to 5 hearts or lower, activate.
         */
        if (remainingHealth > LOW_HEALTH) {
            return;
        }

        /*
         * IMPORTANT:
         *
         * Cancel the damage.
         *
         * This means the player does NOT die.
         */
        event.setCancelled(true);

        /*
         * Mark the ability as permanently used.
         */
        manager.markResurrectionUsed(player);

        /*
         * Apply the effect on the next tick.
         * This prevents conflicts with the damage event.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            /*
                             * FULL HEALTH
                             */
                            player.setHealth(
                                    getMaxHealth(player)
                            );

                            /*
                             * MESSAGE
                             */
                            player.sendMessage(
                                    "§6§lWho decided that?"
                            );

                            /*
                             * STRENGTH X
                             *
                             * Amplifier 9 = Strength X
                             */
                            player.addPotionEffect(
                                    new PotionEffect(
                                            PotionEffectType.STRENGTH,
                                            BUFF_DURATION,
                                            9,
                                            false,
                                            false,
                                            true
                                    )
                            );

                            /*
                             * RESISTANCE X
                             *
                             * Amplifier 9 = Resistance X
                             */
                            player.addPotionEffect(
                                    new PotionEffect(
                                            PotionEffectType.RESISTANCE,
                                            BUFF_DURATION,
                                            9,
                                            false,
                                            false,
                                            true
                                    )
                            );

                        }
                );
    }

    // =====================================================
    // MELEE HIT
    // =====================================================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onHit(
            EntityDamageByEntityEvent event) {

        if (!(event.getDamager()
                instanceof Player player)) {

            return;
        }

        if (!isOwner(player)) {
            return;
        }

        ItemStack weapon =
                player.getInventory()
                        .getItemInMainHand();

        if (!manager.isRhitta(weapon)) {
            return;
        }

        makeUnbreakable(weapon);

        /*
         * LIFE STEAL
         *
         * 20% of damage dealt.
         * Maximum 4 HP.
         */
        double heal =
                Math.min(
                        event.getFinalDamage() * 0.20,
                        4.0
                );

        player.setHealth(
                Math.min(
                        player.getHealth() + heal,
                        getMaxHealth(player)
                )
        );

        /*
         * DEFENSE +1
         */
        manager.addDefense(player);
    }

    // =====================================================
    // RIGHT CLICK FIREBALL
    // =====================================================

    @EventHandler
    public void onInteract(
            PlayerInteractEvent event) {

        if (event.getHand()
                != EquipmentSlot.HAND) {

            return;
        }

        if (event.getAction()
                != Action.RIGHT_CLICK_AIR
                &&
                event.getAction()
                != Action.RIGHT_CLICK_BLOCK) {

            return;
        }

        Player player =
                event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        long now =
                System.currentTimeMillis();

        if (now - lastFireball
                < FIREBALL_COOLDOWN) {

            return;
        }

        lastFireball = now;

        launchFireball(player);

        event.setCancelled(true);
    }

    // =====================================================
    // LAUNCH FIREBALL
    // =====================================================

    private void launchFireball(
            Player player) {

        Vector direction =
                player.getEyeLocation()
                        .getDirection()
                        .normalize();

        Fireball fireball =
                player.getWorld().spawn(
                        player.getEyeLocation()
                                .add(
                                        direction
                                                .clone()
                                                .multiply(1.5)
                                ),
                        Fireball.class
                );

        fireball.setShooter(player);

        fireball.setDirection(direction);

        /*
         * BLOCK DESTRUCTION:
         *
         * 2.0F explosion power.
         */
        fireball.setYield(2.0F);

        /*
         * NO FIRE.
         */
        fireball.setIsIncendiary(false);
    }

    // =====================================================
    // FIREBALL HIT
    // =====================================================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onFireballHit(
            ProjectileHitEvent event) {

        if (!(event.getEntity()
                instanceof Fireball fireball)) {

            return;
        }

        if (!(fireball.getShooter()
                instanceof Player shooter)) {

            return;
        }

        if (!isOwner(shooter)) {
            return;
        }

        Entity hit =
                event.getHitEntity();

        /*
         * Block hit:
         *
         * DO NOT REMOVE THE FIREBALL HERE.
         *
         * Vanilla explosion handles block
         * destruction because yield = 2.0F.
         */
        if (!(hit instanceof LivingEntity target)) {
            return;
        }

        /*
         * DIRECT DAMAGE
         *
         * Base = 10
         *
         * 30% bonus penetration damage
         * = 13 raw damage.
         */
        double damage = 10.0 * 1.30;

        /*
         * Damage mobs AND players.
         */
        target.damage(
                damage,
                shooter
        );
    }

    // =====================================================
    // PREVENT VANILLA FIREBALL DAMAGE
    //
    // Our ProjectileHitEvent handles damage.
    // =====================================================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onFireballDamage(
            EntityDamageByEntityEvent event) {

        if (!(event.getDamager()
                instanceof Fireball fireball)) {

            return;
        }

        if (!(fireball.getShooter()
                instanceof Player shooter)) {

            return;
        }

        if (!isOwner(shooter)) {
            return;
        }

        /*
         * Cancel vanilla damage.
         *
         * ProjectileHitEvent applies our
         * 13 damage instead.
         */
        event.setCancelled(true);
    }

    // =====================================================
    // PICKUP
    // =====================================================

    @EventHandler
    public void onPickup(
            EntityPickupItemEvent event) {

        if (!(event.getEntity()
                instanceof Player player)) {

            return;
        }

        ItemStack item =
                event.getItem()
                        .getItemStack();

        if (!manager.isRhitta(item)) {
            return;
        }

        /*
         * ONLY OWNER.
         */
        if (!isOwner(player)) {

            event.setCancelled(true);

            player.setHealth(0.0);

            return;
        }

        /*
         * Let pickup happen first.
         * Then force exactly ONE.
         */
        plugin.getServer()
                .getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            manager.forceOneRhitta(
                                    player
                            );

                            makeRhittaUnbreakable(
                                    player
                            );

                        }
                );
    }

    // =====================================================
    // DROP
    // =====================================================

    @EventHandler
    public void onDrop(
            PlayerDropItemEvent event) {

        ItemStack item =
                event.getItemDrop()
                        .getItemStack();

        if (!manager.isRhitta(item)) {
            return;
        }

        /*
         * Rhitta can NEVER be dropped.
         */
        event.setCancelled(true);

        event.getItemDrop().remove();

        /*
         * Make sure exactly one remains.
         */
        manager.forceOneRhitta(
                event.getPlayer()
        );
    }

    // =====================================================
    // INVENTORY CLICK
    // =====================================================

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        ItemStack current =
                event.getCurrentItem();

        ItemStack cursor =
                event.getCursor();

        if (!manager.isRhitta(current)
                && !manager.isRhitta(cursor)) {

            return;
        }

        /*
         * Prevent moving Rhitta.
         */
        event.setCancelled(true);

        /*
         * Force exactly ONE.
         */
        manager.forceOneRhitta(player);
    }

    // =====================================================
    // INVENTORY DRAG
    // =====================================================

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        if (!manager.isRhitta(
                event.getOldCursor())) {

            return;
        }

        /*
         * Prevent Rhitta from being dragged.
         */
        event.setCancelled(true);

        manager.forceOneRhitta(player);
    }

    // =====================================================
    // DEATH
    //
    // NO RESURRECTION.
    //
    // Rhitta simply never becomes a death drop.
    // =====================================================

    @EventHandler(
            priority = EventPriority.HIGHEST
    )
    public void onDeath(
            PlayerDeathEvent event) {

        Player player =
                event.getEntity();

        if (!isOwner(player)) {
            return;
        }

        /*
         * Remove Rhitta from death drops.
         */
        event.getDrops()
                .removeIf(
                        manager::isRhitta
                );

        /*
         * NO RESURRECTION.
         *
         * Player dies normally.
         *
         * onRespawn() restores exactly ONE Rhitta.
         */
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private boolean isOwner(
            Player player) {

        return player != null
                && player.getName()
                .equalsIgnoreCase(OWNER);
    }

    private void makeRhittaUnbreakable(
            Player player) {

        for (ItemStack item :
                player.getInventory()
                        .getContents()) {

            if (manager.isRhitta(item)) {
                makeUnbreakable(item);
            }
        }

        ItemStack offhand =
                player.getInventory()
                        .getItemInOffHand();

        if (manager.isRhitta(offhand)) {
            makeUnbreakable(offhand);
        }
    }

    private void makeUnbreakable(
            ItemStack item) {

        if (item == null ||
                !manager.isRhitta(item)) {

            return;
        }

        ItemMeta meta =
                item.getItemMeta();

        if (meta == null) {
            return;
        }

        meta.setUnbreakable(true);

        item.setItemMeta(meta);
    }

    private double getMaxHealth(
            Player player) {

        AttributeInstance attribute =
                player.getAttribute(
                        Attribute.MAX_HEALTH
                );

        return attribute != null
                ? attribute.getValue()
                : 20.0;
    }
}