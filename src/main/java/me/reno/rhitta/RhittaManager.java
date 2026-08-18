package me.reno.rhitta;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RhittaManager {

    private final RhittaPlugin plugin;
    private final NamespacedKey rhittaKey;

    private final Set<UUID> resurrectionUsed = new HashSet<>();
    private final Map<UUID, Integer> defense = new HashMap<>();

    private UUID owner;

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;
        this.rhittaKey = new NamespacedKey(plugin, "rhitta");
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

    public void markAsRhitta(ItemStack item) {
        if (item == null) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(
                rhittaKey,
                PersistentDataType.BYTE,
                (byte) 1
        );
        item.setItemMeta(meta);
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

    public void giveRhitta(Player player) {
        if (player == null) {
            return;
        }
        if (hasRhitta(player)) {
            return;
        }
        ItemStack rhitta = createRhitta();
        player.getInventory().addItem(rhitta);

        owner = player.getUniqueId();
    }

    public boolean hasRhitta(Player player) {
        if (player == null) {
            return false;
        }
        for (ItemStack item : player.getInventory().getContents()) {
            if (isRhitta(item)) {
                return true;
            }
        }
        return false;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setOwner(UUID ownerUuid) {
        this.owner = ownerUuid;
    }

    public boolean isOwner(Player player) {
        if (player == null || owner == null) {
            return false;
        }
        return owner.equals(player.getUniqueId());
    }

    public int getDefense(Player player) {
        if (player == null) {
            return 0;
        }
        return defense.getOrDefault(player.getUniqueId(), 0);
    }

    public void addDefense(Player player) {
        if (player == null) {
            return;
        }
        UUID uuid = player.getUniqueId();
        int current = defense.getOrDefault(uuid, 0);
        defense.put(uuid, current + 1);
    }

    public boolean hasUsedResurrection(Player player) {
        if (player == null) {
            return false;
        }
        return resurrectionUsed.contains(player.getUniqueId());
    }

    public void markResurrectionUsed(Player player) {
        if (player == null) {
            return;
        }
        resurrectionUsed.add(player.getUniqueId());
    }

    public void resetResurrection(Player player) {
        if (player == null) {
            return;
        }
        resurrectionUsed.remove(player.getUniqueId());
    }
}
