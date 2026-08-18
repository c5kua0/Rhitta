package me.reno.rhitta;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
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

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!manager.hasRhitta(player)) {
            manager.giveRhitta(player);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();

        if (!manager.isRhitta(item)) {
            return;
        }

        // Life steal
        double heal = Math.min(event.getFinalDamage() * 0.20, 4.0);
        double newHealth = Math.min(
                player.getHealth() + heal,
                getMaxHealth(player)
        );

        player.setHealth(newHealth);

        // Defense grows every hit
        manager.addDefense(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

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

        player.spigot().respawn();

        plugin.getServer().getScheduler().runTaskLater(
                plugin,
                () -> {

                    manager.giveRhitta(player);

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

                    AttributeInstance maxHealth =
                            player.getAttribute(Attribute.MAX_HEALTH);

                    if (maxHealth != null) {
                        player.setHealth(maxHealth.getValue());
                    }

                },
                2L
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
}
