package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public class RhittaPlugin extends JavaPlugin {

    private RhittaManager rhittaManager;

    @Override
    public void onEnable() {

        rhittaManager =
                new RhittaManager(this);

        getServer()
                .getPluginManager()
                .registerEvents(
                        new RhittaListener(
                                this,
                                rhittaManager
                        ),
                        this
                );

        RhittaCommand command =
                new RhittaCommand(this);

        if (getCommand("rhitta") != null) {

            getCommand("rhitta")
                    .setExecutor(command);
        }

        getLogger().info(
                "Rhitta enabled!"
        );
    }

    @Override
    public void onDisable() {

        getLogger().info(
                "Rhitta disabled!"
        );
    }

    public RhittaManager getRhittaManager() {
        return rhittaManager;
    }
}