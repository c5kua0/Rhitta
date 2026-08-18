package me.reno.rhitta;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;
    private final RhittaManager manager;

    public RhittaListener(RhittaPlugin plugin, RhittaManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    // Give Rhitta when owner joins
    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!manager.isOwner(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            manager.giveRhitta(player);
        }, 1L);
    }

    // Life steal + defense growth + durability repair
    @EventHandler(priority = EventPriority.HIGH)
    public void onAttack(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (!manager.isOwner(player)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(weapon)) {
            return;
        }

        double damage = event.getFinalDamage();

        // Life steal: 20%
        double heal = Math.min(damage * 0.20, 6.0);

        double newHealth = Math.min(
                player.getHealth() + heal,
                player.getMaxHealth()
        );

        player.setHealth(newHealth);

        // Defense increases every successful hit
        manager.addDefense(player);

        // Repair Rhitta
        if (weapon.getType() != Material.AIR && weapon.hasItemMeta()) {
            weapon.setDurability((short) 0);
        }

        player.sendActionBar(
                "§6Rhitta §7| §cLife Steal: §f+" +
                String.format("%.1f", heal) +
                " §7| §bDefense: §f" +
                manager.getDefense(player)
        );
    }

    // Defense reduces incoming damage
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent event) {

        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!manager.isOwner(player)) {
            return;
        }

        int defense = manager.getDefense(player);

        if (defense <= 0) {
            return;
        }

        // Maximum 50% damage reduction
        double reduction = Math.min(defense * 0.01, 0.50);

        double damage = event.getDamage();
        event.setDamage(damage * (1.0 - reduction));
    }

    // One-time resurrection
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {

        Player player = event.getPlayer();

        if (!manager.isOwner(player)) {
            return;
        }

        // Keep Rhitta
        ItemStack rhitta = manager.createRhitta();

        event.getItemsToKeep().add(rhitta);

        event.getDrops().removeIf(manager::isRhitta);

        // Already used resurrection
        if (manager.hasUsedResurrection(player)) {
            return;
        }

        manager.markResurrectionUsed(player);

        // Cancel death and revive with full health
        event.setReviveHealth(player.getMaxHealth());
        event.setCancelled(true);

        player.sendMessage("§6§lRHITTA §7has resurrected you!");

        // Give resurrection buffs after event
        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            if (!player.isOnline()) {
                return;
            }

            player.setHealth(player.getMaxHealth());

            // Strength II for 60 seconds
            player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.STRENGTH,
                            20 * 60,
                            1,
                            false,
                            true,
                            true
                    )
            );

            // Resistance III for 60 seconds
            player.addPotionEffect(
                    new PotionEffect(
                            PotionEffectType.RESISTANCE,
                            20 * 60,
                            2,
                            false,
                            true,
                            true
                    )
            );

            manager.giveRhitta(player);

            player.sendMessage(
                    "§c§lRESURRECTION! §7Strength and Defense increased for §f60 seconds§7."
            );

        }, 2L);
    }

    // Make sure Rhitta returns after respawn
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        if (!manager.isOwner(player)) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            manager.giveRhitta(player);
        }, 1L);
    }

    // Prevent dropping Rhitta
    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {

        Player player = event.getPlayer();

        if (!manager.isOwner(player)) {
            return;
        }

        if (manager.isRhitta(event.getItemDrop().getItemStack())) {
            event.setCancelled(true);

            player.sendMessage(
                    "§6§lRHITTA §7cannot be dropped."
            );
        }
    }

    // Prevent moving Rhitta into containers
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!manager.isOwner(player)) {
            return;
        }

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (manager.isRhitta(current) || manager.isRhitta(cursor)) {

            if (event.getClickedInventory() != player.getInventory()) {
                event.setCancelled(true);
            }
        }
    }

    // Prevent dragging Rhitta into containers
    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!manager.isOwner(player)) {
            return;
        }

        if (manager.isRhitta(event.getOldCursor())) {
            event.setCancelled(true);
        }
    }
            } 
