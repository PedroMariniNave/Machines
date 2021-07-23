package com.zpedroo.voltzmachines.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.machine.MachineHologram;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.machine.PlayerMachine;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ProtocolLibHook {

    private static ProtocolLibHook instance;
    public static ProtocolLibHook getInstance() { return instance; }

    private ProtocolManager protocolManager;

    private HashMap<Player, List<MachineHologram>> holograms;

    public ProtocolLibHook() {
        instance = this;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
        this.holograms = new HashMap<>(512);
        this.registerPackets();
    }

    private void registerPackets() {
        getProtocolManager().addPacketListener(new PacketAdapter(VoltzMachines.get(), ListenerPriority.LOWEST, PacketType.Play.Client.LOOK) {
            public void onPacketReceiving(PacketEvent event) {
                Player player = event.getPlayer();
                Block block = player.getTargetBlock(null, 15);

                Location location = block.getLocation();

                PlayerMachine machine = MachineManager.getInstance().getMachine(location);

                if (machine == null) {
                    if (!holograms.containsKey(player)) return;

                    List<MachineHologram> holoList = holograms.remove(player);

                    for (MachineHologram hologram : holoList) {
                        hologram.hideTo(player);
                    }
                    return;
                }

                MachineHologram hologram = machine.getHologram();

                hologram.showTo(player);

                List<MachineHologram> holoList = holograms.containsKey(player) ? holograms.get(player) : new ArrayList<>();
                holoList.add(hologram);

                holograms.put(player, holoList);
            }
        });
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }
}
