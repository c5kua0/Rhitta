package me.reno.rhitta;

import org.bukkit.entity.Player;

public class RhittaManager {

    private final RhittaPlugin plugin;

    public RhittaManager(RhittaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isOwner(Player player) {
        // Owner system natin ilalagay dito next.
        return false;
    }
}
