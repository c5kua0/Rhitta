package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.Location;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    private static final long FIREBALL_COOLDOWN = 3000L;
    private static final long KING_AURA_COOLDOWN = 15000L;

    private static final double PHYSICAL_ATTACK = 20.0;

    private static final double LIFE_STEAL = 4.0;
    private static final int DEFENSE_PER_HIT = 1;

    private static final double KING_AURA_RADIUS = 8.0;
    private static final double KING_AURA_DAMAGE = 4.0;

    private final List<String> abilities = new ArrayList<>();

    public RhittaListener(
            RhittaPlugin plugin,
            RhittaManager manager) {

        this.plugin = plugin;
        this.manager = manager;

        abilities.add("FIREBALL");
        abilities.add("KING_AURA");
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

        new BukkitRunnable() {
            @Override
            public void run() {
                manager.forceOneRhitta(player);
            }
        }.runTaskLater(plugin, 1L);
    }

    // ============================================================
    // NORMAL ATTACK
    // ============================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(EntityDamageByEntityEvent event) {

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
         * Adds 20 damage to the normal
         * Minecraft attack damage.
         */
        event.setDamage(
                event.getDamage()
                        + PHYSICAL_ATTACK
        );

        // ========================================================
        // LIFE STEAL
        // ========================================================

        double health =
                Math.min(
                        player.getHealth() + LIFE_STEAL,
                        player.getMaxHealth()
                );

        player.setHealth(health);

        // ========================================================
        // DEFENSE GROWTH
        // ========================================================

        manager.addDefense(
                player,
                DEFENSE_PER_HIT
        );
    }

    // ============================================================
    // RIGHT CLICK
    // ============================================================

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {

        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR &&
                action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();

        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        event.setCancelled(true);

        /*
         * NORMAL RIGHT CLICK
         *
         * Changes selected ability.
         */
        if (!player.isSneaking()) {

            cycleAbility(player);

            return;
        }

        /*
         * SHIFT + RIGHT CLICK
         *
         * Activates selected ability.
         */

        String ability =
                manager.getActiveAbility(player);

        if (ability == null) {

            manager.setAbilityActive(
                    player,
                    abilities.get(0)
            );

            ability =
                    abilities.get(0);
        }

        activateAbility(
                player,
                ability
        );
    }

    // ============================================================
    // CYCLE ABILITY
    // ============================================================

    private void cycleAbility(Player player) {

        String current =
                manager.getActiveAbility(player);

        int index = -1;

        if (current != null) {
            index =
                    abilities.indexOf(
                            current
                    );
        }

        index++;

        if (index >= abilities.size()) {
            index = 0;
        }

        String next =
                abilities.get(index);

        manager.setAbilityActive(
                player,
                next
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Rhitta Ability Selected: " +
                ChatColor.YELLOW +
                formatAbility(next)
        );
    }

    // ============================================================
    // ACTIVATE ABILITY
    // ============================================================

    private void activateAbility(
            Player player,
            String ability) {

        switch (ability.toUpperCase()) {

            case "FIREBALL":
                activateFireball(player);
                break;

            case "KING_AURA":
                activateKingAura(player);
                break;

            default:
                player.sendMessage(
                        ChatColor.RED +
                        "Unknown Rhitta ability."
                );
                break;
        }
    }

    // ============================================================
    // FIREBALL
    // ============================================================

    private void activateFireball(Player player) {

        if (manager.isOnCooldown(
                player,
                "FIREBALL")) {

            sendCooldown(
                    player,
                    "FIREBALL"
            );

            return;
        }

        manager.setCooldown(
                player,
                "FIREBALL",
                FIREBALL_COOLDOWN
        );

        Location eye =
                player.getEyeLocation();

        var direction =
                eye.getDirection()
                        .normalize();

        Location spawn =
                eye.clone()
                        .add(
                                direction
                                        .clone()
                                        .multiply(1.0)
                        );

        Fireball fireball =
                player.getWorld()
                        .spawn(
                                spawn,
                                Fireball.class
                        );

        fireball.setShooter(player);
        fireball.setDirection(direction);

        fireball.setYield(0F);
        fireball.setIsIncendiary(false);

        player.sendMessage(
                ChatColor.GOLD +
                "☀ Rhitta Fireball!"
        );
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

        Player shooter =
                (Player) fireball.getShooter();

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
                shooter
        );
    }

    // ============================================================
    // KING AURA
    // ============================================================

    private void activateKingAura(Player player) {

        if (manager.isOnCooldown(
                player,
                "KING_AURA")) {

            sendCooldown(
                    player,
                    "KING_AURA"
            );

            return;
        }

        manager.setCooldown(
                player,
                "KING_AURA",
                KING_AURA_COOLDOWN
        );

        int affected = 0;

        for (LivingEntity entity :
                player.getWorld()
                        .getLivingEntities()) {

            if (entity.equals(player)) {
                continue;
            }

            if (entity.getLocation()
                    .distance(player.getLocation())
                    > KING_AURA_RADIUS) {
                continue;
            }

            if (entity instanceof Player) {

                /*
                 * Don't attack other players.
                 * This keeps the aura safer
                 * for multiplayer.
                 */
                continue;
            }

            entity.damage(
                    KING_AURA_DAMAGE,
                    player
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * 5,
                            0,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20 * 3,
                            1,
                            false,
                            true,
                            true
                    )
            );

            affected++;
        }

        player.sendMessage(
                ChatColor.GOLD +
                "♛ KING AURA unleashed! " +
                ChatColor.YELLOW +
                affected +
                " enemies affected."
        );
    }

    // ============================================================
    // COOLDOWN MESSAGE
    // ============================================================

    private void sendCooldown(
            Player player,
            String ability) {

        long remaining =
                manager.getCooldownRemaining(
                        player,
                        ability
                );

        double seconds =
                remaining / 1000.0;

        player.sendMessage(
                ChatColor.RED +
                formatAbility(ability) +
                " is on cooldown: " +
                String.format(
                        "%.1f",
                        seconds
                ) +
                "s"
        );
    }

    private String formatAbility(
            String ability) {

        if (ability == null) {
            return "None";
        }

        switch (ability.toUpperCase()) {

            case "KING_AURA":
                return "King Aura";

            case "FIREBALL":
                return "Fireball";

            default:
                return ability;
        }
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

        if (manager.isRhitta(item)) {
            event.setCancelled(true);
        }
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
    }

    // ============================================================
    // DEFENSE
    // ============================================================

    @EventHandler(priority = EventPriority.HIGH)
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
         * Maximum = 80%.
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