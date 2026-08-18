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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RhittaListener implements Listener {

    private final RhittaPlugin plugin;

    public RhittaListener(RhittaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {

        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }

        RhittaManager manager = plugin.getRhittaManager();

        if (!manager.isOwner(player)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(weapon)) {
            return;
        }

        double heal = Math.min(event.getFinalDamage() * 0.35, 8.0);

        AttributeInstance maxHealth =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (maxHealth != null) {
            player.setHealth(
                    Math.min(
                            player.getHealth() + heal,
                            maxHealth.getValue()
                    )
            );
        }

        manager.addDefense(player, 1);
        manager.repairRhitta(weapon, 1);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {

        Player player = event.getEntity();
        RhittaManager manager = plugin.getRhittaManager();

        if (!manager.isOwner(player)) {
            return;
        }

        if (!manager.hasResurrection(player)) {
            return;
        }

        manager.prepareResurrection(player);

        event.setKeepInventory(true);
        event.getDrops().removeIf(manager::isRhitta);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {

        Player player = event.getPlayer();
        RhittaManager manager = plugin.getRhittaManager();

        if (!manager.isOwner(player)) {
            return;
        }

        if (!manager.isResurrectionPending(player)) {
            return;
        }

        manager.completeResurrection(player);

        // 200% Strength for 1 minute
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
