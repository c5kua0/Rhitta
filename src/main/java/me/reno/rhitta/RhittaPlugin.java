package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public final class RhittaPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("Rhitta has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rhitta has been disabled!");
    }
}
