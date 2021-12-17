package com.zpedroo.voltzmachines.objects;

import com.gmail.filoghost.holographicdisplays.api.Hologram;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import com.gmail.filoghost.holographicdisplays.api.line.TextLine;
import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.utils.config.Settings;
import org.bukkit.entity.Item;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MachineHologram {

    private PlacedMachine machine;

    private String[] hologramLines;
    private TextLine[] textLines;
    private Item displayItem;

    private Hologram hologram;

    public MachineHologram(PlacedMachine machine) {
        this.machine = machine;
        this.hologramLines = Settings.MACHINE_HOLOGRAM;
        this.updateHologramAndItem();
    }

    public void updateHologramAndItem() {
        if (machine.isDeleted()) return;

        new BukkitRunnable() {
            @Override
            public void run() {
                updateBlock();
                updateHologram();
                spawnItem();
            }
        }.runTaskLater(VoltzMachines.get(), 0L);
    }

    public void removeHologramAndItem() {
        removeHologram();
        removeItem();
    }

    private void updateHologram() {
        if (machine.isDeleted()) return;
        if (hologram == null || hologram.isDeleted()) return;

        for (int i = 0; i < hologramLines.length; i++) {
            textLines[i].setText(machine.replace(hologramLines[i]));
        }
    }

    public void spawnHologram() {
        if (machine.isDeleted()) return;
        if (hologram != null && !hologram.isDeleted()) return;

        hologram = HologramsAPI.createHologram(VoltzMachines.get(), machine.getLocation().clone().add(0.5D, 3.95, 0.5D));
        textLines = new TextLine[hologramLines.length];

        for (int i = 0; i < hologramLines.length; i++) {
            textLines[i] = hologram.insertTextLine(i, machine.replace(hologramLines[i]));
        }
    }

    public void removeHologram() {
        if (hologram == null || hologram.isDeleted()) return;

        hologram.delete();
        hologram = null;
    }

    private void spawnItem() {
        if (machine.isDeleted()) return;
        if (displayItem != null && !displayItem.isDead()) return;

        displayItem = machine.getLocation().getWorld().dropItem(machine.getLocation().clone().add(0.5D, 1D, 0.5D), machine.getMachine().getDisplayItem());
        displayItem.setVelocity(new Vector(0, 0.1, 0));
        displayItem.setPickupDelay(Integer.MAX_VALUE);
        displayItem.setMetadata("***", new FixedMetadataValue(VoltzMachines.get(), true));
        displayItem.setCustomNameVisible(false);
    }

    private void removeItem() {
        if (displayItem == null || displayItem.isDead()) return;

        displayItem.remove();
        displayItem = null;
    }

    private void updateBlock() {
        if (machine.getLocation().getBlock().getType().equals(machine.getMachine().getBlock())) return;

        machine.getLocation().getBlock().setType(machine.getMachine().getBlock());
        machine.getLocation().getBlock().setData(machine.getMachine().getBlockData());
    }
}