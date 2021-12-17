package com.zpedroo.voltzmachines.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.objects.PlacedMachine;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ProtocolLibHook extends PacketAdapter {

    public ProtocolLibHook(Plugin plugin, PacketType packetType) {
        super(plugin, packetType);
    }


    private static final Map<PlacedMachine, List<UUID>> machineViewers = new HashMap<>(16);

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        Block block = player.getTargetBlock((HashSet<Byte>) null, 15);

        Location location = block.getLocation();
        PlacedMachine placedMachine = DataManager.getInstance().getPlacedMachine(location);

        if (placedMachine == null) {
            removeViewer(player);
            return;
        }

        addViewer(player, placedMachine);
    }

    public static void removeViewer(Player player) {
        new HashSet<>(machineViewers.entrySet()).stream().filter(spawnersEntry -> spawnersEntry.getValue().contains(player.getUniqueId()))
                .forEach(entry -> {
                    entry.getValue().remove(player.getUniqueId());
                    if (entry.getValue().size() <= 0) {
                        PlacedMachine machine = entry.getKey();
                        machine.getHologram().removeHologram();
                        machineViewers.remove(machine);
                    }
                });
    }

    private static void addViewer(Player player, PlacedMachine placedMachine) {
        if (machineViewers.containsKey(placedMachine) && machineViewers.get(placedMachine).contains(player.getUniqueId())) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                placedMachine.getHologram().spawnHologram();
            }
        }.runTaskLater(VoltzMachines.get(), 0L);

        List<UUID> viewers = machineViewers.getOrDefault(placedMachine, new ArrayList<>(2));
        viewers.add(player.getUniqueId());

        machineViewers.put(placedMachine, viewers);
    }
}
