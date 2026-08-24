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
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    // ============================================================
    // BALANCED MULTIPLAYER VALUES
    // ============================================================

    private static final double PHYSICAL_ATTACK = 20.0;
    private static final double LIFE_STEAL = 4.0;
    private static final int DEFENSE_PER_HIT = 1;

    private static final long FIREBALL_COOLDOWN = 3000L;
    private static final long AD_COOLDOWN = 20000L;
    private static final long KA_COOLDOWN = 15000L;
    private static final long UE_COOLDOWN = 30000L;
    private static final long PP_COOLDOWN = 12000L;
    private static final long PJ_COOLDOWN = 10000L;
    private static final long KAU_COOLDOWN = 60000L;

    private static final double FIREBALL_DAMAGE = 10.0;

    private static final double AD_RADIUS = 8.0;
    private static final double AD_DAMAGE = 5.0;

    private static final double KA_RADIUS = 8.0;
    private static final double KA_DAMAGE = 4.0;

    private static final double UE_DURATION = 10.0;

    private static final double PP_DAMAGE = 14.0;
    private static final double PJ_DAMAGE = 12.0;

    private static final double KAU_RADIUS = 12.0;
    private static final double KAU_DAMAGE = 20.0;

    private final List<String> abilities = new ArrayList<>();

    public RhittaListener(
            RhittaPlugin plugin,
            RhittaManager manager) {

        this.plugin = plugin;
        this.manager = manager;

        abilities.add("fireball");
        abilities.add("absolute_dominance");
        abilities.add("king_aura");
        abilities.add("unbreakable_ego");
        abilities.add("punishment_proud");
        abilities.add("prides_judgment");
        abilities.add("kings_authority");
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
    // NORMAL RHITTA ATTACK
    // ============================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(
            EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getDamager();

        if (!manager.isOwner(player)) {
            return;
        }

        ItemStack weapon =
                player.getInventory()
                        .getItemInMainHand();

        if (!manager.isRhitta(weapon)) {
            return;
        }

        // +20 physical attack
        event.setDamage(
                event.getDamage()
                        + PHYSICAL_ATTACK
        );

        // Life steal
        double newHealth =
                Math.min(
                        player.getHealth()
                                + LIFE_STEAL,
                        player.getMaxHealth()
                );

        player.setHealth(newHealth);

        // Defense grows on every hit
        manager.addDefense(
                player,
                DEFENSE_PER_HIT
        );
    }

    // ============================================================
    // RIGHT CLICK / SHIFT RIGHT CLICK
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

        if (!manager.isOwner(player)) {
            return;
        }

        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        event.setCancelled(true);

        /*
         * NORMAL RIGHT CLICK
         * = ACTIVATE
         */
        if (!player.isSneaking()) {

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

            return;
        }

        /*
         * SHIFT + RIGHT CLICK
         * = SELECT / CYCLE
         */

        cycleAbility(player);
    }

    // ============================================================
    // SELECT ABILITY
    // ============================================================

    private void cycleAbility(Player player) {

        String current =
                manager.getActiveAbility(player);

        int index =
                abilities.indexOf(current);

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
                "⚔ Rhitta Selected: "
                + ChatColor.YELLOW
                + formatAbility(next)
        );
    }

    // ============================================================
    // ACTIVATE ABILITY
    // ============================================================

    private void activateAbility(
            Player player,
            String ability) {

        if (ability == null) {
            return;
        }

        switch (ability.toLowerCase()) {

            case "fireball":
                activateFireball(player);
                break;

            case "absolute_dominance":
                activateAbsoluteDominance(player);
                break;

            case "king_aura":
                activateKingAura(player);
                break;

            case "unbreakable_ego":
                activateUnbreakableEgo(player);
                break;

            case "punishment_proud":
                activatePunishmentProud(player);
                break;

            case "prides_judgment":
                activatePridesJudgment(player);
                break;

            case "kings_authority":
                activateKingsAuthority(player);
                break;

            default:
                player.sendMessage(
                        ChatColor.RED +
                        "Unknown Rhitta ability."
                );
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

        Vector direction =
                eye.getDirection()
                        .normalize();

        Location spawn =
                eye.clone()
                        .add(
                                direction.clone()
                                        .multiply(1.2)
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
                "🔥 FIREBALL!"
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

        if (!manager.isOwner(shooter)) {
            return;
        }

        LivingEntity target =
                (LivingEntity)
                        event.getHitEntity();

        target.damage(
                FIREBALL_DAMAGE,
                shooter
        );
    }

    // ============================================================
    // ABSOLUTE DOMINANCE
    // ============================================================

    private void activateAbsoluteDominance(
            Player player) {

        if (manager.isOnCooldown(
                player,
                "ABSOLUTE_DOMINANCE")) {

            sendCooldown(
                    player,
                    "ABSOLUTE_DOMINANCE"
            );

            return;
        }

        manager.setCooldown(
                player,
                "ABSOLUTE_DOMINANCE",
                AD_COOLDOWN
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
                    > AD_RADIUS) {
                continue;
            }

            if (entity instanceof Player
                    && entity != player) {
                continue;
            }

            entity.damage(
                    AD_DAMAGE,
                    player
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * 6,
                            1,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20 * 4,
                            1,
                            false,
                            true,
                            true
                    )
            );

            affected++;
        }

        player.sendMessage(
                ChatColor.DARK_RED +
                "👑 ABSOLUTE DOMINANCE!"
        );

        player.sendMessage(
                ChatColor.GRAY.toString() +
                affected +
                " enemy/enemies overwhelmed."
        );
    }

    // ============================================================
    // KING'S AURA
    // ============================================================

    private void activateKingAura(
            Player player) {

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
                KA_COOLDOWN
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * 10,
                        1,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 10,
                        1,
                        false,
                        true,
                        true
                )
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
                    > KA_RADIUS) {
                continue;
            }

            if (entity instanceof Player) {
                continue;
            }

            entity.damage(
                    KA_DAMAGE,
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
                            20 * 4,
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
                "👑 KING'S AURA!"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Strength II + Resistance II"
        );

        player.sendMessage(
                ChatColor.GRAY.toString() +
                affected +
                " enemies affected."
        );
    }

    // ============================================================
    // UNBREAKABLE EGO
    // ============================================================

    private void activateUnbreakableEgo(
            Player player) {

        if (manager.isOnCooldown(
                player,
                "UNBREAKABLE_EGO")) {

            sendCooldown(
                    player,
                    "UNBREAKABLE_EGO"
            );

            return;
        }

        manager.setCooldown(
                player,
                "UNBREAKABLE_EGO",
                UE_COOLDOWN
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        (int) (20 * UE_DURATION),
                        3,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.ABSORPTION,
                        (int) (20 * UE_DURATION),
                        1,
                        false,
                        true,
                        true
                )
        );

        player.sendMessage(
                ChatColor.AQUA +
                "🛡 UNBREAKABLE EGO!"
        );

        player.sendMessage(
                ChatColor.GRAY +
                "Your defenses have become extremely powerful."
        );
    }

    // ============================================================
    // PUNISHMENT OF THE PROUD
    // ============================================================

    private void activatePunishmentProud(
            Player player) {

        if (manager.isOnCooldown(
                player,
                "PUNISHMENT_PROUD")) {

            sendCooldown(
                    player,
                    "PUNISHMENT_PROUD"
            );

            return;
        }

        manager.setCooldown(
                player,
                "PUNISHMENT_PROUD",
                PP_COOLDOWN
        );

        Location location =
                player.getLocation();

        Vector direction =
                player.getLocation()
                        .getDirection()
                        .normalize();

        int hit = 0;

        for (LivingEntity entity :
                player.getWorld()
                        .getLivingEntities()) {

            if (entity.equals(player)) {
                continue;
            }

            if (entity instanceof Player) {
                continue;
            }

            Location target =
                    entity.getLocation();

            Vector toTarget =
                    target.toVector()
                            .subtract(
                                    location.toVector()
                            );

            double distance =
                    toTarget.length();

            if (distance > 8.0) {
                continue;
            }

            if (distance <= 0) {
                continue;
            }

            double dot =
                    direction.dot(
                            toTarget.normalize()
                    );

            if (dot < 0.65) {
                continue;
            }

            entity.damage(
                    PP_DAMAGE,
                    player
            );

            entity.setFireTicks(60);

            hit++;
        }

        player.sendMessage(
                ChatColor.RED +
                "⚔ PUNISHMENT OF THE PROUD!"
        );

        player.sendMessage(
                ChatColor.GRAY.toString() +
                hit +
                " enemy/enemies punished."
        );
    }

    // ============================================================
    // PRIDE'S JUDGMENT
    // ============================================================

    private void activatePridesJudgment(
            Player player) {

        if (manager.isOnCooldown(
                player,
                "PRIDES_JUDGMENT")) {

            sendCooldown(
                    player,
                    "PRIDES_JUDGMENT"
            );

            return;
        }

        manager.setCooldown(
                player,
                "PRIDES_JUDGMENT",
                PJ_COOLDOWN
        );

        Location start =
                player.getEyeLocation();

        Vector direction =
                start.getDirection()
                        .normalize();

        int hit = 0;

        for (double distance = 1.0;
             distance <= 12.0;
             distance += 0.5) {

            Location point =
                    start.clone()
                            .add(
                                    direction.clone()
                                            .multiply(distance)
                            );

            for (LivingEntity entity :
                    player.getWorld()
                            .getLivingEntities()) {

                if (entity.equals(player)) {
                    continue;
                }

                if (entity instanceof Player) {
                    continue;
                }

                if (entity.getLocation()
                        .distance(point)
                        > 1.3) {
                    continue;
                }

                entity.damage(
                        PJ_DAMAGE,
                        player
                );

                entity.setFireTicks(100);

                hit++;
            }
        }

        player.sendMessage(
                ChatColor.LIGHT_PURPLE +
                "⚖ PRIDE'S JUDGMENT!"
        );

        player.sendMessage(
                ChatColor.GRAY.toString() +
                hit +
                " enemy/enemies judged."
        );
    }

    // ============================================================
    // KING'S AUTHORITY ULTIMATE
    // ============================================================

    private void activateKingsAuthority(
            Player player) {

        if (manager.isOnCooldown(
                player,
                "KINGS_AUTHORITY")) {

            sendCooldown(
                    player,
                    "KINGS_AUTHORITY"
            );

            return;
        }

        manager.setCooldown(
                player,
                "KINGS_AUTHORITY",
                KAU_COOLDOWN
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
                    > KAU_RADIUS) {
                continue;
            }

            if (entity instanceof Player) {
                continue;
            }

            entity.damage(
                    KAU_DAMAGE,
                    player
            );

            entity.setFireTicks(20 * 5);

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * 8,
                            2,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20 * 6,
                            2,
                            false,
                            true,
                            true
                    )
            );

            affected++;
        }

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * 15,
                        2,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 15,
                        1,
                        false,
                        true,
                        true
                )
        );

        player.sendMessage(
                ChatColor.GOLD +
                "☀ KING'S AUTHORITY ULTIMATE!"
        );

        player.sendMessage(
                ChatColor.YELLOW.toString() +
                affected +
                " enemies overwhelmed."
        );
    }

    // ============================================================
    // COOLDOWN
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
                formatAbility(ability)
                + " is on cooldown: "
                + String.format(
                        "%.1f",
                        seconds
                )
                + "s"
        );
    }

    // ============================================================
    // FORMAT ABILITY
    // ============================================================

    private String formatAbility(
            String ability) {

        if (ability == null) {
            return "None";
        }

        switch (ability.toLowerCase()) {

            case "fireball":
                return "Fireball";

            case "absolute_dominance":
                return "Absolute Dominance";

            case "king_aura":
                return "King's Aura";

            case "unbreakable_ego":
                return "Unbreakable Ego";

            case "punishment_proud":
                return "Punishment of the Proud";

            case "prides_judgment":
                return "Pride's Judgment";

            case "kings_authority":
                return "King's Authority Ultimate";

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

        if (!manager.isOwner(player)) {
            return;
        }

        if (!manager.hasRhitta(player)) {
            return;
        }

        int defense =
                manager.getDefense(player);

        if (defense <= 0) {
            return;
        }

        // 1 defense = 1% reduction
        // Maximum = 80%
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