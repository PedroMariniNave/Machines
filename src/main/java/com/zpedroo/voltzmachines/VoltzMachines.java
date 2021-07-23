package com.zpedroo.voltzmachines;

import com.zpedroo.voltzmachines.commands.MachinesCmd;
import com.zpedroo.voltzmachines.hooks.ProtocolLibHook;
import com.zpedroo.voltzmachines.hooks.VaultHook;
import com.zpedroo.voltzmachines.hooks.WorldGuardHook;
import com.zpedroo.voltzmachines.listeners.MachineListeners;
import com.zpedroo.voltzmachines.listeners.PlayerChatListener;
import com.zpedroo.voltzmachines.listeners.PlayerGeneralListeners;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.mysql.DBConnection;
import com.zpedroo.voltzmachines.tasks.MachineTask;
import com.zpedroo.voltzmachines.tasks.QuotationTask;
import com.zpedroo.voltzmachines.tasks.SaveTask;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import com.zpedroo.voltzmachines.utils.item.Items;
import com.zpedroo.voltzmachines.utils.menu.Menus;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class VoltzMachines extends JavaPlugin {

    private static VoltzMachines instance;
    public static VoltzMachines get() { return instance; }

    public void onEnable() {
        instance = this;
        new FileUtils(this);

        if (!isMySQLEnabled(getConfig())) {
            getLogger().log(Level.SEVERE, "MySQL are disabled! You need to enable it.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        new DBConnection(getConfig());
        new VaultHook().hook();
        new WorldGuardHook();
        new ProtocolLibHook();
        new MachineTask(this);
        new SaveTask(this);
        new QuotationTask(this);
        new Menus();
        new Items();
        new NumberFormatter(getConfig());

        registerCommands();
        registerListeners();
    }

    public void onDisable() {
        if (!isMySQLEnabled(getConfig())) return;

        try {
            MachineManager.getInstance().saveAll();
            DBConnection.getInstance().closeConnection();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "An error ocurred while trying to save data!");
            ex.printStackTrace();
        }
    }

    private void registerCommands() {
        getCommand("machines").setExecutor(new MachinesCmd());
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new MachineListeners(), this);
        getServer().getPluginManager().registerEvents(new PlayerChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerGeneralListeners(), this);
    }

    private Boolean isMySQLEnabled(FileConfiguration file) {
        if (!file.contains("MySQL.enabled")) return false;

        return file.getBoolean("MySQL.enabled");
    }
}