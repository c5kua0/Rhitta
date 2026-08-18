package me.reno.rhitta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class RhittaManager {

    private final JavaPlugin plugin;

    private final Map<UUID, Integer> defenseLevels = new HashMap<>();
    private final Map<UUID, Boolean> resurrectionUsed = new HashMap<>();
    private final Map<UUID, Boolean> resurrectionPending = new HashMap<>();

    private final NamespacedKey rhittaKey;

    public RhittaManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.rhittaKey = new NamespacedKey(plugin, "rhitta_weapon");
    }

    public ItemStack createRhitta() {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§lRHITTA");

            meta.getPersistentDataContainer().set(
                    rhittaKey,
                    PersistentDataType.BYTE,
                    (byte) 1
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isRhitta(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return false;
        }

        Byte value = meta.getPersistentDataContainer().get(
                rhittaKey,
                PersistentDataType.BYTE
        );

        return value != null && value == (byte) 1;
    }

    public boolean isOwner(Player player) {
        return plugin.getConfig()
                .getString("owner")
                .equalsIgnoreCase(player.getName());
    }

    public void addDefense(Player player) {
        UUID uuid = player.getUniqueId();

        int level = defenseLevels.getOrDefault(uuid, 0);
        level++;

        defenseLevels.put(uuid, level);

        AttributeInstance armor =
                player.getAttribute(Attribute.ARMOR);

        if (armor != null) {
            armor.setBaseValue(10.0 + level);
        }
    }

    public boolean canResurrect(Player player) {
        UUID uuid = player.getUniqueId();

        return !resurrectionUsed.getOrDefault(uuid, false);
    }

    public void prepareResurrection(Player player) {
        UUID uuid = player.getUniqueId();

        resurrectionUsed.put(uuid, true);
        resurrectionPending.put(uuid, true);
    }

    public boolean isResurrectionPending(Player player) {
        return resurrectionPending.getOrDefault(
                player.getUniqueId(),
                false
        );
    }

    public void resurrect(Player player) {
        UUID uuid = player.getUniqueId();

        resurrectionPending.remove(uuid);

        player.setHealth(getMaxHealth(player));

        AttributeInstance attack =
                player.getAttribute(Attribute.ATTACK_DAMAGE);

        AttributeInstance armor =
                player.getAttribute(Attribute.ARMOR);

        if (attack != null) {
            double original = attack.getBaseValue();

            attack.setBaseValue(original * 3.0);

            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> attack.setBaseValue(original),
                    20L * 60L
            );
        }

        if (armor != null) {
            double original = armor.getBaseValue();

            armor.setBaseValue(original * 3.0);

            plugin.getServer().getScheduler().runTaskLater(
                    plugin,
                    () -> armor.setBaseValue(original),
                    20L * 60L
            );
        }
    }

    private double getMaxHealth(Player player) {
        AttributeInstance health =
                player.getAttribute(Attribute.MAX_HEALTH);

        if (health == null) {
            return 20.0;
        }

        return health.getValue();
    }
    }
        attribute.setBaseValue(newValue);
    }

    public void repairRhitta(ItemStack item, int amount) {

        if (!isRhitta(item)) {
            return;
        }

        ItemMeta meta = item.getItemMeta();

        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int currentDamage = damageable.getDamage();

        damageable.setDamage(
                Math.max(0, currentDamage - amount)
        );

        item.setItemMeta(damageable);
    }

    public boolean hasResurrection(Player player) {
        return !resurrectionUsed.contains(player.getUniqueId());
    }

    public void prepareResurrection(Player player) {

        UUID uuid = player.getUniqueId();

        resurrectionUsed.add(uuid);
        resurrectionPending.add(uuid);
    }

    public boolean isResurrectionPending(Player player) {
        return resurrectionPending.contains(player.getUniqueId());
    }

    public void completeResurrection(Player player) {
        resurrectionPending.remove(player.getUniqueId());
    }

    public void resetResurrection(Player player) {

        UUID uuid = player.getUniqueId();

        resurrectionUsed.remove(uuid);
        resurrectionPending.remove(uuid);
    }

    public NamespacedKey getRhittaKey() {
        return rhittaKey;
    }

    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }
            }        if (meta == null) {
            return;
        }

        if (!meta.isUnbreakable()) {
            int newDamage = Math.max(
                    0,
                    meta.getDamage() - amount
            );

            meta.setDamage(newDamage);
            item.setItemMeta(meta);
        }
    }

    public boolean hasResurrection(Player player) {
        return !resurrectionUsed.contains(player.getUniqueId());
    }

    public void prepareResurrection(Player player) {
        UUID uuid = player.getUniqueId();

        resurrectionUsed.add(uuid);
        resurrectionPending.add(uuid);
    }

    public boolean isResurrectionPending(Player player) {
        return resurrectionPending.contains(player.getUniqueId());
    }

    public void completeResurrection(Player player) {
        resurrectionPending.remove(player.getUniqueId());
    }

    public void resetResurrection(Player player) {
        resurrectionUsed.remove(player.getUniqueId());
        resurrectionPending.remove(player.getUniqueId());
    }

    public NamespacedKey getRhittaKey() {
        return rhittaKey;
    }

    public NamespacedKey getOwnerKey() {
        return ownerKey;
    }
}
