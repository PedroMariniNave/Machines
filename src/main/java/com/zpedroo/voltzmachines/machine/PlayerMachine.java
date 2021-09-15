package com.zpedroo.voltzmachines.machine;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.utils.config.Messages;
import com.zpedroo.voltzmachines.utils.config.Titles;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public class PlayerMachine {

    private Location location;
    private UUID ownerUUID;
    private BigInteger stack;
    private BigInteger fuel;
    private BigInteger drops;
    private Integer integrity;
    private Machine machine;
    private List<Manager> managers;
    private Boolean infiniteFuel;
    private Boolean infiniteIntegrity;
    private Boolean status;
    private Boolean update;
    private Integer delay;
    private MachineHologram hologram;

    public PlayerMachine(Location location, UUID ownerUUID, BigInteger stack, BigInteger fuel, BigInteger drops, Integer integrity, Machine machine, List<Manager> managers, Boolean infiniteFuel, Boolean infiniteIntegrity) {
        this.location = location;
        this.ownerUUID = ownerUUID;
        this.stack = stack;
        this.fuel = fuel;
        this.drops = drops;
        this.integrity = integrity;
        this.machine = machine;
        this.managers = managers;
        this.infiniteFuel = infiniteFuel;
        this.infiniteIntegrity = infiniteIntegrity;
        this.status = false;
        this.update = false;
        this.delay = machine.getDelay();
        this.hologram = new MachineHologram(this);
    }

    public Location getLocation() {
        return location;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
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

    public Integer getIntegrity() {
        return integrity;
    }

    public Machine getMachine() {
        return machine;
    }

    public List<Manager> getManagers() {
        return managers;
    }

    public Boolean hasInfiniteFuel() {
        return infiniteFuel;
    }

    public Boolean hasInfiniteIntegrity() {
        return infiniteIntegrity;
    }

    public Manager getManager(UUID uuid) {
        for (Manager manager : managers) {
            if (!manager.getUUID().equals(uuid)) continue;

            return manager;
        }

         return null;
    }

    public Boolean isEnabled() {
        return status;
    }

    public Boolean isQueueUpdate() {
        return update;
    }

    public Boolean hasReachStackLimit() {
        if (machine.getMaxStack().signum() < 0) return false;

        return stack.compareTo(machine.getMaxStack()) >= 0;
    }

    public Boolean canInteract(Player player) {
        if (player.getUniqueId().equals(getOwnerUUID())) return true;
        if (player.hasPermission("machines.admin")) return true;

        Manager manager = getManager(player.getUniqueId());
        return manager != null;
    }

    public Integer getDelay() {
        return delay;
    }

    public MachineHologram getHologram() {
        return hologram;
    }

    public void delete() {
        MachineManager.getInstance().getDataCache().getDeletedMachines().add(location);
        MachineManager.getInstance().getDataCache().getPlayerMachines().remove(location);
        MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(ownerUUID).remove(this);

        this.hologram.remove();
        this.location.getBlock().setType(Material.AIR);
    }

    public void setInfiniteFuel(Boolean infiniteFuel) {
        this.infiniteFuel = infiniteFuel;
        this.update = true;
        this.hologram.update(this);
    }

    public void setInfiniteIntegrity(Boolean infiniteIntegrity) {
        this.infiniteIntegrity = infiniteIntegrity;
        this.update = true;
        this.hologram.update(this);
    }

    public String replace(String text) {
        if (text == null || text.isEmpty()) return "";

        return StringUtils.replaceEach(text, new String[] {
                "{owner}",
                "{type}",
                "{stack}",
                "{max_stack}",
                "{fuel}",
                "{drops}",
                "{integrity}",
                "{status}"
        }, new String[] {
                Bukkit.getOfflinePlayer(ownerUUID).getName(),
                machine.getTypeTranslated(),
                NumberFormatter.getInstance().format(stack),
                NumberFormatter.getInstance().format(machine.getMaxStack()),
                infiniteFuel ? "∞" : NumberFormatter.getInstance().format(fuel),
                NumberFormatter.getInstance().format(drops),
                infiniteIntegrity ? "∞" : integrity.toString() + "%",
                status ? Messages.ENABLED : Messages.DISABLED
        });
    }

    public void switchStatus() {
        this.status = !status;
        this.hologram.update(this);
    }

    public void setStatus(Boolean status) {
        this.status = status;
        this.hologram.update(this);
    }

    public void updateDelay() {
        this.delay = machine.getDelay();
    }

    public void setDelay(Integer delay) {
        this.delay = delay;
    }

    public void setQueueUpdate(Boolean status) {
        this.update = status;
    }

    public void addFuel(BigInteger value) {
        this.fuel = fuel.add(value);
        this.update = true;
        this.hologram.update(this);
    }

    public void removeFuel(BigInteger value) {
        this.fuel = fuel.subtract(value);
        if (fuel.signum() <= 0) this.fuel = BigInteger.ZERO;

        this.update = true;
        this.hologram.update(this);
    }

    public void addStack(BigInteger value) {
        this.stack = stack.add(value);
        this.update = true;
        this.hologram.update(this);
    }

    public void removeStack(BigInteger value) {
        this.stack = stack.subtract(value);
        this.update = true;
        if (stack.signum() <= 0) {
            VoltzMachines.get().getServer().getScheduler().runTaskLater(VoltzMachines.get(), this::delete, 0L); // fix async block remove
            return;
        }

        this.hologram.update(this);
    }

    public void setDrops(BigInteger value) {
        this.drops = value;
        this.update = true;
        this.hologram.update(this);
    }

    public void addDrops(BigInteger value) {
        this.drops = drops.add(value);
        this.update = true;
        this.hologram.update(this);
    }

    public void setIntegrity(Integer value) {
        if (value <= 0) {
            this.delete();
            return;
        }

        this.integrity = value;
        this.hologram.update(this);
    }

    public void addIntegrity(Integer value) {
        this.integrity += value;
        this.update = true;
        this.hologram.update(this);
    }

    public void sellDrops(Player player) {
        if (drops.signum() <= 0) return;

        for (String cmd : machine.getCommands()) {
            if (cmd == null) break;

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), StringUtils.replaceEach(cmd, new String[]{
                    "{player}",
                    "{amount}"
            }, new String[]{
                    player.getName(),
                    drops.toString()
            }));
        }

        player.sendTitle(Titles.WHEN_SELL_TITLE, StringUtils.replaceEach(Titles.WHEN_SELL_SUBTITLE, new String[]{
                "{value}"
        }, new String[]{
                NumberFormatter.getInstance().format(drops)
        }));

        this.drops = BigInteger.ZERO;
        this.hologram.update(this);
    }

    public void cache() {
        MachineManager.getInstance().getDataCache().getPlayerMachines().put(location, this);

        List<PlayerMachine> machines = MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(getOwnerUUID());
        machines.add(this);

        MachineManager.getInstance().getDataCache().setUUIDMachines(ownerUUID, machines);
    }
}