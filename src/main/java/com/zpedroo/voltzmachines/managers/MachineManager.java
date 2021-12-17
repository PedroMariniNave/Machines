package com.zpedroo.voltzmachines.managers;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlacedMachine;
import com.zpedroo.voltzmachines.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zpedroo.voltzmachines.utils.config.Messages.*;
import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class MachineManager {

    public static void clearAll() {
        DataManager.getInstance().getCache().getPlacedMachines().values().stream().filter(placedMachine ->
                placedMachine.getHologram() != null).forEach(machine -> machine.getHologram().removeHologramAndItem());
    }

    public static void updatePrices(boolean forced) {
        for (Machine machine : DataManager.getInstance().getCache().getMachines().values()) {
            updatePrice(machine);
        }

        for (String msg : NEW_QUOTATION) {
            if (msg == null) break;

            Bukkit.broadcastMessage(msg);
        }

        if (forced) return;

        long nextUpdate = NEXT_UPDATE + TimeUnit.HOURS.toMillis(24);
        if (NEXT_UPDATE == 0) nextUpdate = getEndOfDay().getTime();

        FileUtils.get().getFile(FileUtils.Files.CONFIG).get().set("Settings.next-update", nextUpdate);
        FileUtils.get().getFile(FileUtils.Files.CONFIG).save();

        NEXT_UPDATE = nextUpdate;
    }

    public static void updatePrice(Machine machine) {
        File folder = new File(VoltzMachines.get().getDataFolder(), "/machines");
        File[] files = folder.listFiles((file, name) -> name.equals(machine.getType() + ".yml"));

        if (files == null || files.length <= 0) return;

        File file = files[0];
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);

        double minPrice = machine.getDropsMinimumValue().doubleValue();
        double maxPrice = machine.getDropsMaximumValue().doubleValue();

        BigInteger newPrice = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(minPrice, maxPrice)));

        try {
            fileConfig.set("Machine-Settings.drops.price", newPrice.toString());
            fileConfig.set("Machine-Settings.drops.previous", machine.getDropsValue().toString());
            fileConfig.save(file);
        } catch (Exception ex) {
            // ignore
        }

        machine.setDropsPreviousValue(machine.getDropsValue());
        machine.setDropsValue(newPrice);
    }

    public static Object[] getNearMachines(Player player, Block block, BigInteger addAmount, String type) {
        int radius = STACK_RADIUS;
        if (radius <= 0) return null;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block blocks = block.getRelative(x, y, z);
                    if (blocks.getType().equals(Material.AIR)) continue;

                    PlacedMachine machine = DataManager.getInstance().getPlacedMachine(blocks.getLocation());
                    if (machine == null) continue;
                    if (!StringUtils.equals(type, machine.getMachine().getType())) continue;
                    if (machine.hasReachStackLimit()) continue;
                    if (!machine.canInteract(player)) continue;

                    BigInteger overLimit = BigInteger.ZERO;
                    if (machine.getMachine().getMaxStack().signum() > 0 && machine.getStack().add(addAmount).compareTo(machine.getMachine().getMaxStack()) > 0) {
                        overLimit = machine.getStack().add(addAmount).subtract(machine.getMachine().getMaxStack());
                    }

                    return new Object[] { machine, overLimit };
                }
            }
        }

        return null;
    }

    private static Date getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return calendar.getTime();
    }
}
