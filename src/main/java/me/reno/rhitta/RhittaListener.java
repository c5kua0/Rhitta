package me.reno.rhitta;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RhittaListener implements Listener {

    private static final String OWNER = "_ToshiroCyMc";

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    public RhittaListener(RhittaPlugin plugin, RhittaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!isOwner(player)) {
            return;
        }

        if (!manager.hasRhitta(player)) {
            manager.giveRhitta(player);
        }

        makeRhittaUnbreakable(player);
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player) || !isOwner(player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        makeUnbreakable(item);

        // Life steal: 20% of damage dealt, capped at 4 HP
        double heal = Math.min(event.getFinalDamage() * 0.20, 4.0);
        double newHealth = Math.min(player.getHealth() + heal, getMaxHealth(player));
        player.setHealth(newHealth);

        // Defense grows every hit
        manager.addDefense(player);
    }

    /**
     * Anyone except the owner who picks up Rhitta is instantly killed
     * and the pickup is cancelled. The owner picking it up is safe.
     */
    @EventHandler
    public void onRhittaPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        Item itemEntity = event.getItem();
        ItemStack item = itemEntity.getItemStack();

        if (!manager.isRhitta(item)) {
            return;
        }

        if (!isOwner(player)) {
            event.setCancelled(true);
            player.setHealth(0.0);
            return;
        }

        makeUnbreakable(item);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!isOwner(player)) {
            return;
        }

        if (!manager.hasRhitta(player)) {
            return;
        }

        if (manager.hasUsedResurrection(player)) {
            return;
        }

        manager.markResurrectionUsed(player);

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> resurrect(player),
                2L
        );
    }

    private void resurrect(Player player) {
        if (!player.isOnline()) {
            return;
        }

        // Totem-style resurrection
        player.spigot().respawn();

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {
                    manager.giveRhitta(player);
                    makeRhittaUnbreakable(player);

                    // Strength III for 60 seconds (represents the 200% strength boost)
                    player.addPotionEffect(
                            new PotionEffect(
                                    PotionEffectType.STRENGTH,
                                    20 * 60,
                                    2,
                                    false,
                                    false,
                                    true
                            )
                    );

                    AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHealth != null) {
                        player.setHealth(maxHealth.getValue());
                    }
                },
                2L
        );
    }

    private boolean isOwner(Player player) {
        return player.getName().equalsIgnoreCase(OWNER);
    }

    private void makeRhittaUnbreakable(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (manager.isRhitta(item)) {
                makeUnbreakable(item);
            }
        }
    }

    private void makeUnbreakable(ItemStack item) {
        if (item == null || !manager.isRhitta(item)) {
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
        AttributeInstance attribute = player.getAttribute(Attribute.MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 20.0;
    }
    } 
