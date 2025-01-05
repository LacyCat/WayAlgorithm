package org.algorithm.wayAlgorithm;

import org.bukkit.plugin.java.JavaPlugin;
import org.algorithm.wayAlgorithm.CommandHandler;

public class WayAlgorithm extends JavaPlugin {

    private CommandHandler commandHandler;
    public String version = getDescription().getVersion();
    @Override
    public void onEnable() {
        getLogger().info("WayAlgorithm plugin has been enabled!");
        commandHandler = new CommandHandler(this);
        getCommand("set1").setExecutor(commandHandler);
        getCommand("set2").setExecutor(commandHandler);
        getCommand("calc").setExecutor(commandHandler);
        getCommand("reset").setExecutor(commandHandler);
    }

    @Override
    public void onDisable() {
        getLogger().info("WayAlgorithm plugin has been disabled!");
    }
}
