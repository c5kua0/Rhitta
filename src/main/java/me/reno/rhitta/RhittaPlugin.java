package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public class RhittaPlugin extends JavaPlugin {

    private RhittaManager rhittaManager;

    @Override
    public void onEnable() {

        rhittaManager = new RhittaManager(this);

        getServer().getPluginManager().registerEvents(
                new RhittaListener(this, rhittaManager),
                this
        );

        RhittaCommand command = new RhittaCommand(this);

        if (getCommand("rhitta") != null) {
            getCommand("rhitta").setExecutor(command);
        }

        getLogger().info("Rhitta has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rhitta has been disabled!");
    }

    public RhittaManager getRhittaManager() {
        return rhittaManager;
    }
}
