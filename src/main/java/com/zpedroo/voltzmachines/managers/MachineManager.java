package com.zpedroo.voltzmachines.managers;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.hooks.WorldGuardHook;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlayerMachine;
import com.zpedroo.voltzmachines.utils.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zpedroo.voltzmachines.utils.config.Messages.*;
import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class MachineManager {

    private static MachineManager instance;
    public static MachineManager getInstance() { return instance; }

    public MachineManager() {
        instance = this;
    }

    public void clearAll() {
        new HashSet<>(DataManager.getInstance().getCache().getPlayerMachines().values()).forEach(machine -> {
            machine.getHologram().remove();
        });
    }

    public void updatePrices(Boolean forced) {
        for (Machine machine : DataManager.getInstance().getCache().getMachines().values()) {
            File folder = new File(VoltzMachines.get().getDataFolder(), "/machines");
            File[] files = folder.listFiles((file, name) -> name.equals(machine.getType() + ".yml"));

            if (files == null || files.length <= 0) return;

            File file = files[0];
            FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(file);
            BigInteger newValue = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(MIN_PRICE.doubleValue(), MAX_PRICE.doubleValue())));

            try {
                fileConfig.set("Machine-Settings.drops.price", newValue.longValue());
                fileConfig.set("Machine-Settings.drops.previous", machine.getDropsValue().longValue());
                fileConfig.save(file);
            } catch (Exception ex) {
                // ignore
            }

            machine.setDropsPreviousValue(machine.getDropsValue());
            machine.setDropsValue(newValue);
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

    public Object[] getNearMachines(Player player, Block block, BigInteger addAmount, String type) {
        int radius = STACK_RADIUS;
        if (radius <= 0) return null;

        int initialX = block.getX(), initialY = block.getY(), initialZ = block.getZ();

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    if (initialX == x && initialY == y && initialZ == z) continue;
                    if (!WorldGuardHook.getInstance().canBuild(player, new Location(block.getWorld(), x, y, z))) continue;

                    Block blocks = block.getRelative(x, y, z);
                    if (blocks.getType().equals(Material.AIR)) continue;

                    PlayerMachine machine = DataManager.getInstance().getMachine(blocks.getLocation());
                    if (machine == null) continue;
                    if (!StringUtils.equals(type, machine.getMachine().getType())) continue;
                    if (machine.hasReachStackLimit()) continue;

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

    private Date getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DATE);
        calendar.set(year, month, day, 23, 59, 59);
        return calendar.getTime();
    }
}
