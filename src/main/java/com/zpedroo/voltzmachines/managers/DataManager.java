package com.zpedroo.voltzmachines.managers;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.cache.DataCache;
import com.zpedroo.voltzmachines.mysql.DBConnection;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.objects.PlayerMachine;
import com.zpedroo.voltzmachines.utils.builder.ItemBuilder;
import com.zpedroo.voltzmachines.utils.enums.Permission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class DataManager {

    private static DataManager instance;
    public static DataManager getInstance() { return instance; }

    private DataCache dataCache;

    public DataManager() {
        instance = this;
        this.dataCache = new DataCache();
        this.loadConfigMachines();
        VoltzMachines.get().getServer().getScheduler().runTaskLaterAsynchronously(VoltzMachines.get(), this::loadPlacedMachines, 20L);
    }

    private void loadConfigMachines() {
        File folder = new File(VoltzMachines.get().getDataFolder(), "/machines");
        File[] files = folder.listFiles((file, name) -> name.endsWith(".yml"));
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

    private void loadPlacedMachines() {
        dataCache.setPlayerMachines(DBConnection.getInstance().getDBManager().getPlacedMachines());
    }

    public void saveAll() {
        new HashSet<>(dataCache.getDeletedMachines()).forEach(machine -> {
            DBConnection.getInstance().getDBManager().deleteMachine(serializeLocation(machine));
        });

        dataCache.getDeletedMachines().clear();

        new HashSet<>(dataCache.getPlayerMachines().values()).forEach(machine -> {
            if (machine == null) return;
            if (!machine.isQueueUpdate()) return;

            DBConnection.getInstance().getDBManager().saveMachine(machine);
            machine.setQueueUpdate(false);
        });
    }

    public void updateTopMachines() {
        dataCache.setTopMachines(getTopMachinesOrdered());
    }

    private void cache(Machine machine) {
        dataCache.getMachines().put(machine.getType().toUpperCase(), machine);
    }

    private Map<UUID, BigInteger> getTopMachinesOrdered() {
        Map<UUID, BigInteger> playerMachines = new HashMap<>(dataCache.getPlayerMachinesByUUID().size());

        new HashSet<>(dataCache.getPlayerMachinesByUUID().values()).forEach(machines -> {
            new HashSet<>(machines).forEach(machine -> {
                playerMachines.put(machine.getOwnerUUID(), machine.getStack().add(playerMachines.getOrDefault(machine.getOwnerUUID(), BigInteger.ZERO)));
            });
        });

        return orderMapByValue(playerMachines, 10);
    }

    private Map<UUID, BigInteger> orderMapByValue(Map<UUID, BigInteger> map, Integer limit) {
        return map.entrySet().stream().sorted((value1, value2) -> value2.getValue().compareTo(value1.getValue())).limit(limit)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (value1, value2) -> value1, LinkedHashMap::new));
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

    public PlayerMachine getMachine(Location location) {
        return dataCache.getPlayerMachines().get(location);
    }

    public Machine getMachine(String type) {
        return dataCache.getMachines().get(type.toUpperCase());
    }

    public DataCache getCache() {
        return dataCache;
    }
}