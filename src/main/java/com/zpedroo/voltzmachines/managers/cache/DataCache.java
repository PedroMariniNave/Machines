package com.zpedroo.voltzmachines.managers.cache;

import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlayerMachine;
import org.bukkit.Location;

import java.math.BigInteger;
import java.util.*;

public class DataCache {

    private Map<String, Machine> machines;
    private Map<Location, PlayerMachine> playerMachines;
    private Map<UUID, List<PlayerMachine>> playerMachinesByUUID;
    private Map<UUID, BigInteger> topMachines;
    private Set<Location> deletedMachines;

    public DataCache() {
        this.machines = new HashMap<>(32);
        this.playerMachines = new HashMap<>(5120);
        this.deletedMachines = new HashSet<>(5120);
        this.playerMachinesByUUID = new HashMap<>(2560);
        this.topMachines = new HashMap<>(10);
    }

    public Map<String, Machine> getMachines() {
        return machines;
    }

    public Map<Location, PlayerMachine> getPlayerMachines() {
        return playerMachines;
    }

    public Map<UUID, List<PlayerMachine>> getPlayerMachinesByUUID() {
        return playerMachinesByUUID;
    }

    public List<PlayerMachine> getPlayerMachinesByUUID(UUID uuid) {
        if (!playerMachinesByUUID.containsKey(uuid)) return new ArrayList<>();

        return playerMachinesByUUID.get(uuid);
    }

    public Map<UUID, BigInteger> getTopMachines() {
        return topMachines;
    }

    public Set<Location> getDeletedMachines() {
        return deletedMachines;
    }

    public void setPlayerMachines(Map<Location, PlayerMachine> playerMachines) {
        this.playerMachines = playerMachines;
    }

    public void setUUIDMachines(UUID uuid, List<PlayerMachine> machines) {
        this.playerMachinesByUUID.put(uuid, machines);
    }

    public void setTopMachines(Map<UUID, BigInteger> topMachines) {
        this.topMachines = topMachines;
    }
}