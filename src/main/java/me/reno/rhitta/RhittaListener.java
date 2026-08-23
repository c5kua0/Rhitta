package me.reno.rhitta;

import org.bukkit.ChatColor;
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
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    private static final long FIREBALL_COOLDOWN = 3000L;
    private long lastFireball = 0L;

    public RhittaListener(
            RhittaPlugin plugin,
            RhittaManager manager) {

        this.plugin = plugin;
        this.manager = manager;
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
                        () -> manager.forceOneRhitta(player),
                        1L
                );
    }

    // ============================================================
    // LIFE STEAL + DEFENSE
    // ============================================================

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {

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

        // Life steal
        double lifeSteal =
                plugin.getConfig()
                        .getDouble(
                                "weapon.life-steal",
                                4.0
                        );

        double newHealth =
                Math.min(
                        player.getHealth()
                                + lifeSteal,
                        player.getMaxHealth()
                );

        player.setHealth(newHealth);

        // Defense growth
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
    // FIREBALL
    // ============================================================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

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
         * Fireball ONLY works when
         *
         * /rhitta 0
         *
         * is active.
         */
        if (!manager.isFireballMode(player)) {
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

        var spawnLocation =
                eye.clone()
                        .add(
                                direction.clone()
                                        .multiply(1.0)
                        );

        Fireball fireball =
                player.getWorld()
                        .spawn(
                                spawnLocation,
                                Fireball.class
                        );

        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setYield(0F);
        fireball.setIsIncendiary(false);

        event.setCancelled(true);
    }

    // ============================================================
    // FIREBALL HIT
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
                (Player) fireball.getShooter()
        );
    }

    // ============================================================
    // PICKUP
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

        ItemStack item