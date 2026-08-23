package me.reno.rhitta;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    private long lastFireball = 0L;

    private static final long FIREBALL_COOLDOWN = 3000L;

    // Exact Rhitta physical attack damage.
    private static final double RHITTA_DAMAGE = 20.0;

    public RhittaListener(
            RhittaPlugin plugin,
            RhittaManager manager) {

        this.plugin = plugin;
        this.manager = manager;

        startBuffTask();
    }

    // ============================================================
    // JOIN
    // ============================================================

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {

        Player player = event.getPlayer();

        if (!manager.isOwner(player)) {
            return;
        }

        manager.forceOneRhitta(player);

        if (manager.isBuffsEnabled()) {
            applyRhittaBuffs(player);
        }
    }

    // ============================================================
    // RESPAWN
    // ============================================================

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            manager.forceOneRhitta(player);

                            if (manager.isBuffsEnabled()) {
                                applyRhittaBuffs(player);
                            }

                        },
                        1L
                );
    }

    // ============================================================
    // RHITTA ATTACK
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttack(
            EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getDamager();

        ItemStack weapon =
                player.getInventory()
                        .getItemInMainHand();

        if (!manager.isRhitta(weapon)) {
            return;
        }

        /*
         * RHITTA PHYSICAL ATTACK
         *
         * Base damage is set to 20.
         *
         * Armor and other Minecraft
         * damage calculations can still
         * reduce the final damage.
         */

        event.setDamage(RHITTA_DAMAGE);

        // ========================================================
        // LIFE STEAL
        // ========================================================

        double lifeSteal =
                plugin.getConfig()
                        .getDouble(
                                "weapon.life-steal",
                                2.0
                        );

        double health =
                Math.min(
                        player.getHealth()
                                + lifeSteal,
                        player.getMaxHealth()
                );

        player.setHealth(health);

        // ========================================================
        // DEFENSE GROWTH
        // ========================================================

        int defense =
                plugin.getConfig()
                        .getInt(
                                "weapon.defense-per-hit",
                                1
                        );

        manager.addDefense(
                player,
                defense
        );
    }

    // ============================================================
    // RHITTA BUFFS
    // ============================================================

    private void startBuffTask() {

        new BukkitRunnable() {

            @Override
            public void run() {

                for (Player player :
                        plugin.getServer()
                                .getOnlinePlayers()) {

                    if (!manager.isOwner(player)) {
                        continue;
                    }

                    if (!manager.isBuffsEnabled()) {
                        removeRhittaBuffs(player);
                        continue;
                    }

                    if (!manager.hasRhitta(player)) {
                        removeRhittaBuffs(player);
                        continue;
                    }

                    applyRhittaBuffs(player);
                }
            }

        }.runTaskTimer(
                plugin,
                0L,
                40L
        );
    }

    private void applyRhittaBuffs(Player player) {

        /*
         * Strength I
         */
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        80,
                        0,
                        false,
                        false,
                        false
                )
        );

        /*
         * Resistance II
         *
         * Amplifier 1 = Level II
         */
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        80,
                        1,
                        false,
                        false,
                        false
                )
        );

        /*
         * Speed I
         */
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SPEED,
                        80,
                        0,
                        false,
                        false,
                        false
                )
        );

        /*
         * Regeneration I
         */
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.REGENERATION,
                        80,
                        0,
                        false,
                        false,
                        false
                )
        );
    }

    private void removeRhittaBuffs(Player player) {

        player.removePotionEffect(
                PotionEffectType.STRENGTH
        );

        player.removePotionEffect(
                PotionEffectType.RESISTANCE
        );

        player.removePotionEffect(
                PotionEffectType.SPEED
        );

        player.removePotionEffect(
                PotionEffectType.REGENERATION
        );
    }

    // ============================================================
    // FIREBALL
    // ============================================================

    @EventHandler
    public void onInteract(
            PlayerInteractEvent event) {

        if (event.getHand()
                != EquipmentSlot.HAND) {

            return;
        }

        Action action =
                event.getAction();

        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {

            return;
        }

        Player player =
                event.getPlayer();

        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        /*
         * Fireball only works when:
         *
         * /rhitta 0
         *
         * is active.
         */

        if (!"0".equals(
                manager.getActiveAbility(player))) {

            return;
        }

        long now =
                System.currentTimeMillis();

        if (now - lastFireball
                < FIREBALL_COOLDOWN) {

            return;
        }

        lastFireball = now;

        var eye =
                player.getEyeLocation();

        var direction =
                eye.getDirection()
                        .normalize();

        var location =
                eye.clone()
                        .add(
                                direction.clone()
                                        .multiply(1.0)
                        );

        Fireball fireball =
                player.getWorld()
                        .spawn(
                                location,
                                Fireball.class
                        );

        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setYield(0F);
        fireball.setIsIncendiary(false);

        event.setCancelled(true);
    }

    // ============================================================
    // FIREBALL DAMAGE
    // ============================================================

    @EventHandler
    public void onFireballHit(
            ProjectileHitEvent event) {

        if (!(event.getEntity()
                instanceof Fireball)) {

            return;
        }

        Fireball fireball =
                (Fireball) event.getEntity();

        if (!(fireball.getShooter()
                instanceof Player)) {

            return;
        }

        if (!(event.getHitEntity()
                instanceof LivingEntity)) {

            return;
        }

        LivingEntity target =
                (LivingEntity)
                        event.getHitEntity();

        double damage =
                plugin.getConfig()
                        .getDouble(
                                "weapon.fireball-damage",
                                4.0
                        );

        target.damage(
                damage,
                (Player)
                        fireball.getShooter()
        );
    }

    // ============================================================
    // PICKUP PROTECTION
    // ============================================================

    @EventHandler
    public void onPickup(
            EntityPickupItemEvent event) {

        if (!(event.getEntity()
                instanceof Player)) {

            return;
        }

        Player player =
                (Player) event.getEntity();

        ItemStack item =
                event.getItem()
                        .getItemStack();

        if (!manager.isRhitta(item)) {
            return;
        }

        if (!manager.isOwner(player)) {

            event.setCancelled(true);

            return;
        }

        if (manager.hasRhitta(player)) {

            event.setCancelled(true);

            event.getItem().remove();
        }
    }

    // ============================================================
    // DROP PROTECTION
    // ============================================================

    @EventHandler
    public void onDrop(
            PlayerDropItemEvent event) {

        ItemStack item =
                event.getItemDrop()
                        .getItemStack();

        if (!manager.isRhitta(item)) {
            return;
        }

        event.setCancelled(true);
    }

    // ============================================================
    // INVENTORY PROTECTION
    // ============================================================

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event) {

        ItemStack current =
                event.getCurrentItem();

        ItemStack cursor =
                event.getCursor();

        if (!manager.isRhitta(current)
                && !manager.isRhitta(cursor)) {

            return;
        }

        if (event.getClickedInventory() != null
                && !(event.getClickedInventory()
                instanceof PlayerInventory)) {

            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDrag(
            InventoryDragEvent event) {

        ItemStack cursor =
                event.getOldCursor();

        if (!manager.isRhitta(cursor)) {
            return;
        }

        if (!(event.getInventory()
                instanceof PlayerInventory)) {

            event.setCancelled(true);
        }
    }

    // ============================================================
    // DEATH
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(
            PlayerDeathEvent event) {

        event.getDrops()
                .removeIf(manager::isRhitta);

        Player player = event.getEntity();

        removeRhittaBuffs(player);
    }

    // ============================================================
    // DEFENSE
    // ============================================================

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(
            EntityDamageEvent event) {

        if (!(event.getEntity()
                instanceof Player)) {

            return;
        }

        Player player =
                (Player) event.getEntity();

        if (!manager.hasRhitta(player)) {
            return;
        }

        int defense =
                manager.getDefense(player);

        if (defense <= 0) {
            return;
        }

        /*
         * 1 defense = 1% damage reduction.
         *
         * Maximum reduction = 80%.
         */

        double reduction =
                Math.min(
                        defense * 0.01,
                        0.80
                );

        event.setDamage(
                event.getDamage()
                        * (1.0 - reduction)
        );
    }
}