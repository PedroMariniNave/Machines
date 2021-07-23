package com.zpedroo.voltzmachines.tasks;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.MachineManager;
import org.bukkit.scheduler.BukkitRunnable;

import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class QuotationTask extends BukkitRunnable {

    public QuotationTask(VoltzMachines voltzMachines) {
        this.runTaskTimerAsynchronously(voltzMachines, 20 * 60L, 20 * 60L);
    }

    @Override
    public void run() {
        if (System.currentTimeMillis() < NEXT_UPDATE) return;

        MachineManager.getInstance().updatePrices(false);
    }
}