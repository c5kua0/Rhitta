package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public class RhittaPlugin extends JavaPlugin {

    private RhittaManager rhittaManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.rhittaManager = new RhittaManager(this);

        RhittaListener listener = new RhittaListener(this, rhittaManager);
        getServer().getPluginManager().registerEvents(listener, this);

        RhittaCommand command = new RhittaCommand(this);
        getCommand("rhitta").setExecutor(command);

        // Sunrise / Noon buff scheduler
        listener.startBuffScheduler();

        getLogger().info("Rhitta enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rhitta disabled!");
    }

    public RhittaManager getRhittaManager() {
        return rhittaManager;
    }
}