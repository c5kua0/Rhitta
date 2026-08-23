package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public class RhittaPlugin extends JavaPlugin {

    private RhittaManager rhittaManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        rhittaManager = new RhittaManager(this);

        RhittaListener listener =
                new RhittaListener(this, rhittaManager);

        getServer().getPluginManager()
                .registerEvents(listener, this);

        RhittaCommand command =
                new RhittaCommand(this);

        if (getCommand("rhitta") != null) {
            getCommand("rhitta").setExecutor(command);
            getCommand("rhitta").setTabCompleter(command);
        }

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