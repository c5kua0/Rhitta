package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public class RhittaPlugin extends JavaPlugin {

    private RhittaManager manager;

    @Override
    public void onEnable() {

        manager = new RhittaManager(this);

        getServer().getPluginManager().registerEvents(
                new RhittaListener(this, manager),
                this
        );

        RhittaCommand command = new RhittaCommand(manager);

        if (getCommand("rhitta") != null) {
            getCommand("rhitta").setExecutor(command);
            getCommand("rhitta").setTabCompleter(command);
        }

        getLogger().info("Rhitta has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Rhitta has been disabled!");
    }

    public RhittaManager getManager() {
        return manager;
    }
}
