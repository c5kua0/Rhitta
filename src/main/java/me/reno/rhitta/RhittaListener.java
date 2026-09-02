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
    // NORMAL COOLDOWNS
    // ============================================================

    private static final long FIREBALL_COOLDOWN = 3000L;
    private static final long AD_COOLDOWN = 20000L;
    private static final long KA_COOLDOWN = 15000L;
    private static final long UE_COOLDOWN = 30000L;
    private static final long PP_COOLDOWN = 8000L;
    private static final long PJ_COOLDOWN = 10000L;
    private static final long KAU_COOLDOWN = 60000L;

    // ============================================================
    // DAMAGE
    // ============================================================

    private static final double FIREBALL_DAMAGE = 10.0;

    // Absolute Dominance
    private static final double AD_DAMAGE = 30.0;

    private static final double KA_DAMAGE = 4.0;

    // Punishment of the Proud
    private static final double PP_DAMAGE = 35.0;

    // Pride's Judgment
    private static final double PJ_DAMAGE = 45.0;

    private static final double KAU_DAMAGE = 20.0;

    // ============================================================
    // RADIUS / RANGE
    // ============================================================

    // Absolute Dominance
    private static final double AD_RADIUS = 15.0;

    private static final double KA_RADIUS = 8.0;

    private static final double KAU_RADIUS = 12.0;

    // Pride's Judgment
    private static final double PJ_RANGE = 30.0;
    private static final double PJ_THICKNESS = 1.8;
    private static final double PJ_SEGMENT_LENGTH = 4.0;

    // Punishment of the Proud
    private static final double PP_RANGE = 10.0;

    // ============================================================
    // DURATIONS
    // ============================================================

    private static final int UE_DURATION = 10;
    private static final int KA_DURATION = 10;
    private static final int KAU_DURATION = 15;

    // Absolute Dominance
    private static final int AD_SLOW_DURATION = 6;
    private static final int AD_WEAKNESS_DURATION = 6;
    private static final int AD_FIRE_DURATION = 5;
    private static final int AD_DARKNESS_DURATION = 3;
    private static final int AD_WITHER_DURATION = 4;

    // Awakening
    private static final long AWAKENING_DURATION = 30_000L;

    // ============================================================
    // AWAKENED PLAYERS
    // ============================================================

    private final Set<UUID> awakenedPlayers =
            new HashSet<>();

    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public RhittaListener(
            RhittaPlugin plugin,
            RhittaManager manager) {

        this.plugin = plugin;
        this.manager = manager;

        startAwakeningAuraTask();
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

        // Awakening ends on death
        awakenedPlayers.remove(
                player.getUniqueId()
        );

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

        int slot = event.getNewSlot();

        // Slot 8 = Awakening
        if (slot == 7) {

            player.sendActionBar(
                    ChatColor.GOLD +
                    "⚡ AWAKENING — SHIFT + RIGHT CLICK"
            );

            return;
        }

        String ability =
                manager.getAbilityForSlot(slot);

        if (ability == null) {
            return;
        }

        String prefix =
                isAwakened(player)
                        ? ChatColor.RED + "⚡ AWAKENED: "
                        : ChatColor.GOLD.toString();

        player.sendActionBar(
                prefix +
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
    // SKILL RIGHT CLICK
    // ============================================================

    @EventHandler(
            priority = EventPriority.HIGHEST,
            ignoreCancelled = false
    )
    public void onSkillUse(PlayerInteractEvent event) {

        // MAIN HAND ONLY
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        // RIGHT CLICK ONLY
        if (event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }

        Player player =
                event.getPlayer();

        // OWNER ONLY
        if (!manager.isOwner(player)) {
            return;
        }

        // SKILLS ENABLED
        if (!manager.isSkillsEnabled()) {
            return;
        }

        /*
         * Prevent accidental activation while eating.
         *
         * Empty hand still works.
         * Rhitta does NOT need to be held.
         */
        ItemStack item =
                player.getInventory()
                        .getItemInMainHand();

        if (item != null &&
                item.getType().isEdible()) {

            return;
        }

        int slot =
                player.getInventory()
                        .getHeldItemSlot();

        // ========================================================
        // SLOT 8 = AWAKENING
        // ========================================================

        if (slot == 7) {

            if (!player.isSneaking()) {
                return;
            }

            event.setCancelled(true);

            activateAwakening(player);

            return;
        }

        // ========================================================
        // SLOTS 1–7
        // ========================================================

        String ability =
                manager.getAbilityForSlot(slot);

        if (ability == null) {
            return;
        }

        event.setCancelled(true);

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

        /*
         * Awakening changes the skills automatically.
         */
        if (isAwakened(player)) {

            switch (ability.toLowerCase()) {

                case "fireball":
                    activateAwakenedFireball(player);
                    return;

                case "king_aura":
                    activateAwakenedKingAura(player);
                    return;

                case "absolute_dominance":
                    activateAwakenedAbsoluteDominance(player);
                    return;

                case "unbreakable_ego":
                    activateAwakenedUnbreakableEgo(player);
                    return;

                case "punishment_proud":
                    activateAwakenedPunishmentProud(player);
                    return;

                case "prides_judgment":
                    activateAwakenedPridesJudgment(player);
                    return;

                case "kings_authority":
                    activateAwakenedKingsAuthority(player);
                    return;

                default:
                    break;
            }
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
    // AWAKENING
    // ============================================================

    private void activateAwakening(
            Player player) {

        if (isAwakened(player)) {

            player.sendMessage(
                    ChatColor.RED +
                    "You are already awakened."
            );

            return;
        }

        awakenedPlayers.add(
                player.getUniqueId()
        );

        // ========================================================
        // GLOBAL MESSAGE
        // ========================================================

        plugin.getServer()
                .broadcastMessage(
                        ChatColor.DARK_RED +
                        ChatColor.BOLD +
                        "Who Decided That?"
                );

        // ========================================================
        // ACTIVATION EFFECT
        // ========================================================

        Location location =
                player.getLocation();

        player.getWorld().spawnParticle(
                Particle.FLAME,
                location.clone().add(0, 1, 0),
                100,
                1.0,
                1.2,
                1.0,
                0.08
        );

        player.getWorld().spawnParticle(
                Particle.TOTEM_OF_UNDYING,
                location.clone().add(0, 1, 0),
                50,
                0.8,
                1.0,
                0.8,
                0.08
        );

        player.getWorld().spawnParticle(
                Particle.CRIT,
                location.clone().add(0, 1, 0),
                50,
                0.8,
                1.0,
                0.8,
                0.15
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                1.5f,
                0.5f
        );

        player.sendTitle(
                ChatColor.DARK_RED +
                        "AWAKENED",
                ChatColor.GOLD +
                        "Who Decided That?",
                10,
                40,
                10
        );

        player.sendMessage(
                ChatColor.DARK_RED +
                ChatColor.BOLD +
                "⚡ RHITTA AWAKENED"
        );

        player.sendMessage(
                ChatColor.GOLD +
                "Your skills have awakened."
        );

        // ========================================================
        // AUTOMATIC END
        // ========================================================

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                awakenedPlayers.remove(
                                        player.getUniqueId()
                                );
                                return;
                            }

                            if (!awakenedPlayers.contains(
                                    player.getUniqueId()
                            )) {
                                return;
                            }

                            awakenedPlayers.remove(
                                    player.getUniqueId()
                            );

                            player.sendMessage(
                                    ChatColor.GRAY +
                                    "⚡ Awakening has ended."
                            );

                            player.sendTitle(
                                    ChatColor.GRAY +
                                            "AWAKENING ENDED",
                                    ChatColor.DARK_GRAY +
                                            "The power fades...",
                                    5,
                                    30,
                                    10
                            );

                            player.getWorld().spawnParticle(
                                    Particle.SMOKE,
                                    player.getLocation()
                                            .add(0, 1, 0),
                                    50,
                                    0.7,
                                    1.0,
                                    0.7,
                                    0.04
                            );

                        },
                        20L * 30
                );
    }

    // ============================================================
    // AWAKENING CHECK
    // ============================================================

    private boolean isAwakened(
            Player player) {

        return awakenedPlayers.contains(
                player.getUniqueId()
        );
    }

    // ============================================================
    // CONTINUOUS FLAME AURA
    // ============================================================

    private void startAwakeningAuraTask() {

        plugin.getServer()
                .getScheduler()
                .runTaskTimer(
                        plugin,
                        () -> {

                            if (awakenedPlayers.isEmpty()) {
                                return;
                            }

                            for (UUID uuid :
                                    new HashSet<>(
                                            awakenedPlayers
                                    )) {

                                Player player =
                                        plugin.getServer()
                                                .getPlayer(uuid);

                                if (player == null ||
                                        !player.isOnline()) {

                                    awakenedPlayers.remove(uuid);
                                    continue;
                                }

                                Location center =
                                        player.getLocation()
                                                .add(0, 1, 0);

                                // Flame aura
                                for (double angle = 0;
                                     angle < Math.PI * 2;
                                     angle += Math.PI / 10) {

                                    double x =
                                            Math.cos(angle) * 0.75;

                                    double z =
                                            Math.sin(angle) * 0.75;

                                    double y =
                                            Math.sin(
                                                    angle * 2
                                            ) * 0.35;

                                    player.getWorld()
                                            .spawnParticle(
                                                    Particle.FLAME,
                                                    center.clone()
                                                            .add(
                                                                    x,
                                                                    y,
                                                                    z
                                                            ),
                                                    2,
                                                    0.03,
                                                    0.08,
                                                    0.03,
                                                    0.01
                                            );
                                }

                                player.getWorld()
                                        .spawnParticle(
                                                Particle.SOUL_FIRE_FLAME,
                                                center,
                                                3,
                                                0.35,
                                                0.6,
                                                0.35,
                                                0.02
                                        );
                            }

                        },
                        1L,
                        2L
                );
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
    // AWAKENED FIREBALL
    // ============================================================

    private void activateAwakenedFireball(
            Player player) {

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

        Fireball fireball =
                player.getWorld().spawn(
                        eye.clone()
                                .add(
                                        direction.clone()
                                                .multiply(1.5)
                                ),
                        Fireball.class
                );

        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setYield(3.0F);
        fireball.setIsIncendiary(false);

        player.getWorld().spawnParticle(
                Particle.FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                60,
                0.6,
                0.8,
                0.6,
                0.08
        );

        player.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                30,
                0.5,
                0.7,
                0.5,
                0.04
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SHOOT,
                1.3f,
                0.5f
        );

        sendActivated(
                player,
                "⚡ AWAKENED FIREBALL"
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
                70,
                0.7,
                0.7,
                0.7,
                0.08
        );

        fireball.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                location,
                35,
                0.6,
                0.6,
                0.6,
                0.05
        );

        fireball.getWorld().spawnParticle(
                Particle.SMOKE,
                location,
                30,
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

                double damage =
                        fireball.getYield() > 0
                                ? 25.0
                                : FIREBALL_DAMAGE;

                target.damage(
                        damage,
                        shooter
                );

                target.setFireTicks(
                        fireball.getYield() > 0
                                ? 120
                                : 60
                );

                target.getWorld().spawnParticle(
                        Particle.FLAME,
                        target.getLocation()
                                .add(0, 1, 0),
                        40,
                        0.5,
                        0.7,
                        0.5,
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
    // AWAKENED KING'S AURA
    // ============================================================

    private void activateAwakenedKingAura(
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
                        2,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SPEED,
                        20 * 15,
                        1,
                        false,
                        true,
                        true
                )
        );

        spawnRing(
                player,
                Particle.FLAME,
                10.0
        );

        int affected = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        10.0)) {

            entity.damage(
                    15.0,
                    player
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

            entity.setFireTicks(100);

            spawnHitParticles(
                    entity,
                    Particle.FLAME
            );

            affected++;
        }

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_AMBIENT,
                1.3f,
                0.7f
        );

        sendActivated(
                player,
                "⚡ AWAKENED KING'S AURA"
        );

        player.sendMessage(
                ChatColor.GRAY +
                affected +
                " enemies dominated."
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
    // AWAKENED UNBREAKABLE EGO
    // ============================================================

    private void activateAwakenedUnbreakableEgo(
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
                        20 * 15,
                        4,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.ABSORPTION,
                        20 * 15,
                        3,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.REGENERATION,
                        20 * 10,
                        1,
                        false,
                        true,
                        true
                )
        );

        spawnRing(
                player,
                Particle.TOTEM_OF_UNDYING,
                6.0
        );

        spawnRing(
                player,
                Particle.FLAME,
                5.0
        );

        spawnParticles(
                player,
                Particle.ENCHANT,
                80
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ITEM_TOTEM_USE,
                1.5f,
                0.5f
        );

        sendActivated(
                player,
                "⚡ AWAKENED UNBREAKABLE EGO"
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
                        .multiply(3.0);

        dash.setY(
                Math.max(
                        0.45,
                        direction.getY() * 0.6
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
                1.2f,
                0.5f
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

            if (dot < 0.25) {
                continue;
            }

            entity.damage(
                    PP_DAMAGE,
                    player
            );

            entity.setFireTicks(100);

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * 5,
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

            hit++;
        }

        player.getWorld().spawnParticle(
                Particle.FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                80,
                1.0,
                1.0,
                1.0,
                0.08
        );

        player.sendMessage(
                ChatColor.GRAY +
                String.valueOf(hit) +
                " enemy/enemies hit by the dash."
        );
    }

    // ============================================================
    // AWAKENED PUNISHMENT
    // ============================================================

    private void activateAwakenedPunishmentProud(
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
                        .multiply(4.5);

        dash.setY(
                Math.max(
                        0.55,
                        direction.getY() * 0.8
                )
        );

        player.setVelocity(dash);

        spawnDashTrail(
                player,
                direction
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SHOOT,
                1.5f,
                0.4f
        );

        plugin.getServer()
                .getScheduler()
                .runTaskLater(
                        plugin,
                        () -> damageAwakenedDashTargets(player),
                        3L
                );

        sendActivated(
                player,
                "⚡ AWAKENED PUNISHMENT"
        );
    }

    private void damageAwakenedDashTargets(
            Player player) {

        if (!player.isOnline()) {
            return;
        }

        Location location =
                player.getLocation();

        int hit = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        12.0)) {

            entity.damage(
                    60.0,
                    player
            );

            entity.setFireTicks(
                    20 * 8
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * 8,
                            3,
                            false,
                            true,
                            true
                    )
            );

            spawnHitParticles(
                    entity,
                    Particle.FLAME
            );

            hit++;
        }

        player.getWorld().spawnParticle(
                Particle.FLAME,
                location.add(0, 1, 0),
                120,
                1.5,
                1.0,
                1.5,
                0.1
        );

        player.getWorld().spawnParticle(
                Particle.CRIT,
                player.getLocation()
                        .add(0, 1, 0),
                80,
                1.2,
                1.0,
                1.2,
                0.15
        );

        player.sendMessage(
                ChatColor.RED +
                hit +
                " enemies destroyed."
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

        fireJudgmentBeam(
                player,
                PJ_DAMAGE,
                PJ_RANGE,
                100,
                3,
                20 * 5
        );

        sendActivated(
                player,
                "⚖ PRIDE'S JUDGMENT"
        );
    }

    // ============================================================
    // AWAKENED PRIDE'S JUDGMENT
    // ============================================================

    private void activateAwakenedPridesJudgment(
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

        fireJudgmentBeam(
                player,
                80.0,
                40.0,
                180,
                5,
                20 * 10
        );

        sendActivated(
                player,
                "⚡ AWAKENED PRIDE'S JUDGMENT"
        );
    }

    // ============================================================
    // JUDGMENT BEAM
    // ============================================================

    private void fireJudgmentBeam(
            Player player,
            double damage,
            double range,
            int flameAmount,
            int weaknessAmplifier,
            int fireTicks) {

        Location start =
                player.getEyeLocation();

        Vector direction =
                start.getDirection()
                        .normalize();

        Set<UUID> alreadyHit =
                new HashSet<>();

        for (double distance = 0.8;
             distance <= range;
             distance += 0.20) {

            Location point =
                    start.clone()
                            .add(
                                    direction.clone()
                                            .multiply(distance)
                            );

            player.getWorld().spawnParticle(
                    Particle.FLAME,
                    point,
                    5,
                    0.35,
                    0.35,
                    0.35,
                    0.03
            );

            player.getWorld().spawnParticle(
                    Particle.DUST,
                    point,
                    5,
                    PJ_THICKNESS * 0.5,
                    PJ_THICKNESS * 0.5,
                    PJ_THICKNESS * 0.5,
                    0,
                    new Particle.DustOptions(
                            Color.RED,
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
                        range
                ) > PJ_THICKNESS) {

                    continue;
                }

                entity.damage(
                        damage,
                        player
                );

                entity.setFireTicks(
                        fireTicks
                );

                entity.addPotionEffect(
                        new PotionEffect(
                                PotionEffectType.WEAKNESS,
                                20 * 5,
                                weaknessAmplifier,
                                false,
                                true,
                                true
                        )
                );

                alreadyHit.add(
                        entity.getUniqueId()
                );

                Location hitLocation =
                        entity.getLocation()
                                .add(0, 1, 0);

                entity.getWorld().spawnParticle(
                        Particle.FLAME,
                        hitLocation,
                        flameAmount,
                        0.7,
                        0.8,
                        0.7,
                        0.06
                );

                entity.getWorld().spawnParticle(
                        Particle.CRIT,
                        hitLocation,
                        30,
                        0.6,
                        0.7,
                        0.6,
                        0.1
                );

                entity.getWorld().spawnParticle(
                        Particle.DUST,
                        hitLocation,
                        25,
                        0.7,
                        0.7,
                        0.7,
                        0,
                        new Particle.DustOptions(
                                Color.RED,
                                3.5f
                        )
                );
            }
        }

        Location end =
                start.clone()
                        .add(
                                direction.clone()
                                        .multiply(range)
                        );

        player.getWorld().spawnParticle(
                Particle.FLAME,
                end,
                100,
                1.5,
                1.5,
                1.5,
                0.08
        );

        player.getWorld().spawnParticle(
                Particle.EXPLOSION,
                end,
                5,
                0.5,
                0.5,
                0.5,
                0
        );

        player.getWorld().spawnParticle(
                Particle.CRIT,
                start,
                60,
                1.0,
                1.0,
                1.0,
                0.15
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_EVOKER_CAST_SPELL,
                1.5f,
                0.4f
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SHOOT,
                1.2f,
                0.6f
        );

        player.sendMessage(
                ChatColor.GRAY +
                alreadyHit.size() +
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

        spawnRing(
                player,
                Particle.FLAME,
                AD_RADIUS
        );

        spawnParticles(
                player,
                Particle.ANGRY_VILLAGER,
                40
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

            // Slowness 355
            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20 * AD_SLOW_DURATION,
                            354,
                            false,
                            true,
                            true
                    )
            );

            // Weakness IV
            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * AD_WEAKNESS_DURATION,
                            3,
                            false,
                            true,
                            true
                    )
            );

            // Darkness
            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.DARKNESS,
                            20 * AD_DARKNESS_DURATION,
                            0,
                            false,
                            true,
                            true
                    )
            );

            // Wither II
            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WITHER,
                            20 * AD_WITHER_DURATION,
                            1,
                            false,
                            true,
                            true
                    )
            );

            // Fire
            entity.setFireTicks(
                    20 * AD_FIRE_DURATION
            );

            spawnHitParticles(
                    entity,
                    Particle.FLAME
            );

            entity.getWorld().spawnParticle(
                    Particle.SOUL_FIRE_FLAME,
                    entity.getLocation()
                            .add(0, 1, 0),
                    30,
                    0.5,
                    0.7,
                    0.5,
                    0.04
            );

            affected++;
        }

        // CASTER BUFFS
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * 8,
                        2,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 8,
                        2,
                        false,
                        true,
                        true
                )
        );

        player.getWorld().spawnParticle(
                Particle.EXPLOSION,
                player.getLocation()
                        .add(0, 1, 0),
                8,
                1.0,
                1.0,
                1.0,
                0
        );

        player.getWorld().spawnParticle(
                Particle.FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                120,
                1.5,
                1.2,
                1.5,
                0.1
        );

        player.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                60,
                1.0,
                1.0,
                1.0,
                0.06
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_AMBIENT,
                1.5f,
                0.5f
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                1.3f,
                0.6f
        );

        sendActivated(
                player,
                "👑 ABSOLUTE DOMINANCE"
        );

        player.sendMessage(
                ChatColor.RED +
                "30 Damage • Slowness 355 • Weakness IV"
        );

        player.sendMessage(
                ChatColor.GRAY +
                affected +
                " enemies completely dominated."
        );
    }

    // ============================================================
    // AWAKENED ABSOLUTE DOMINANCE
    // ============================================================

    private void activateAwakenedAbsoluteDominance(
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

        double radius = 20.0;

        spawnRing(
                player,
                Particle.FLAME,
                radius
        );

        spawnRing(
                player,
                Particle.SOUL_FIRE_FLAME,
                radius * 0.8
        );

        int affected = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        radius)) {

            entity.damage(
                    75.0,
                    player
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20 * 8,
                            354,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * 8,
                            4,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.DARKNESS,
                            20 * 5,
                            0,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WITHER,
                            20 * 6,
                            2,
                            false,
                            true,
                            true
                    )
            );

            entity.setFireTicks(
                    20 * 10
            );

            spawnHitParticles(
                    entity,
                    Particle.SOUL_FIRE_FLAME
            );

            affected++;
        }

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * 12,
                        3,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 12,
                        3,
                        false,
                        true,
                        true
                )
        );

        player.getWorld().spawnParticle(
                Particle.EXPLOSION,
                player.getLocation()
                        .add(0, 1, 0),
                15,
                1.5,
                1.5,
                1.5,
                0
        );

        player.getWorld().spawnParticle(
                Particle.FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                200,
                2.0,
                1.5,
                2.0,
                0.12
        );

        player.getWorld().spawnParticle(
                Particle.SOUL_FIRE_FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                100,
                1.5,
                1.5,
                1.5,
                0.08
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                1.8f,
                0.4f
        );

        sendActivated(
                player,
                "⚡ AWAKENED ABSOLUTE DOMINANCE"
        );

        player.sendMessage(
                ChatColor.RED +
                "75 DAMAGE — TOTAL DOMINATION"
        );

        player.sendMessage(
                ChatColor.GRAY +
                affected +
                " enemies overwhelmed."
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
    // AWAKENED KING'S AUTHORITY
    // ============================================================

    private void activateAwakenedKingsAuthority(
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

        double radius = 20.0;

        spawnRing(
                player,
                Particle.FLAME,
                radius
        );

        spawnRing(
                player,
                Particle.END_ROD,
                radius * 0.75
        );

        spawnRing(
                player,
                Particle.SOUL_FIRE_FLAME,
                radius * 0.5
        );

        spawnParticles(
                player,
                Particle.TOTEM_OF_UNDYING,
                120
        );

        int affected = 0;

        for (LivingEntity entity :
                getNearbyEnemies(
                        player,
                        radius)) {

            entity.damage(
                    60.0,
                    player
            );

            entity.setFireTicks(
                    20 * 10
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.WEAKNESS,
                            20 * 10,
                            3,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.SLOWNESS,
                            20 * 8,
                            3,
                            false,
                            true,
                            true
                    )
            );

            entity.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.DARKNESS,
                            20 * 5,
                            0,
                            false,
                            true,
                            true
                    )
            );

            spawnHitParticles(
                    entity,
                    Particle.SOUL_FIRE_FLAME
            );

            affected++;
        }

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * 20,
                        3,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.RESISTANCE,
                        20 * 20,
                        2,
                        false,
                        true,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.REGENERATION,
                        20 * 10,
                        2,
                        false,
                        true,
                        true
                )
        );

        player.getWorld().spawnParticle(
                Particle.EXPLOSION,
                player.getLocation()
                        .add(0, 1, 0),
                20,
                2.0,
                1.5,
                2.0,
                0
        );

        player.getWorld().spawnParticle(
                Particle.FLAME,
                player.getLocation()
                        .add(0, 1, 0),
                250,
                2.5,
                2.0,
                2.5,
                0.12
        );

        player.getWorld().playSound(
                player.getLocation(),
                Sound.ENTITY_WITHER_SPAWN,
                2.0f,
                0.5f
        );

        sendActivated(
                player,
                "⚡ AWAKENED KING'S AUTHORITY"
        );

        player.sendMessage(
                ChatColor.RED +
                affected +
                " enemies have been crushed."
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

            // DO NOT HIT THE CASTER
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
                20,
                0.4,
                0.6,
                0.4,
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
             angle += Math.PI / 40) {

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

        for (int i = 0; i < 24; i++) {

            Location point =
                    location.clone()
                            .subtract(
                                    direction.clone()
                                            .multiply(
                                                    i * 0.35
                                            )
                            );

            player.getWorld().spawnParticle(
                    Particle.FLAME,
                    point,
                    6,
                    0.18,
                    0.18,
                    0.18,
                    0.03
            );

            player.getWorld().spawnParticle(
                    Particle.CRIT,
                    point,
                    3,
                    0.1,
                    0.1,
                    0.1,
                    0.08
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

        awakenedPlayers.remove(
                player.getUniqueId()
        );

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