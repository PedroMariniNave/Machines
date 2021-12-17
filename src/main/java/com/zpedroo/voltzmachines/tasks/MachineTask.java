package com.zpedroo.voltzmachines.tasks;

import com.zpedroo.voltzmachines.managers.DataManager;
import org.bukkit.Sound;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import xyz.xenondevs.particle.ParticleEffect;

import java.math.BigInteger;
import java.util.Random;

import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class MachineTask extends BukkitRunnable {

    public MachineTask(Plugin plugin) {
        this.runTaskTimer(plugin, MACHINE_UPDATE, MACHINE_UPDATE);
    }

    @Override
    public void run() {
        DataManager.getInstance().getCache().getPlacedMachines().values().stream().filter(placedMachine ->
                placedMachine != null && placedMachine.isEnabled()).forEach(machine -> {

            int delay = machine.getDelay() - MACHINE_UPDATE;
            machine.setDelay(delay);

            if (delay >= 0) return;

            BigInteger drops = null;

            if (machine.hasInfiniteFuel() || machine.getFuel().compareTo(machine.getStack()) >= 0) {
                drops = machine.getMachine().getDropsAmount().multiply(machine.getStack());
            } else {
                drops = machine.getMachine().getDropsAmount().multiply(machine.getFuel()).multiply(BigInteger.TEN);
            }

            if (!machine.hasInfiniteIntegrity()) {
                if (machine.getIntegrity().intValue() <= 70) { // low efficiency
                    int toDivide = (100 - machine.getIntegrity().intValue()) / 10;

                    if (toDivide >= 2) drops = drops.divide(BigInteger.valueOf(toDivide));
                }

                Random random = new Random();

                if (random.nextInt(100 + 1) <= 55) {
                    machine.setIntegrity(machine.getIntegrity().subtract(BigInteger.ONE));
                }

                if (machine.getIntegrity().intValue() <= 60) { // 60% of integrity = chance of stop machine and lost all drops
                    if (random.nextInt(100 + 1) <= 5) {
                        machine.switchStatus();
                        machine.setDrops(BigInteger.ZERO);
                        machine.getLocation().getWorld().playSound(machine.getLocation(), Sound.EXPLODE, 10f, 10f);

                        ParticleEffect.EXPLOSION_HUGE.display(machine.getLocation().clone().add(0.5D, 0D, 0.5D));
                    }
                }
            }

            if (drops.signum() <= 0) drops = BigInteger.ONE;

            machine.addDrops(drops);

            if (!machine.hasInfiniteFuel()) {
                BigInteger fuel = machine.getStack().divide(BigInteger.TEN); // 10 stacks = 1L
                machine.removeFuel(fuel.signum() <= 0 ? BigInteger.ONE : fuel);

                if (machine.getFuel().signum() <= 0) {
                    machine.switchStatus();
                }
            }

            machine.updateDelay();
            machine.getLocation().getWorld().playSound(machine.getLocation(), Sound.FIRE_IGNITE, 0.5f, 0.5f);

            double y = 1D;
            double z = 0.6D;

            while (y < 2.25D) {
                ParticleEffect.SMOKE_LARGE.display(machine.getLocation().clone().add(0.5D, y = y + 0.1D, z = z + 0.05D));
            }
        });
    }
}