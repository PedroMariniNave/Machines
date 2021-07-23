package com.zpedroo.voltzmachines.machine.cache;

import com.zpedroo.voltzmachines.machine.Machine;
import com.zpedroo.voltzmachines.machine.PlayerMachine;
import org.bukkit.Location;

import java.util.*;

public class MachineCacheData {

    private HashMap<String, Machine> machines;
    private HashMap<Location, PlayerMachine> playerMachines;
    private Map<UUID, List<PlayerMachine>> playerMachinesByUUID;
    private Set<Location> deletedMachines;

    public MachineCacheData() {
        this.machines = new HashMap<>(32);
        this.playerMachines = new HashMap<>(5120);
        this.deletedMachines = new HashSet<>(5120);
        this.playerMachinesByUUID = new HashMap<>(2560);
    }

    public HashMap<String, Machine> getMachines() {
        return machines;
    }

    public HashMap<Location, PlayerMachine> getPlayerMachines() {
        return playerMachines;
    }

    public List<PlayerMachine> getPlayerMachinesByUUID(UUID uuid) {
        if (!playerMachinesByUUID.containsKey(uuid)) return new ArrayList<>();

        return playerMachinesByUUID.get(uuid);
    }

    public Set<Location> getDeletedMachines() {
        return deletedMachines;
    }

    public void setPlayerMachines(HashMap<Location, PlayerMachine> playerMachines) {
        this.playerMachines = playerMachines;
    }

    public void setUUIDMachines(UUID uuid, List<PlayerMachine> machines) {
        this.playerMachinesByUUID.put(uuid, machines);
    }
}