package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public class RhittaPlugin extends JavaPlugin {

    private RhittaManager rhittaManager;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        rhittaManager = new RhittaManager(this);

        RhittaListener listener =
                new RhittaListener(
                        this,
                        rhittaManager
                );

        getServer()
                .getPluginManager()
                .registerEvents(
                        listener,
                        this
                );

        RhittaCommand command =
                new RhittaCommand(this);

        if (getCommand("rhitta") != null) {

            getCommand("rhitta")
                    .setExecutor(command);

            // Only add setTabCompleter here if
            // RhittaCommand implements TabCompleter.
        }

        getLogger().info(
                "Rhitta 2.0 enabled!"
        );
    }

    @Override
    public void onDisable() {

        getLogger().info(
                "Rhitta 2.0 disabled!"
        );
    }

    public RhittaManager getRhittaManager() {
        return rhittaManager;
    }
}