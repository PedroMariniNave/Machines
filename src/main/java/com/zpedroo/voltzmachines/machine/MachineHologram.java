package com.zpedroo.voltzmachines.machine;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.utils.config.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class MachineHologram {

    private String[] hologramLines;
    private TextLine[] textLines;
    private Item displayItem;

    private Hologram hologram;

    public MachineHologram(PlayerMachine machine) {
        this.hologramLines = Settings.MACHINE_HOLOGRAM;
        Bukkit.getScheduler().runTaskLater(VoltzMachines.get(), () -> update(machine), 0L);
    }

    public void update(PlayerMachine machine) {
        machine.getLocation().getBlock().setType(machine.getMachine().getBlock());

        if (hologram != null && hologram.isDeleted()) return;

        if (hologram == null) {
            hologram = HologramsAPI.createHologram(VoltzMachines.get(), machine.getLocation().clone().add(0.5D, 3.95, 0.5D));
            textLines = new TextLine[hologramLines.length];

            for (int i = 0; i < hologramLines.length; i++) {
                textLines[i] = hologram.insertTextLine(i, machine.replace(hologramLines[i]));
            }

            hologram.getVisibilityManager().setVisibleByDefault(false);

            displayItem = machine.getLocation().getWorld().dropItem(machine.getLocation().clone().add(0.5D, 1D, 0.5D), machine.getMachine().getDisplayItem());
            displayItem.setVelocity(new Vector(0, 0.1, 0));
            displayItem.setPickupDelay(Integer.MAX_VALUE);
            displayItem.setCustomName("Machine Item");
            displayItem.setCustomNameVisible(false);
        } else {
            for (int i = 0; i < hologramLines.length; i++) {
                this.textLines[i].setText(machine.replace(hologramLines[i]));
            }
        }
    }

    public void showTo(Player player) {
        if (hologram == null) return;

        this.hologram.getVisibilityManager().showTo(player);
    }

    public void hideTo(Player player) {
        if (hologram == null) return;

        this.hologram.getVisibilityManager().hideTo(player);
    }

    public void remove() {
        if (hologram == null) return;

        this.hologram.delete();
        this.displayItem.remove();
        this.hologram = null;
    }
}