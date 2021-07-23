package com.zpedroo.voltzmachines.tasks;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.utils.config.Settings;
import org.bukkit.Sound;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.xenondevs.particle.ParticleEffect;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Random;

public class MachineTask extends BukkitRunnable {

    public MachineTask(VoltzMachines voltzMachines) {
        this.runTaskTimerAsynchronously(voltzMachines, 20 * 30L, Settings.MACHINE_UPDATE);
    }

    @Override
    public void run() {
        new HashSet<>(MachineManager.getInstance().getDataCache().getPlayerMachines().values()).forEach(machine -> {
            if (machine == null || !machine.isEnabled()) return;

            int delay = machine.getDelay() - Settings.MACHINE_UPDATE;
            machine.setDelay(delay);

            if (delay >= 0) return;

            BigInteger drops = null;

            if (machine.getFuel().compareTo(machine.getStack()) >= 0) {
                drops = machine.getMachine().getAmount().multiply(machine.getStack());
            } else {
                drops = machine.getMachine().getAmount().multiply(machine.getStack().subtract(machine.getFuel()));
            }

            if (machine.getIntegrity() <= 70) { // low efficiency
                Integer toDivide = (100 - machine.getIntegrity()) / 10;

                if (toDivide >= 2) drops = drops.divide(BigInteger.valueOf(toDivide));
            }

            if (drops.signum() <= 0) drops = BigInteger.ONE;

            BigInteger fuel = drops.divide(BigInteger.TEN); // 10 stacks = 1L

            machine.addDrops(drops);
            machine.removeFuel(fuel.signum() <= 0 ? BigInteger.ONE : fuel);

            if (machine.getFuel().signum() <= 0) {
                machine.switchStatus();
            }

            machine.updateDelay();
            machine.getLocation().getWorld().playSound(machine.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);

            double y = 1D;
            double z = 0.6D;

            while (y < 2.25D) {
                ParticleEffect.SMOKE_LARGE.display(machine.getLocation().clone().add(0.5D, y = y + 0.1D, z = z + 0.05D));
            }

            Random random = new Random();

            if (random.nextInt(100 + 1) <= 25) {
                machine.setIntegrity(machine.getIntegrity() - 1);
            }

            if (machine.getIntegrity() <= 60) { // 60% of integrity = chance of stop machine and lost all drops
                if (random.nextInt(100 + 1) <= 5) {
                    machine.switchStatus();
                    machine.setDrops(BigInteger.ZERO);
                    machine.getLocation().getWorld().playSound(machine.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 10f, 10f);

                    ParticleEffect.EXPLOSION_HUGE.display(machine.getLocation().clone().add(0.5D, 0D, 0.5D));
                }
            }
        });
    }
}