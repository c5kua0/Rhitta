package me.reno.rhitta;

import org.bukkit.Attribute;
import org.bukkit.EntityEffect;
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

        if (!(event.getEntity() instanceof LivingEntity target)) {
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

        // LIFE STEAL
        double heal = Math.min(event.getFinalDamage() * 0.35, 8.0);

        double maxHealth = player.getAttribute(
                Attribute.MAX_HEALTH
        ).getValue();

        double newHealth = Math.min(
                player.getHealth() + heal,
                maxHealth
        );

        player.setHealth(newHealth);

        // DEFENSE GROWTH
        manager.addDefense(player, 1);

        // DURABILITY REPAIR
        manager.repairRhitta(weapon, 1);

        // HIT EFFECT
        target.playEffect(EntityEffect.HURT);
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

        // REMOVE RHITTA FROM DROPS
        event.getDrops().removeIf(manager::isRhitta);

        event.setKeepInventory(true);
        event.getDrops().clear();
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
