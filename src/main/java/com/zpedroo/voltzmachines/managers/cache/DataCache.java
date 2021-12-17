package com.zpedroo.voltzmachines.managers.cache;

import com.zpedroo.voltzmachines.objects.Bonus;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlacedMachine;
import org.bukkit.Location;

import java.math.BigInteger;
import java.util.*;

public class DataCache {

    private Map<String, Machine> machines;
    private Map<Location, PlacedMachine> placedMachines;
    private Map<UUID, List<PlacedMachine>> placedMachinesByUUID;
    private Map<UUID, BigInteger> topMachines;
    private Set<Location> deletedMachines;
    private List<Bonus> bonuses;

    public DataCache() {
        this.machines = new HashMap<>(24);
        this.deletedMachines = new HashSet<>(32);
        this.placedMachinesByUUID = new HashMap<>(32);
        this.bonuses = new ArrayList<>(4);
    }

    public Map<String, Machine> getMachines() {
        return machines;
    }

    public Map<Location, PlacedMachine> getPlacedMachines() {
        return placedMachines;
    }

    public Map<UUID, List<PlacedMachine>> getPlacedMachinesByUUID() {
        return placedMachinesByUUID;
    }

    public List<PlacedMachine> getPlayerMachinesByUUID(UUID uuid) {
        return placedMachinesByUUID.getOrDefault(uuid, new LinkedList<>());
    }

    public Map<UUID, BigInteger> getTopMachines() {
        return topMachines;
    }

    public Set<Location> getDeletedMachines() {
        return deletedMachines;
    }

    public List<Bonus> getBonuses() {
        return bonuses;
    }

    public void setPlacedMachines(Map<Location, PlacedMachine> placedMachines) {
        this.placedMachines = placedMachines;
    }

    public void setUUIDMachines(UUID uuid, List<PlacedMachine> machines) {
        this.placedMachinesByUUID.put(uuid, machines);
    }

    public void setTopMachines(Map<UUID, BigInteger> topMachines) {
        this.topMachines = topMachines;
    }
}