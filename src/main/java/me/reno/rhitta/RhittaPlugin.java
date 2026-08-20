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


    // ----------------------------------------------------------------
    // Anti-duplication
    // ----------------------------------------------------------------

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();
        ItemStack stack = event.getItem().getItemStack();
        if (!manager.isRhitta(stack)) return;

        if (!manager.isOwner(player)) {
            event.setCancelled(true);
            return;
        }
        // Owner picking their own sword back up is fine, but never let
        // them end up holding two.
        if (manager.hasRhitta(player)) {
            event.setCancelled(true);
            event.getItem().remove();
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        if (manager.isRhitta(stack)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (manager.isRhitta(current) || manager.isRhitta(cursor)) {
            // Only block moves that would take it out of the player's own
            // inventory (into another container, e.g. a chest/shulker box).
            if (event.getClickedInventory() != null
                    && !(event.getClickedInventory() instanceof PlayerInventory)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack cursor = event.getOldCursor();
        if (manager.isRhitta(cursor)) {
            if (event.getInventory() != null && !(event.getInventory() instanceof PlayerInventory)) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * The actual dupe-on-death fix: strip every Rhitta copy from the
     * death drop list, from EVERY slot (main inventory, armor, off hand),
     * regardless of who died or whether they're the current owner.
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        drops.removeIf(manager::isRhitta);
    }

    // ----------------------------------------------------------------
    // Sunrise / Noon buffs
    // ----------------------------------------------------------------

    public void startBuffScheduler() {
        int interval = plugin.getConfig().getInt("buffs.check-interval-ticks", 20);
        new BukkitRunnable() {
            @Override
            public void run() {
                checkTimeBuffs();
            }
        }.runTaskTimer(plugin, interval, interval);
    }

    private void checkTimeBuffs() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (!manager.isOwner(player)) continue;
            if (!manager.hasRhitta(player)) continue;

            World world = player.getWorld();
            long time = world.getTime();
            long day = world.getFullTime() / 24000L;

            applyTimeBuff(player, world, "sunrise", time, day, lastSunriseDay);
            applyTimeBuff(player, world, "noon", time, day, lastNoonDay);
        }
    }

    private void applyTimeBuff(Player player, World world, String key, long time, long day,
                                Map<UUID, Long> lastTriggeredDay) {
        String path = "buffs." + key;
        if (!plugin.getConfig().getBoolean(path + ".enabled", false)) return;

        long triggerTick = plugin.getConfig().getLong(path + ".time-ticks", 0L);
        int window = plugin.getConfig().getInt(path + ".window-ticks", 40);

        boolean inWindow = time >= triggerTick && time < triggerTick + window;
        if (!inWindow) return;

        Long lastDay = lastTriggeredDay.get(player.getUniqueId());
        if (lastDay != null && lastDay == day) return; // already triggered today

        lastTriggeredDay.put(player.getUniqueId(), day);

        String effectName = plugin.getConfig().getString(path + ".effect", "SPEED");
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type == null) {
            plugin.getLogger().warning("Unknown potion effect '" + effectName + "' for buffs." + key + ".effect");
            return;
        }

        int amplifier = plugin.getConfig().getInt(path + ".amplifier", 0);
        int durationSeconds = plugin.getConfig().getInt(path + ".duration-seconds", 30);
        int durationTicks = durationSeconds * 20;

        player.addPotionEffect(new PotionEffect(type, durationTicks, amplifier, false, true, true));

        String message = plugin.getConfig().getString(path + ".message", "");
        if (message != null && !message.isEmpty()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    // ----------------------------------------------------------------
    // Helpers
    // ----------------------------------------------------------------

    private ItemStack findRhitta(Player player) {
        PlayerInventory inv = player.getInventory();
        if (manager.isRhitta(inv.getItemInMainHand())) return inv.getItemInMainHand();
        if (manager.isRhitta(inv.getItemInOffHand())) return inv.getItemInOffHand();
        for (ItemStack stack : inv.getContents()) {
            if (manager.isRhitta(stack)) return stack;
        }
        return null;
    }
}