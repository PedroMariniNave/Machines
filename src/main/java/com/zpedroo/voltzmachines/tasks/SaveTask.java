package com.zpedroo.voltzmachines.tasks;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.utils.config.Settings;
import org.bukkit.scheduler.BukkitRunnable;

public class SaveTask extends BukkitRunnable {

    public SaveTask(VoltzMachines voltzMachines) {
        this.runTaskTimerAsynchronously(voltzMachines, 20 * Settings.SAVE_INTERVAL, 20 * Settings.SAVE_INTERVAL);
    }

    @Override
    public void run() {
        MachineManager.getInstance().saveAll();
    }
}