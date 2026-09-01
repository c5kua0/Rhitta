package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
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
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    // ============================================================
    // COMBAT
    // ============================================================

    private static final double PHYSICAL_ATTACK = 20.0;
    private static final double LIFE_STEAL = 4.0;
    private static final int DEFENSE_PER_HIT = 1;

    // ============================================================
    // COOLDOWNS
    // ============================================================

    private static final long FIREBALL_COOLDOWN = 3000L;
    private static final long AD_COOLDOWN = 20000L;
    private static final long KA_COOLDOWN = 15000L;
    private static final long UE_COOLDOWN = 30000L;
    private static final long PP_COOLDOWN = 12000L;
    private static final long PJ_COOLDOWN = 10000L;
    private static final long KAU_COOLDOWN = 60000L;

    // ============================================================
    // DAMAGE
    // ============================================================

    private static final double FIREBALL_DAMAGE = 10.0;
    private static final double AD_DAMAGE = 5.0;
    private static final double KA_DAMAGE = 4.0;
    private static final double PP_DAMAGE = 14.0;
    private static final double PJ_DAMAGE = 12.0;
    private static final double KAU_DAMAGE = 20.0;

    // ============================================================
    // RADIUS / RANGE
    // ============================================================

    private static final double AD_RADIUS = 8.0;
    private static final double KA_RADIUS = 8.0;
    private static final double KAU_RADIUS = 12.0;

    private static final double PJ_RANGE = 25.0;
    private static final double PJ_THICKNESS = 1.5;
    private static final double PJ_SEGMENT_LENGTH = 3.0;

    private static final double PP_RANGE = 8.0;

    // ============================================================
    // DURATIONS
    // ============================================================

    private static final int UE_DURATION = 10;
    private static final int KA_DURATION = 10;
    private static final int KAU_DURATION = 15;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

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

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> manager.forceOneRhitta(player),
                        1L
                );
    }

    // ============================================================
    // RESPAWN
    // ============================================================

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        if (!manager.isOwner(player)) {
            return;
        }

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            manager.forceOneRhitta(player);

                        },
                        2L
                );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            manager.removeDuplicates();

                        },
                        20L
                );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                return;
                            }

                            manager.removeDuplicates();

                        },
                        60L
                );
    }

    // ============================================================
    // HOTBAR SKILL INDICATOR
    // ============================================================

    @EventHandler
    public void onHotbarChange(PlayerItemHeldEvent event) {

        Player player = event.getPlayer();

        if (!manager.isOwner(player)) {
            return;
        }

        if (!manager.isSkillsEnabled()) {
            return;
        }

        String ability =
                manager.getAbilityForSlot(
                        event.getNewSlot()
                );

        if (ability == null) {
            return;
        }

        player.sendActionBar(
                ChatColor.GOLD +
                formatAbility(ability)
        );
    }

    // ============================================================
    // NORMAL RHITTA ATTACK
    // ============================================================

    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(EntityDamageByEntityEvent event) {

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

        event.setDamage(
                event.getDamage() + PHYSICAL_ATTACK
        );

        double newHealth =
                Math.min(
                        player.getHealth() + LIFE_STEAL,
                        player.getMaxHealth()
                );

        player.setHealth(newHealth);

        manager.addDefense(
                player,
                DEFENSE_PER_HIT
        );

        player.getWorld().spawnParticle(
                Particle.CRIT,
                player.getLocation().add(0, 1, 0),
                8,
                0.3,
                0.4,
                0.3,
                0.1
        );
    }

    // ============================================================
    // RHITTA SKILL RIGHT CLICK
    // ============================================================
    /*
     * IMPORTANT:
     *
     * Skills DO NOT require Rhitta to be held.
     *
     * The skill is determined ONLY by:
     *
     *     1. Player is Rhitta owner
     *     2. Skills are enabled
     *     3. Player is right-clicking with MAIN HAND
     *     4. Current hotbar slot
     *
     * Rhitta can be anywhere in the inventory.
     */

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onSkillUse(PlayerInteractEvent event) {

        // MAIN HAND ONLY
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // ONLY RIGHT CLICK AIR
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player =
                event.getPlayer();

        // OWNER ONLY
        if (!manager.isOwner(player)) {
            return;
        }

        // SKILLS MUST BE ENABLED
        if (!manager.isSkillsEnabled()) {
            return;
        }

        // ========================================================
        // IMPORTANT FIX
        // ========================================================
        //
        // We DO NOT check:
        //
        //     manager.isRhitta(item)
        //
        // The selected hotbar slot controls the skill.
        //

        int slot =
                player.getInventory()
                        .getHeldItemSlot();

        String ability =
                manager.getAbilityForSlot(slot);

        if (ability == null) {
            return;
        }

        // Stop the normal right-click action.
        event.setCancelled(true);

        // Activate the selected skill.
        activateAbility(
                player,
                ability
        );
    }

    // ============================================================
    // ABILITY ACTIVATION
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

            case "king_aura":
                activateKingAura(player);
                break;

            case "absolute_dominance":
                activateAbsoluteDominance(player);
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
                        "Unknown Rhitta skill."
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
                player.getWorld().spawn(
                        spawn,
                        Fireball.class
                );

        fireball.setShooter(player);
        fireball.setDirection(direction);

        fireball.setYield(0F);
        fireball.setIsIncendiary(false);

        spawnParticles(
                player,
                Particle.FLAME,
                20
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_BLAZE_SHOOT,
                1.0f,
                1.0f
        );

        sendActivated(
                player,
                "🔥 FIREBALL"
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

        Player shooter =
                (Player) fireball.getShooter();

        if (!manager.isOwner(shooter)) {
            return;
        }

        Location location =
                fireball.getLocation();

        fireball.getWorld().spawnParticle(
                Particle.FLAME,
                location,
                40,
                0.5,
                0.5,
                0.5,
                0.08
        );

        fireball.getWorld().spawnParticle(
                Particle.SMOKE,
                location,
                25,
                0.5,
                0.5,
                0.5,
                0.03
        );

        if (event.getHitEntity()
                instanceof LivingEntity) {

            LivingEntity target =
                    (LivingEntity) event.getHitEntity();

            if (target != shooter) {

                target.damage(
                        FIREBALL_DAMAGE,
                        shooter
                );

                target.getWorld().spawnParticle(
                        Particle.FLAME,
                        target.getLocation().add(0, 1, 0),
                        30,
                        0.4,
                        0.5,
                        0.4,
                        0.05
                );
            }
        }
    }

    // ============================================================
    // KING'S AURA
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
                KA_COOLDOWN
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * KA_DURATION,
                        1,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * KA_DURATION,
                        1,
                        false,
                        true,
                        true
                )
        );

        spawnRing(
                player,
                Particle.ENCHANT,
                KA_RADIUS
        );

        int affected = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        KA_RADIUS)) {

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

            spawnHitParticles(
                    entity,
                    Particle.ENCHANT
            );

            affected++;
        }

        sendActivated(
                player,
                "👑 KING'S AURA"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                "Strength II + Resistance II"
        );

        player.sendMessage(
                ChatColor.GRAY +
                String.valueOf(affected) +
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
                UE_DURATION * 3000L
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * UE_DURATION,
                        3,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.ABSORPTION,
                        20 * UE_DURATION,
                        1,
                        false,
                        true,
                        true
                )
        );

        spawnRing(
                player,
                Particle.TOTEM_OF_UNDYING,
                4.0
        );

        spawnParticles(
                player,
                Particle.ENCHANT,
                35
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ITEM_TOTEM_USE,
                1.0f,
                0.8f
        );

        sendActivated(
                player,
                "🛡 UNBREAKABLE EGO"
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

        Vector direction =
                player.getLocation()
                        .getDirection()
                        .normalize();

        Vector dash =
                direction.clone()
                        .multiply(1.8);

        dash.setY(
                Math.max(
                        0.35,
                        direction.getY() * 0.5
                )
        );

        player.setVelocity(dash);

        spawnDashTrail(
                player,
                direction
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_ATTACK_SWEEP,
                1.0f,
                0.6f
        );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> damageDashTargets(player),
                        2L
                );

        sendActivated(
                player,
                "⚔ PUNISHMENT OF THE PROUD"
        );
    }

    private void damageDashTargets(
            Player player) {

        if (!player.isOnline()) {
            return;
        }

        Location location =
                player.getLocation();

        Vector direction =
                location.getDirection()
                        .normalize();

        int hit = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        PP_RANGE)) {

            Vector toTarget =
                    entity.getLocation()
                            .toVector()
                            .subtract(
                                    location.toVector()
                            );

            double distance =
                    toTarget.length();

            if (distance <= 0) {
                continue;
            }

            double dot =
                    direction.dot(
                            toTarget.normalize()
                    );

            if (dot < 0.45) {
                continue;
            }

            entity.damage(
                    PP_DAMAGE,
                    player
            );

            entity.setFireTicks(60);

            spawnHitParticles(
                    entity,
                    Particle.FLAME
            );

            hit++;
        }

        player.sendMessage(
                ChatColor.GRAY +
                String.valueOf(hit) +
                " enemy/enemies hit by the dash."
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

        Set<UUID> alreadyHit =
                new HashSet<>();

        for (double distance = 0.8;
             distance <= PJ_RANGE;
             distance += 0.20) {

            Location point =
                    start.clone()
                            .add(
                                    direction.clone()
                                            .multiply(distance)
                            );

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    point,
                    6,
                    PJ_THICKNESS * 0.45,
                    PJ_THICKNESS * 0.45,
                    PJ_THICKNESS * 0.45,
                    0,
                    new Particle.DustOptions(
                            Color.RED,
                            2.5f
                    )
            );

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    point,
                    5,
                    PJ_THICKNESS * 0.65,
                    PJ_THICKNESS * 0.65,
                    PJ_THICKNESS * 0.65,
                    0,
                    new Particle.DustOptions(
                            Color.BLACK,
                            3.0f
                    )
            );

            player.getWorld().spawnParticle(
                    Particle.CRIT,
                    point,
                    3,
                    0.35,
                    0.35,
                    0.35,
                    0.08
            );

            player.getWorld().spawnParticle(
                    Particle.ENCHANT,
                    point,
                    2,
                    0.3,
                    0.3,
                    0.3,
                    0.08
            );

            for (LivingEntity entity :
                    getNearbyEnemies(
                            player,
                            distance + PJ_THICKNESS)) {

                if (alreadyHit.contains(
                        entity.getUniqueId())) {

                    continue;
                }

                Location entityLocation =
                        entity.getLocation()
                                .add(0, 1, 0);

                if (distanceFromLine(
                        entityLocation,
                        start,
                        direction,
                        PJ_RANGE
                ) > PJ_THICKNESS) {

                    continue;
                }

                entity.damage(
                        PJ_DAMAGE,
                        player
                );

                entity.setFireTicks(100);

                alreadyHit.add(
                        entity.getUniqueId()
                );

                Location hitLocation =
                        entity.getLocation()
                                .add(0, 1, 0);

                entity.getWorld().spawnParticle(
                        Particle.DUST,
                        hitLocation,
                        30,
                        0.6,
                        0.8,
                        0.6,
                        0,
                        new Particle.DustOptions(
                                Color.RED,
                                3.0f
                        )
                );

                entity.getWorld().spawnParticle(
                        Particle.DUST,
                        hitLocation,
                        20,
                        0.7,
                        0.7,
                        0.7,
                        0,
                        new Particle.DustOptions(
                                Color.BLACK,
                                3.5f
                        )
                );

                entity.getWorld().spawnParticle(
                        Particle.CRIT,
                        hitLocation,
                        20,
                        0.5,
                        0.7,
                        0.5,
                        0.1
                );
            }
        }

        Vector flashDirection =
                direction.clone()
                        .multiply(PJ_SEGMENT_LENGTH);

        Location flashEnd =
                start.clone()
                        .add(flashDirection);

        player.getWorld().spawnParticle(
                Particle.DUST,
                flashEnd,
                35,
                1.0,
                1.0,
                1.0,
                0,
                new Particle.DustOptions(
                        Color.RED,
                        3.5f
                )
        );

        player.getWorld().spawnParticle(
                Particle.DUST,
                flashEnd,
                30,
                1.0,
                1.0,
                1.0,
                0,
                new Particle.DustOptions(
                        Color.BLACK,
                        4.0f
                )
        );

        player.getWorld().spawnParticle(
                Particle.DUST,
                start,
                50,
                1.0,
                1.0,
                1.0,
                0,
                new Particle.DustOptions(
                        Color.RED,
                        3.5f
                )
        );

        player.getWorld().spawnParticle(
                Particle.DUST,
                start,
                40,
                1.0,
                1.0,
                1.0,
                0,
                new Particle.DustOptions(
                        Color.BLACK,
                        4.0f
                )
        );

        player.getWorld().spawnParticle(
                Particle.CRIT,
                start,
                25,
                0.8,
                0.8,
                0.8,
                0.15
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_EVOKER_CAST_SPELL,
                1.2f,
                0.5f
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SHOOT,
                1.0f,
                0.8f
        );

        sendActivated(
                player,
                "⚖ PRIDE'S JUDGMENT"
        );

        player.sendMessage(
                ChatColor.GRAY +
                String.valueOf(alreadyHit.size()) +
                " enemy/enemies judged."
        );
    }

    // ============================================================
    // DISTANCE FROM BEAM LINE
    // ============================================================

    private double distanceFromLine(
            Location point,
            Location start,
            Vector direction,
            double maxRange) {

        Vector relative =
                point.toVector()
                        .subtract(start.toVector());

        double projection =
                relative.dot(direction);

        if (projection < 0 ||
                projection > maxRange) {

            return Double.MAX_VALUE;
        }

        Vector closest =
                direction.clone()
                        .multiply(projection);

        return relative
                .subtract(closest)
                .length();
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

        spawnRing(
                player,
                Particle.SMOKE,
                AD_RADIUS
        );

        spawnParticles(
                player,
                Particle.ANGRY_VILLAGER,
                20
        );

        int affected = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        AD_RADIUS)) {

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

            spawnHitParticles(
                    entity,
                    Particle.SMOKE
            );

            affected++;
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_AMBIENT,
                0.8f,
                1.2f
        );

        sendActivated(
                player,
                "👑 ABSOLUTE DOMINANCE"
        );

        player.sendMessage(
                ChatColor.GRAY +
                String.valueOf(affected) +
                " enemy/enemies overwhelmed."
        );
    }

    // ============================================================
    // KING'S AUTHORITY
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

        spawnRing(
                player,
                Particle.FLAME,
                KAU_RADIUS
        );

        spawnRing(
                player,
                Particle.END_ROD,
                KAU_RADIUS * 0.7
        );

        spawnParticles(
                player,
                Particle.TOTEM_OF_UNDYING,
                50
        );

        int affected = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        KAU_RADIUS)) {

            entity.damage(
                    KAU_DAMAGE,
                    player
            );

            entity.setFireTicks(
                    20 * 5
            );

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

            spawnHitParticles(
                    entity,
                    Particle.FLAME
            );

            affected++;
        }

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * KAU_DURATION,
                        2,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * KAU_DURATION,
                        1,
                        false,
                        true,
                        true
                )
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                1.0f,
                0.8f
        );

        sendActivated(
                player,
                "☀ KING'S AUTHORITY ULTIMATE"
        );

        player.sendMessage(
                ChatColor.YELLOW +
                String.valueOf(affected) +
                " enemies overwhelmed."
        );
    }

    // ============================================================
    // NEARBY ENEMIES
    // ============================================================

    private Iterable<LivingEntity> getNearbyEnemies(
            Player player,
            double radius) {

        List<LivingEntity> result =
                new ArrayList<>();

        for (LivingEntity entity :
                player.getWorld()
                        .getLivingEntities()) {

            if (entity == player) {
                continue;
            }

            if (!entity.isValid()) {
                continue;
            }

            if (entity.getLocation()
                    .distanceSquared(
                            player.getLocation()
                    )
                    <= radius * radius) {

                result.add(entity);
            }
        }

        return result;
    }

    // ============================================================
    // PARTICLES
    // ============================================================

    private void spawnParticles(
            Player player,
            Particle particle,
            int amount) {

        Location location =
                player.getLocation()
                        .add(0, 1, 0);

        player.getWorld().spawnParticle(
                particle,
                location,
                amount,
                0.5,
                0.8,
                0.5,
                0.05
        );
    }

    private void spawnHitParticles(
            LivingEntity entity,
            Particle particle) {

        Location location =
                entity.getLocation()
                        .add(0, 1, 0);

        entity.getWorld().spawnParticle(
                particle,
                location,
                15,
                0.35,
                0.5,
                0.35,
                0.05
        );
    }

    // ============================================================
    // RING
    // ============================================================

    private void spawnRing(
            Player player,
            Particle particle,
            double radius) {

        World world =
                player.getWorld();

        Location center =
                player.getLocation()
                        .add(0, 0.15, 0);

        for (double angle = 0;
             angle < Math.PI * 2;
             angle += Math.PI / 20) {

            double x =
                    Math.cos(angle) * radius;

            double z =
                    Math.sin(angle) * radius;

            world.spawnParticle(
                    particle,
                    center.clone()
                            .add(x, 0, z),
                    2,
                    0,
                    0.05,
                    0,
                    0.01
            );
        }
    }

    // ============================================================
    // DASH TRAIL
    // ============================================================

    private void spawnDashTrail(
            Player player,
            Vector direction) {

        Location location =
                player.getLocation()
                        .add(0, 0.8, 0);

        for (int i = 0; i < 12; i++) {

            Location point =
                    location.clone()
                            .subtract(
                                    direction.clone()
                                            .multiply(
                                                    i * 0.25
                                            )
                            );

            player.getWorld().spawnParticle(
                    Particle.FLAME,
                    point,
                    4,
                    0.15,
                    0.15,
                    0.15,
                    0.02
            );

            player.getWorld().spawnParticle(
                    Particle.CLOUD,
                    point,
                    2,
                    0.1,
                    0.1,
                    0.1,
                    0.02
            );
        }
    }

    // ============================================================
    // ACTIVATION MESSAGE
    // ============================================================

    private void sendActivated(
            Player player,
            String ability) {

        player.sendMessage(
                ChatColor.GOLD +
                ability +
                ChatColor.YELLOW +
                " activated!"
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

    // ============================================================
    // FORMAT ABILITY
    // ============================================================

    private String formatAbility(
            String ability) {

        switch (ability.toLowerCase()) {

            case "fireball":
                return "Fireball";

            case "king_aura":
                return "King's Aura";

            case "unbreakable_ego":
                return "Unbreakable Ego";

            case "punishment_proud":
                return "Punishment of the Proud";

            case "prides_judgment":
                return "Pride's Judgment";

            case "absolute_dominance":
                return "Absolute Dominance";

            case "kings_authority":
                return "King's Authority";

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

    // ============================================================
    // INVENTORY DRAG
    // ============================================================

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

        Player player =
                event.getEntity();

        event.getDrops()
                .removeIf(manager::isRhitta);

        if (manager.isOwner(player)) {

            plugin.getServer()
                    .getScheduler()
                    .runTaskLater(
                            plugin,
                            () -> {

                                if (!player.isOnline()) {
                                    return;
                                }

                                manager.removeDuplicates();

                            },
                            40L
                    );
        }
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