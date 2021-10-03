package com.zpedroo.voltzmachines.hooks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.objects.MachineHologram;
import com.zpedroo.voltzmachines.objects.PlayerMachine;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class ProtocolLibHook extends PacketAdapter {

    public ProtocolLibHook(Plugin plugin, PacketType packetType) {
        super(plugin, packetType);
    }

    private Map<Player, List<MachineHologram>> hologramsToHide = new HashMap<>(128);

    public void onPacketReceiving(PacketEvent event) {
        Player player = event.getPlayer();
        Block block = player.getTargetBlock(null, 15);

        Location location = block.getLocation();
        PlayerMachine machine = DataManager.getInstance().getMachine(location);

        if (machine == null) {
            if (!hologramsToHide.containsKey(player)) return;

            List<MachineHologram> holoList = hologramsToHide.remove(player);

            for (MachineHologram hologram : holoList) {
                hologram.hideTo(player);
            }
            return;
        }

        MachineHologram hologram = machine.getHologram();

        hologram.showTo(player);

        List<MachineHologram> holoList = hologramsToHide.containsKey(player) ? hologramsToHide.get(player) : new ArrayList<>(2);
        holoList.add(hologram);

        hologramsToHide.put(player, holoList);
    }
}
