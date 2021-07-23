package com.zpedroo.voltzmachines.managers;

import com.zpedroo.voltzmachines.FileUtils;
import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.hooks.WorldGuardHook;
import com.zpedroo.voltzmachines.machine.Machine;
import com.zpedroo.voltzmachines.machine.PlayerMachine;
import com.zpedroo.voltzmachines.machine.cache.MachineCacheData;
import com.zpedroo.voltzmachines.mysql.DBConnection;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.utils.builder.ItemBuilder;
import com.zpedroo.voltzmachines.utils.enums.Permission;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static com.zpedroo.voltzmachines.utils.config.Messages.*;
import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class MachineManager {

    private static MachineManager instance;
    public static MachineManager getInstance() { return instance; }

    private MachineCacheData machineCacheData;

    public MachineManager() {
        instance = this;
        this.machineCacheData = new MachineCacheData();
        this.loadConfigMachines();
        new BukkitRunnable() {
            @Override
            public void run() {
                for (World world : Bukkit.getWorlds()) {
                    for (Entity entity : world.getEntities()) {
                        if (!entity.getType().equals(EntityType.DROPPED_ITEM)) continue;
                        if (!StringUtils.equals(entity.getName(), "Machine Item")) continue;

                        entity.remove();
                    }
                }

                loadPlacedMachines();
            }
        }.runTaskLaterAsynchronously(VoltzMachines.get(), 100L);
    }

    private void loadConfigMachines() {
        File folder = new File(VoltzMachines.get().getDataFolder(), "/machines");
        File[] files = folder.listFiles((d, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File fl : files) {
            if (fl == null) continue;

            FileConfiguration file = YamlConfiguration.loadConfiguration(fl);

            ItemStack item = ItemBuilder.build(file, "Machine-Settings.item").build();
            Material block = Material.valueOf(file.getString("Machine-Settings.machine-block"));
            String type = fl.getName().replace(".yml", "");
            String typeTranslated = file.getString("Machine-Settings.type-translated");
            String displayName = ChatColor.translateAlternateColorCodes('&', file.getString("Machine-Settings.display-name"));
            Integer delay = file.getInt("Machine-Settings.drops.delay");
            BigInteger amount = new BigInteger(file.getString("Machine-Settings.drops.amount"));
            BigInteger dropsValue = new BigInteger(file.getString("Machine-Settings.drops.price"));
            BigInteger dropsPreviousValue = new BigInteger(file.getString("Machine-Settings.drops.previous"));
            BigInteger maxStack = new BigInteger(file.getString("Machine-Settings.max-stack"));
            String permission = file.getString("Machine-Settings.place-permission", "NULL");
            List<String> commands = file.getStringList("Machine-Settings.commands");

            if (dropsValue.signum() <= 0) {
                dropsValue = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(MIN_PRICE.doubleValue(), MAX_PRICE.doubleValue())));
                try {
                    file.set("Machine-Settings.drops.price", dropsValue.longValue());
                    file.save(fl);
                } catch (Exception ex) {
                    // ignore
                }
            }

            cache(new Machine(item, block, type, typeTranslated, displayName, delay, amount, dropsValue, dropsPreviousValue, maxStack, permission, commands));
        }
    }

    public void updatePrices(Boolean forced) {
        for (Machine machine : getDataCache().getMachines().values()) {
            File folder = new File(VoltzMachines.get().getDataFolder(), "/machines");
            File[] files = folder.listFiles((d, name) -> name.equals(machine.getType() + ".yml"));

            if (files == null) return;

            YamlConfiguration yamlConfig = YamlConfiguration.loadConfiguration(files[0]);
            BigInteger newValue = new BigInteger(String.format("%.0f", ThreadLocalRandom.current().nextDouble(MIN_PRICE.doubleValue(), MAX_PRICE.doubleValue())));

            try {
                yamlConfig.set("Machine-Settings.drops.price", newValue.longValue());
                yamlConfig.set("Machine-Settings.drops.previous", machine.getDropsValue().longValue());
                yamlConfig.save(files[0]);
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

    private void loadPlacedMachines() {
        getDataCache().setPlayerMachines(DBConnection.getInstance().getDBManager().getPlacedMachines());
    }

    public void saveAll() {
        new HashSet<>(getDataCache().getDeletedMachines()).forEach(machine -> {
            DBConnection.getInstance().getDBManager().deleteMachine(serializeLocation(machine));
        });

        getDataCache().getDeletedMachines().clear();

        new HashSet<>(getDataCache().getPlayerMachines().values()).forEach(machine -> {
            if (machine == null) return;
            if (!machine.isQueueUpdate()) return;

            DBConnection.getInstance().getDBManager().saveMachine(machine);
            machine.setQueueUpdate(false);
        });
    }

    private void cache(Machine machine) {
        getDataCache().getMachines().put(machine.getType().toUpperCase(), machine);
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

                    PlayerMachine machine = getDataCache().getPlayerMachines().get(blocks.getLocation());
                    if (machine == null) continue;
                    if (!StringUtils.equals(type, machine.getMachine().getType())) continue;
                    if (machine.hasReachStackLimit()) continue;

                    BigInteger overLimit = BigInteger.ZERO;
                    if (machine.getStack().add(addAmount).compareTo(machine.getMachine().getMaxStack()) > 0) {
                        overLimit = machine.getStack().add(addAmount).subtract(machine.getMachine().getMaxStack());
                    }

                    return new Object[] { machine, overLimit };
                }
            }
        }

        return null;
    }

    public MachineCacheData getDataCache() {
        return machineCacheData;
    }

    public PlayerMachine getMachine(Location location) {
        return getDataCache().getPlayerMachines().get(location);
    }

    public Machine getMachine(String type) {
        return getDataCache().getMachines().get(type.toUpperCase());
    }

    public String serializeManagers(List<Manager> managers) {
        if (managers == null || managers.isEmpty()) return "";

        StringBuilder serialized = new StringBuilder(32);

        for (Manager manager : managers) {
            serialized.append(manager.getUUID().toString()).append("#");

            for (Permission permission : manager.getPermissions()) {
                serialized.append(permission.toString()).append("#");
            }

            serialized.append(",");
        }

        return serialized.toString();
    }

    public List<Manager> deserializeManagers(String managers) {
        if (managers == null || managers.isEmpty()) return new ArrayList<>(5);

        List<Manager> ret = new ArrayList<>(64);
        String[] split = managers.split(",");

        for (String str : split) {
            if (str == null) break;

            String[] managersSplit = str.split("#");

            List<Permission> permissions = new ArrayList<>(5);
            if (managersSplit.length > 1) {
                for (int i = 1; i < managersSplit.length; ++i) {
                    permissions.add(Permission.valueOf(managersSplit[i]));
                }
            }

            ret.add(new Manager(UUID.fromString(managersSplit[0]), permissions));
        }

        return ret;
    }

    public String serializeLocation(Location location) {
        if (location == null) return null;

        StringBuilder serialized = new StringBuilder(4);
        serialized.append(location.getWorld().getName());
        serialized.append("#" + location.getX());
        serialized.append("#" + location.getY());
        serialized.append("#" + location.getZ());

        return serialized.toString();
    }

    public Location deserializeLocation(String location) {
        if (location == null) return null;

        String[] locationSplit = location.split("#");
        double x = Double.parseDouble(locationSplit[1]);
        double y = Double.parseDouble(locationSplit[2]);
        double z = Double.parseDouble(locationSplit[3]);

        return new Location(Bukkit.getWorld(locationSplit[0]), x, y, z);
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
