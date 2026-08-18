package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public final class RhittaPlugin extends JavaPlugin {

    private RhittaManager rhittaManager;

    @Override
    public void onEnable() {
        rhittaManager = new RhittaManager(this);

        getServer().getPluginManager().registerEvents(
                new RhittaListener(this),
                this
        );

        if (getCommand("rhitta") != null) {
            getCommand("rhitta").setExecutor(new RhittaCommand(this));
        }

        getLogger().info("Rhitta has awakened!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rhitta has been sealed.");
    }

    public RhittaManager getRhittaManager() {
        return rhittaManager;
    }
}
