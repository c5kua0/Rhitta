package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    private long lastFireball = 0L;
    private static final long FIREBALL_COOLDOWN = 3000L; // ms
    private static final double LOW_HEALTH = 2.0;
    private static final int BUFF_DURATION = 20 * 60; // ticks, resurrection buff

    // Tracks which in-game "day" each world last fired its sunrise/noon buff,
    // so a buff triggers once per threshold per day instead of every tick
    // that the check happens to run while time sits at/after that value.
    private final Map<UUID, Long> lastSunriseDay = new HashMap<>();
    private final Map<UUID, Long> lastNoonDay = new HashMap<>();

    public RhittaListener(RhittaPlugin plugin, RhittaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // ----------------------------------------------------------------
    // Join / respawn - keep exactly one copy in the owner's hands
    // ----------------------------------------------------------------

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (manager.isOwner(player)) {
            manager.forceOneRhitta(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            manager.forceOneRhitta(player);
        }, 1L);
    }

    // ----------------------------------------------------------------
    // Resurrection
    // ----------------------------------------------------------------

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onLowHealth(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        if (event.isCancelled()) return;
        if (!manager.hasRhitta(player)) return;

        ItemStack rhitta = findRhitta(player);
        if (rhitta == null || manager.hasUsedResurrection(rhitta)) return;

        double remainingHealth = player.getHealth() - event.getFinalDamage();
        if (remainingHealth > LOW_HEALTH) return;

        event.setCancelled(true);
        manager.markResurrectionUsed(rhitta);

        player.setHealth(Math.min(LOW_HEALTH + 1.0, player.getMaxHealth()));
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Who decided that?");

        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, BUFF_DURATION, 1, false, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, BUFF_DURATION, 1, false, false, false));
    }

    // ----------------------------------------------------------------
    // Life steal / defense stacking
    // ----------------------------------------------------------------

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!manager.isRhitta(weapon)) return;

        double lifeSteal = plugin.getConfig().getDouble("weapon.life-steal", 4.0);
        double heal = Math.min(player.getHealth() + lifeSteal, player.getMaxHealth());
        player.setHealth(heal);

        double defensePerHit = plugin.getConfig().getDouble("weapon.defense-per-hit", 1.0);
        manager.addDefense(player, (int) defensePerHit);

        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        if (attribute != null) {
            // Defense is tracked separately; nothing to do to the attribute
            // itself here, this hook is left for future scaling logic.
        }
    }

    // ----------------------------------------------------------------
    // Fireball ability (right click)
    // ----------------------------------------------------------------

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!manager.isRhitta(item)) return;

        long now = System.currentTimeMillis();
        if (now - lastFireball < FIREBALL_COOLDOWN) return;
        lastFireball = now;

        Location eye = player.getEyeLocation();
        org.bukkit.util.Vector direction = eye.getDirection().normalize();
        Location spawnLoc = eye.clone().add(direction.clone().multiply(1.0));

        Fireball fireball = (Fireball) player.getWorld().spawn(spawnLoc, Fireball.class);
        fireball.setShooter(player);
        fireball.setDirection(direction);
        fireball.setYield(0F);
        fireball.setIsIncendiary(false);
    }

    @EventHandler
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Fireball)) return;
        Fireball fireball = (Fireball) event.getEntity();
        if (!(fireball.getShooter() instanceof Player)) return;

        Entity hit = event.getHitEntity();
        if (!(hit instanceof LivingEntity)) return;

        onFireballDamage((LivingEntity) hit);
    }

    private void onFireballDamage(LivingEntity target) {
        double damage = plugin.getConfig().getDouble("weapon.fireball-damage", 4.0);
        target.damage(damage);
    }