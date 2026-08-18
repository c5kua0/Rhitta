package me.reno.rhitta;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
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
        if (!(event.getDamager() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getDamager();

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        double heal = event.getFinalDamage() * 0.25;

        AttributeInstance health =
                player.getAttribute(Attribute.MAX_HEALTH);

        double maxHealth = 20.0;

        if (health != null) {
            maxHealth = health.getValue();
        }

        player.setHealth(
                Math.min(player.getHealth() + heal, maxHealth)
        );

        manager.addDefense(player);

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

        event.getDrops().removeIf(manager::isRhitta);

        if (manager.canResurrect(player)) {
            manager.prepareResurrection(player);
            event.setKeepInventory(true);
            event.getDrops().clear();
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        if (!manager.isResurrectionPending(player)) {
            return;
        }

        manager.resurrect(player);
    }
}            return;
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
