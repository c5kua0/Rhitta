package me.reno.rhitta;

import org.bukkit.plugin.java.JavaPlugin;

public class RhittaPlugin extends JavaPlugin {

    private RhittaManager rhittaManager;

    @Override
    public void onEnable() {

        // ========================================================
        // CONFIG
        // ========================================================

        saveDefaultConfig();

        // ========================================================
        // MANAGER
        // ========================================================

        rhittaManager = new RhittaManager(this);

        // ========================================================
        // LISTENER
        // ========================================================

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

        // ========================================================
        // COMMAND
        // ========================================================

        RhittaCommand command =
                new RhittaCommand(this);

        if (getCommand("rhitta") != null) {

            getCommand("rhitta")
                    .setExecutor(command);

            getCommand("rhitta")
                    .setTabCompleter(command);

        } else {

            getLogger().severe(
                    "Command 'rhitta' is missing from plugin.yml!"
            );
        }

        // ========================================================
        // ENABLE MESSAGE
        // ========================================================

        getLogger().info(
                "================================="
        );

        getLogger().info(
                "Rhitta 2.0 enabled!"
        );

        getLogger().info(
                "Owner: " +
                RhittaManager.OWNER_NAME
        );

        getLogger().info(
                "================================="
        );
    }

    // ============================================================
    // DISABLE
    // ============================================================

    @Override
    public void onDisable() {

        getLogger().info(
                "Rhitta 2.0 disabled!"
        );
    }

    // ============================================================
    // MANAGER GETTER
    // ============================================================

    public RhittaManager getRhittaManager() {

        return rhittaManager;
    }
}