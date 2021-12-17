package com.zpedroo.voltzmachines.objects;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.BonusManager;
import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.utils.config.Messages;
import com.zpedroo.voltzmachines.utils.config.Titles;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public class PlacedMachine {

    private Location location;
    private UUID ownerUUID;
    private BigInteger stack;
    private BigInteger fuel;
    private BigInteger drops;
    private BigInteger integrity;
    private Machine machine;
    private MachineHologram hologram;
    private List<Manager> managers;
    private boolean infiniteFuel;
    private boolean infiniteIntegrity;
    private boolean status;
    private boolean update;
    private boolean deleted;
    private int delay;

    public PlacedMachine(Location location, UUID ownerUUID, BigInteger stack, BigInteger fuel, BigInteger drops, BigInteger integrity, Machine machine, List<Manager> managers, boolean infiniteFuel, boolean infiniteIntegrity) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.stack = stack;
        this.fuel = fuel;
        this.drops = drops;
        this.integrity = integrity;
        this.machine = machine;
        this.hologram = new MachineHologram(this);
        this.managers = managers;
        this.infiniteFuel = infiniteFuel;
        this.infiniteIntegrity = infiniteIntegrity;
        this.status = false;
        this.update = false;
        this.deleted = false;
        this.delay = machine.getDropsDelay();
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public OfflinePlayer getOwner() {
        return Bukkit.getOfflinePlayer(ownerUUID);
    }

    public BigInteger getStack() {
        return stack;
    }

    public BigInteger getFuel() {
        return fuel;
    }

    public BigInteger getDrops() {
        return drops;
    }

    public BigInteger getIntegrity() {
        return integrity;
    }

    public Machine getMachine() {
        return machine;
    }

    public List<Manager> getManagers() {
        return managers;
    }

    public Manager getManager(UUID uuid) {
        for (Manager manager : managers) {
            if (!manager.getUUID().equals(uuid)) continue;

            return manager;
        }

        return null;
    }

    public boolean hasInfiniteFuel() {
        return infiniteFuel;
    }

    public boolean hasInfiniteIntegrity() {
        return infiniteIntegrity;
    }

    public boolean isEnabled() {
        return status;
    }

    public boolean isQueueUpdate() {
        return update;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public boolean hasReachStackLimit() {
        if (machine.getMaxStack().signum() <= 0) return false;

        return stack.compareTo(machine.getMaxStack()) >= 0;
    }

    public boolean canInteract(Player player) {
        if (player.getUniqueId().equals(getOwnerUUID())) return true;
        if (player.hasPermission("machines.admin")) return true;

        Manager manager = getManager(player.getUniqueId());
        return manager != null;
    }

    public int getDelay() {
        return delay;
    }

    public MachineHologram getHologram() {
        return hologram;
    }

    public void delete() {
        this.deleted = true;

        DataManager.getInstance().getCache().getDeletedMachines().add(location);
        DataManager.getInstance().getCache().getPlacedMachines().remove(location);
        DataManager.getInstance().getCache().getPlayerMachinesByUUID(ownerUUID).remove(this);

        this.location.getBlock().setType(Material.AIR);
        if (hologram != null) this.hologram.removeHologramAndItem();
    }

    public void setInfiniteFuel(boolean infiniteFuel) {
        this.infiniteFuel = infiniteFuel;
        this.update = true;
        if (hologram != null) this.hologram.updateHologramAndItem();
    }

    public void setInfiniteIntegrity(boolean infiniteIntegrity) {
        this.infiniteIntegrity = infiniteIntegrity;
        this.update = true;
        if (hologram != null) this.hologram.updateHologramAndItem();
    }

    public String replace(String text) {
        if (text == null || text.isEmpty()) return "";

        return StringUtils.replaceEach(text, new String[] {
                "{owner}",
                "{stack}",
                "{max_stack}",
                "{drops}",
                "{fuel}",
                "{integrity}",
                "{type}",
                "{status}"
        }, new String[] {
                Bukkit.getOfflinePlayer(ownerUUID).getName(),
                NumberFormatter.getInstance().format(stack),
                NumberFormatter.getInstance().format(machine.getMaxStack()),
                NumberFormatter.getInstance().format(drops),
                infiniteFuel ? "∞" : NumberFormatter.getInstance().format(fuel),
                infiniteIntegrity ? "∞" : integrity.toString() + "%",
                machine.getTypeTranslated(),
                status ? Messages.ENABLED : Messages.DISABLED
        });
    }

    public void switchStatus() {
        this.setStatus(!status);
    }

    public void setStatus(boolean status) {
        this.status = status;
        this.hologram.updateHologramAndItem();
    }

    public void updateDelay() {
        this.delay = machine.getDropsDelay();
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void addFuel(BigInteger amount) {
        this.setFuel(fuel.add(amount));
    }

    public void removeFuel(BigInteger amount) {
        this.setFuel(fuel.subtract(amount));
    }

    public void setFuel(BigInteger amount) {
        this.fuel = amount;
        if (fuel.signum() <= 0) this.fuel = BigInteger.ZERO;

        this.update = true;
        if (hologram != null) this.hologram.updateHologramAndItem();
    }

    public void addStack(BigInteger amount) {
        this.setStack(stack.add(amount));
    }

    public void removeStack(BigInteger amount) {
        this.setStack(stack.subtract(amount));
    }

    public void setStack(BigInteger amount) {
        this.stack = amount;
        this.update = true;
        if (stack.signum() <= 0) {
            VoltzMachines.get().getServer().getScheduler().runTaskLater(VoltzMachines.get(), this::delete, 0L); // fix async block remove
            return;
        }

        if (hologram != null) this.hologram.updateHologramAndItem();
    }

    public void addDrops(BigInteger amount) {
        this.setDrops(drops.add(amount));
    }

    public void setDrops(BigInteger amount) {
        this.drops = amount;
        this.update = true;
        if (hologram != null) this.hologram.updateHologramAndItem();
    }

    public void addIntegrity(BigInteger amount) {
        this.setIntegrity(integrity.add(amount));
    }

    public void setIntegrity(BigInteger amount) {
        if (amount.signum() <= 0) {
            this.delete();
            return;
        }

        this.integrity = amount;
        this.update = true;
        if (hologram != null) this.hologram.updateHologramAndItem();
    }

    public void sellDrops(Player player) {
        if (drops.signum() <= 0) return;

        OfflinePlayer machineOwner = getOwner();
        BigInteger finalDropsPrice = BonusManager.applyBonus(machineOwner.getName(), drops.multiply(machine.getDropsValue()));

        for (String cmd : machine.getCommands()) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), StringUtils.replaceEach(cmd, new String[]{
                    "{player}",
                    "{amount}"
            }, new String[]{
                    player.getName(),
                    finalDropsPrice.toString()
            }));
        }

        player.sendTitle(Titles.WHEN_SELL_TITLE, StringUtils.replaceEach(Titles.WHEN_SELL_SUBTITLE, new String[]{
                "{value}"
        }, new String[]{
                NumberFormatter.getInstance().format(finalDropsPrice)
        }));

        this.drops = BigInteger.ZERO;
        if (hologram != null) this.hologram.updateHologramAndItem();
    }

    public void cache() {
        DataManager.getInstance().getCache().getPlacedMachines().put(location, this);

        List<PlacedMachine> machines = DataManager.getInstance().getCache().getPlayerMachinesByUUID(ownerUUID);
        machines.add(this);

        DataManager.getInstance().getCache().setUUIDMachines(ownerUUID, machines);
    }
}