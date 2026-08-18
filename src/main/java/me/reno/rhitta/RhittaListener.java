package me.reno.rhitta;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

public class RhittaListener implements Listener {

    private final RhittaManager manager;

    public RhittaListener(RhittaManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        Entity entity = event.getEntity();

        if (!(entity instanceof LivingEntity target)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        // Life steal
        double damage = event.getFinalDamage();
        double heal = Math.min(damage * 0.25, 4.0);

        double newHealth = Math.min(
                player.getHealth() + heal,
                getMaxHealth(player)
        );

        player.setHealth(newHealth);

        // Defense growth
        manager.addDefense(player);

        // Prevent Rhitta from losing durability
        if (item.getType().getMaxDurability() > 0) {
            item.setDurability((short) 0);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();

        if (!manager.isOwner(player)) {
            return;
        }

        ItemStack rhitta = null;

        for (ItemStack item : player.getInventory().getContents()) {
            if (manager.isRhitta(item)) {
                rhitta = item;
                break;
            }
        }

        // Keep Rhitta from dropping
        if (rhitta != null) {
            event.getDrops().removeIf(manager::isRhitta);
        }

        // One-time resurrection
        if (manager.canResurrect(player)) {
            event.setKeepInventory(true);
            event.getDrops().clear();

            manager.prepareResurrection(player);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();

        if (!manager.isResurrectionPending(player)) {
            return;
        }

        manager.resurrect(player);

        player.sendMessage(
                ChatColor.GOLD + "✦ RHITTA HAS CHOSEN YOU ✦"
        );

        player.sendMessage(
                ChatColor.RED + "Resurrection activated!"
        );

        player.sendMessage(
                ChatColor.RED + "+200% Strength"
                + ChatColor.GRAY + " | "
                + ChatColor.BLUE + "+200% Defense"
        );
    }

    private double getMaxHealth(Player player) {

        AttributeInstance attribute =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (attribute == null) {
            return 20.0;
        }

        return attribute.getValue();
    }
            }                        20 * 60,
                        2,
                        false,
                        true,
                        true
                )
        );

        // 200% Defense for 1 minute
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
    }
    }
        if (!manager.isOwner(player)) {
            return;
        }

        if (!manager.isResurrectionPending(player)) {
            return;
        }

        manager.completeResurrection(player);

        // 200% STRENGTH
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.STRENGTH,
                        20 * 60,
                        2,
                        false,
                        true,
                        true
                )
        );

        // 200% DEFENSE
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
    }
    }
