package com.zpedroo.voltzmachines.managers;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.cache.DataCache;
import com.zpedroo.voltzmachines.mysql.DBConnection;
import com.zpedroo.voltzmachines.objects.Bonus;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.objects.PlacedMachine;
import com.zpedroo.voltzmachines.utils.FileUtils;
import com.zpedroo.voltzmachines.utils.builder.ItemBuilder;
import com.zpedroo.voltzmachines.enums.Permission;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
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
import java.util.stream.Collectors;

public class DataManager {

    private static DataManager instance;
    public static DataManager getInstance() { return instance; }

    private DataCache dataCache;

    public DataManager() {
        instance = this;
        this.dataCache = new DataCache();
        this.loadConfigMachines();
        this.loadConfigBonuses();
        VoltzMachines.get().getServer().getScheduler().runTaskLaterAsynchronously(VoltzMachines.get(), this::loadPlacedMachines, 20L);
        VoltzMachines.get().getServer().getScheduler().runTaskLaterAsynchronously(VoltzMachines.get(), this::loadTopMachines, 20L);
    }

    private void loadConfigMachines() {
        File folder = new File(VoltzMachines.get().getDataFolder(), "/machines");
        File[] files = folder.listFiles((file, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File fl : files) {
            if (fl == null) continue;

            FileConfiguration file = YamlConfiguration.loadConfiguration(fl);

            ItemStack item = ItemBuilder.build(file, "Machine-Settings.item").build();

            String[] materialSplit = file.getString("Machine-Settings.machine-block").split(":");

            Material block = Material.valueOf(materialSplit[0]);
            byte blockData = materialSplit.length > 1 ? Byte.parseByte(materialSplit[1]) : 0;
            String type = fl.getName().replace(".yml", "");
            String typeTranslated = file.getString("Machine-Settings.type-translated");
            String displayName = ChatColor.translateAlternateColorCodes('&', file.getString("Machine-Settings.display-name"));
            int delay = file.getInt("Machine-Settings.drops.delay");
            BigInteger amount = NumberFormatter.getInstance().filter(file.getString("Machine-Settings.drops.amount"));
            BigInteger dropsValue = NumberFormatter.getInstance().filter(file.getString("Machine-Settings.drops.price"));
            BigInteger dropsPreviousValue = NumberFormatter.getInstance().filter(file.getString("Machine-Settings.drops.previous"));
            BigInteger dropsMinimumValue = NumberFormatter.getInstance().filter(file.getString("Machine-Settings.drops.min"));
            BigInteger dropsMaximumValue = NumberFormatter.getInstance().filter(file.getString("Machine-Settings.drops.max"));
            BigInteger maxStack = NumberFormatter.getInstance().filter(file.getString("Machine-Settings.max-stack"));
            String permission = file.getString("Machine-Settings.permission", null);
            List<String> commands = file.getStringList("Machine-Settings.commands");

            Machine machine = new Machine(item, block, blockData, type, typeTranslated, displayName, delay, amount, dropsValue, dropsPreviousValue, dropsMinimumValue, dropsMaximumValue, maxStack, permission, commands);
            
            if (dropsValue.signum() <= 0) {
                MachineManager.updatePrice(machine);
            }

            cache(machine);
        }
    }

    private void loadConfigBonuses() {
        FileUtils.Files file = FileUtils.Files.CONFIG;
        for (String str : FileUtils.get().getSection(file, "Bonus")) {
            String permission = FileUtils.get().getString(file, "Bonus." + str + ".permission");
            double bonusPercentage = FileUtils.get().getDouble(file, "Bonus." + str + ".bonus");

            cache(new Bonus(permission, bonusPercentage));
        }
    }

    private void loadPlacedMachines() {
        dataCache.setPlacedMachines(DBConnection.getInstance().getDBManager().getPlacedMachines());
    }

    private void loadTopMachines() {
        dataCache.setTopMachines(DBConnection.getInstance().getDBManager().getCache().getTopMachines());
    }

    public void saveAll() {
        new HashSet<>(dataCache.getDeletedMachines()).forEach(machineLocation -> {
            DBConnection.getInstance().getDBManager().deleteMachine(machineLocation);
        });

        dataCache.getDeletedMachines().clear();

        new HashSet<>(dataCache.getPlacedMachines().values()).forEach(machine -> {
            if (machine == null) return;
            if (!machine.isQueueUpdate()) return;

            DBConnection.getInstance().getDBManager().saveMachine(machine);
            machine.setUpdate(false);
        });
    }

    public void updateTopMachines() {
        dataCache.setTopMachines(getTopMachinesOrdered());
    }

    private void cache(Machine machine) {
        dataCache.getMachines().put(machine.getType().toUpperCase(), machine);
    }

    private void cache(Bonus bonus) {
        dataCache.getBonuses().add(bonus);
    }

    private Map<UUID, BigInteger> getTopMachinesOrdered() {
        Map<UUID, BigInteger> playerMachines = new HashMap<>(dataCache.getPlacedMachinesByUUID().size());

        new HashSet<>(dataCache.getPlacedMachinesByUUID().values()).forEach(machines -> {
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

        StringBuilder serialized = new StringBuilder(16);

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

        List<Manager> ret = new ArrayList<>(4);
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

    public List<PlacedMachine> getPlacedMachinesByUUID(UUID uuid) {
        List<PlacedMachine> placedMachines = dataCache.getPlayerMachinesByUUID(uuid);
        if (placedMachines == null) {
            placedMachines = DBConnection.getInstance().getDBManager().getPlacedMachinesByUUID(uuid);
            dataCache.getPlacedMachinesByUUID().put(uuid, placedMachines);
        }

        return placedMachines;
    }

    public PlacedMachine getPlacedMachine(Location location) {
        if (dataCache.getPlacedMachines() == null) return null;
        
        return dataCache.getPlacedMachines().get(location);
    }

    public Machine getMachine(String type) {
        return dataCache.getMachines().get(type.toUpperCase());
    }

    public DataCache getCache() {
        return dataCache;
    }
}