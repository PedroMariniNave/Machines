package com.zpedroo.voltzmachines.tasks;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.DataManager;
import org.bukkit.scheduler.BukkitRunnable;

import static com.zpedroo.voltzmachines.utils.config.Settings.SAVE_INTERVAL;

public class SaveTask extends BukkitRunnable {

    public SaveTask(VoltzMachines voltzMachines) {
        this.runTaskTimerAsynchronously(voltzMachines, 20 * SAVE_INTERVAL, 20 * SAVE_INTERVAL);
    }

    @Override
    public void run() {
        DataManager.getInstance().saveAll();
        DataManager.getInstance().updateTopMachines();
    }
}