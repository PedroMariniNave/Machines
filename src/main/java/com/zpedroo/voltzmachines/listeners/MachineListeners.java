package com.zpedroo.voltzmachines.listeners;

import com.intellectualcrafters.plot.object.Location;
import com.intellectualcrafters.plot.object.Plot;
import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlacedMachine;
import com.zpedroo.voltzmachines.utils.config.Messages;
import com.zpedroo.voltzmachines.utils.config.Settings;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import com.zpedroo.voltzmachines.utils.item.Items;
import com.zpedroo.voltzmachines.utils.menu.Menus;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MachineListeners implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand() == null || event.getItemInHand().getType().equals(Material.AIR)) return;

        ItemStack item = event.getItemInHand().clone();
        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey("MachinesFuel") || nbt.hasKey("MachinesInfiniteFuel") || nbt.hasKey("MachinesInfiniteRepair") || nbt.hasKey("MachinesRepair")) event.setCancelled(true);
        if (!nbt.hasKey("MachinesAmount")) return;

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (!player.hasPermission("machines.admin")) {
            Location location = new Location(
                    block.getWorld().getName(),
                    block.getX(),
                    block.getY(),
                    block.getZ()
            );
            Plot plot = Plot.getPlot(location);
            if (plot == null) return;
            if (!plot.isAdded(player.getUniqueId())) return;
        }

        Machine machine = DataManager.getInstance().getMachine(nbt.getString("MachinesType").toUpperCase());
        if (machine == null) return;

        if (machine.getPermission() != null && !player.hasPermission(machine.getPermission())) {
            event.setCancelled(true);
            player.sendMessage(Messages.MACHINE_PERMISSION);
            return;
        }

        BigInteger stack = new BigInteger(nbt.getString("MachinesAmount"));
        BigInteger integrity = new BigInteger(nbt.getString("MachinesIntegrity"));
        Object[] objects = MachineManager.getNearMachines(player, block, stack, machine.getType());
        PlacedMachine placedMachine = objects != null ? (PlacedMachine) objects[0] : null;
        BigInteger overLimit = null;

        if (placedMachine != null) {
            event.setCancelled(true);
            overLimit = (BigInteger) objects[1];
            placedMachine.addStack(stack.subtract(overLimit));
        } else {
            int radius = Settings.STACK_RADIUS;

            if (radius > 0) {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            Block blocks = block.getRelative(x, y, z);
                            if (blocks.getType().equals(Material.AIR)) continue;

                            PlacedMachine nearMachine = DataManager.getInstance().getPlacedMachine(blocks.getLocation());
                            if (nearMachine == null) continue;

                            event.setCancelled(true);
                            player.sendMessage(Messages.NEAR_MACHINE);
                            return;
                        }
                    }
                }
            }

            List<PlacedMachine> machinesWithSameType = DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId())
                    .stream().filter(playerMachine -> playerMachine.getMachine().getType().equals(machine.getType())).collect(Collectors.toList());

            if (machinesWithSameType.size() >= 1) {
                event.setCancelled(true);
                player.sendMessage(Messages.ONLY_ONE_MACHINE);
                return;
            }

            if (machine.getMaxStack().signum() > 0 && stack.compareTo(machine.getMaxStack()) > 0) {
                overLimit = stack.subtract(machine.getMaxStack());
            } else overLimit = BigInteger.ZERO;

            block.setType(machine.getBlock());
            block.setData(machine.getBlockData());
            block.getState().update(true);

            placedMachine = new PlacedMachine(block.getLocation(), player.getUniqueId(), stack.subtract(overLimit), BigInteger.ZERO, BigInteger.ZERO, integrity, machine, new ArrayList<>(), false, false);
            placedMachine.cache();
            placedMachine.setUpdate(true);
        }

        item.setAmount(1);
        player.getInventory().removeItem(item);

        if (overLimit.signum() > 0) player.getInventory().addItem(machine.getItem(overLimit, integrity));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        if (event.getItem() == null || event.getItem().getType().equals(Material.AIR)) return;

        PlacedMachine machine = null;
        if (event.getClickedBlock() != null) machine = DataManager.getInstance().getPlacedMachine(event.getClickedBlock().getLocation());
        if (machine != null) return;

        ItemStack item = event.getItem().clone();
        NBTItem nbt = new NBTItem(item);

        Player player = event.getPlayer();
        List<PlacedMachine> machines = DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId());

        if (nbt.hasKey("MachinesFuel")) {

            event.setCancelled(true);

            if (machines.isEmpty()) {
                player.sendMessage(Messages.ZERO_MACHINES_FUEL);
                return;
            }

            Integer machinesWithInfiniteEnergy = 0;
            for (PlacedMachine placedMachine : machines) {
                if (placedMachine.hasInfiniteFuel()) ++machinesWithInfiniteEnergy;
            }

            Integer machinesAmount = machines.size();
            BigInteger amount = new BigInteger(nbt.getString("MachinesFuel"));
            if (player.isSneaking()) {
                amount = amount.multiply(BigInteger.valueOf(item.getAmount()));
            } else {
                item.setAmount(1);
            }

            BigInteger toAdd = amount.divide(BigInteger.valueOf(machinesAmount - machinesWithInfiniteEnergy));
            while (toAdd.compareTo(BigInteger.ONE) < 0) {
                toAdd = amount.divide(BigInteger.valueOf(--machinesAmount));
            }

            for (int i = 0; i < machinesAmount; ++i) {
                PlacedMachine placedMachine = machines.get(i);
                if (placedMachine.hasInfiniteFuel()) continue;

                placedMachine.addFuel(toAdd);
            }

            player.getInventory().removeItem(item);
            player.playSound(player.getLocation(), Sound.LAVA_POP, 0.5f, 10f);
            player.sendMessage(StringUtils.replaceEach(Messages.SUCCESSFUL_FUELED, new String[]{
                    "{machines}",
                    "{fuel}"
            }, new String[]{
                    NumberFormatter.getInstance().formatDecimal(machinesAmount.doubleValue() - machinesWithInfiniteEnergy.doubleValue()),
                    NumberFormatter.getInstance().format(toAdd)
            }));
            return;
        }

        if (nbt.hasKey("MachinesRepair")) {

            event.setCancelled(true);

            if (machines.isEmpty()) {
                player.sendMessage(Messages.ZERO_MACHINES_REPAIR);
                return;
            }

            Integer machinesAmount = machines.size();
            BigInteger percentage = new BigInteger(nbt.getString("MachinesRepair"));
            if (player.isSneaking()) {
                percentage = percentage.multiply(BigInteger.valueOf(item.getAmount()));
            } else {
                item.setAmount(1);
            }

            BigInteger toAdd = percentage.divide(BigInteger.valueOf(machinesAmount));
            BigInteger amountRepaired = BigInteger.ZERO;
            while (toAdd.compareTo(BigInteger.ONE) < 0) {
                toAdd = percentage.divide(BigInteger.valueOf(--machinesAmount));
            }

            BigInteger overLimit = BigInteger.ZERO;

            for (int i = 0; i < machinesAmount; ++i) {
                PlacedMachine placedMachine = machines.get(i);
                BigInteger excess = BigInteger.ZERO;
                if (placedMachine.getIntegrity().compareTo(BigInteger.valueOf(100)) >= 0 || placedMachine.hasInfiniteIntegrity() || placedMachine.getIntegrity().add(toAdd).compareTo(BigInteger.valueOf(100)) > 0) {
                    excess = toAdd.subtract(BigInteger.valueOf(100 - placedMachine.getIntegrity().intValue()));
                    overLimit = overLimit.add(excess);
                }

                if (toAdd.subtract(excess).signum() <= 0) continue;

                BigInteger toRepair = toAdd.subtract(excess);
                amountRepaired = amountRepaired.add(toRepair);

                placedMachine.addIntegrity(toRepair);
            }

            if (overLimit.divide(toAdd).compareTo(BigInteger.valueOf(machinesAmount)) == 0) {
                player.sendMessage(Messages.ZERO_MACHINES_REPAIR);
                return;
            }

            player.getInventory().removeItem(item);
            player.playSound(player.getLocation(), Sound.ANVIL_USE, 0.5f, 0.5f);
            player.sendMessage(StringUtils.replaceEach(Messages.SUCCESSFUL_REPAIRED, new String[]{
                    "{machines}",
                    "{repair}"
            }, new String[]{
                    NumberFormatter.getInstance().formatDecimal(machinesAmount.doubleValue()),
                    NumberFormatter.getInstance().format(amountRepaired.divide(BigInteger.valueOf(machinesAmount)))
            }));
            if (overLimit.signum() > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMachineInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();

        Block block = event.getClickedBlock();
        if (block == null || block.getType().equals(Material.AIR)) return;

        PlacedMachine machine = DataManager.getInstance().getPlacedMachine(block.getLocation());
        if (machine == null) return;

        event.setCancelled(true);

        if (!machine.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        if (event.getItem() != null && event.getItem().getType() != Material.AIR) {
            ItemStack item = event.getItem().clone();
            NBTItem nbt = new NBTItem(item);

            if (nbt.hasKey("MachinesInfiniteFuel")) {
                if (machine.hasInfiniteFuel()) return;

                machine.setInfiniteFuel(true);
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.LAVA_POP, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("MachinesInfiniteRepair")) {
                if (machine.hasInfiniteIntegrity()) return;

                machine.setInfiniteIntegrity(true);
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.ANVIL_USE, 0.5f, 0.5f);
                return;
            }

            if (nbt.hasKey("MachinesFuel")) {
                if (machine.hasInfiniteFuel()) return;

                BigInteger amount = new BigInteger(nbt.getString("MachinesFuel"));

                machine.addFuel(amount);
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.LAVA_POP, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("MachinesRepair")) {
                if (machine.getIntegrity().compareTo(BigInteger.valueOf(100)) >= 0 || machine.hasInfiniteIntegrity()) return;

                BigInteger percentage = new BigInteger(nbt.getString("MachinesRepair"));
                BigInteger overLimit = BigInteger.ZERO;

                if (machine.getIntegrity().add(percentage).compareTo(BigInteger.valueOf(100)) > 0) {
                    overLimit = percentage.subtract(BigInteger.valueOf(100 - machine.getIntegrity().intValue()));
                }

                machine.addIntegrity(percentage.subtract(overLimit));
                item.setAmount(1);

                player.getInventory().removeItem(item);
                player.playSound(player.getLocation(), Sound.ANVIL_USE, 0.5f, 0.5f);
                if (overLimit.signum() > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
                return;
            }
        }

        Menus.getInstance().openMachineMenu(player, machine);
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 0.5f, 2f);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        PlacedMachine machine = DataManager.getInstance().getPlacedMachine(block.getLocation());
        if (machine == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!machine.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        machine.delete();

        ItemStack itemInHand = event.getPlayer().getItemInHand().clone();
        BigInteger toGive = machine.getStack();

        boolean correctPickaxe = false;

        if (itemInHand.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(itemInHand);
            if (nbt.hasKey("MachinesPickaxe")) correctPickaxe = true;
        }

        if (!correctPickaxe) {
            toGive = machine.getStack().subtract(machine.getStack().multiply(BigInteger.valueOf(Settings.TAX_REMOVE_STACK)).divide(BigInteger.valueOf(100)));
        }

        if (toGive.compareTo(machine.getStack()) < 0) {
            player.sendMessage(StringUtils.replaceEach(Messages.INCORRECT_PICKAXE, new String[]{
                    "{lost}"
            }, new String[]{
                    NumberFormatter.getInstance().format(machine.getStack().subtract(toGive))
            }));
        }

        if (machine.hasInfiniteFuel()) player.getInventory().addItem(Items.getInstance().getInfiniteFuel());
        if (machine.hasInfiniteIntegrity()) player.getInventory().addItem(Items.getInstance().getInfiniteRepair());
        if (machine.getDrops().signum() > 0) machine.sellDrops(player);

        player.getInventory().addItem(machine.getMachine().getItem(toGive, machine.getIntegrity()));

        if (machine.getFuel().signum() <= 0) return;

        player.getInventory().addItem(Items.getInstance().getFuel(machine.getFuel()));
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBreakByInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Block block = event.getClickedBlock();
        if (block == null || block.getType().equals(Material.AIR)) return;

        PlacedMachine machine = DataManager.getInstance().getPlacedMachine(block.getLocation());
        if (machine == null) return;

        Player player = event.getPlayer();
        if (player.getItemInHand() == null || player.getItemInHand().getType().equals(Material.AIR)) return;

        ItemStack item = player.getItemInHand();
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey("MachinesPickaxe")) return;

        event.setCancelled(true);

        if (!machine.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        machine.delete();

        if (machine.hasInfiniteFuel()) player.getInventory().addItem(Items.getInstance().getInfiniteFuel());
        if (machine.hasInfiniteIntegrity()) player.getInventory().addItem(Items.getInstance().getInfiniteRepair());
        if (machine.getDrops().signum() > 0) machine.sellDrops(player);

        player.getInventory().addItem(machine.getMachine().getItem(machine.getStack(), machine.getIntegrity()));

        if (machine.getFuel().signum() <= 0) return;

        player.getInventory().addItem(Items.getInstance().getFuel(machine.getFuel()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDespawn(ItemDespawnEvent event) {
        if (!event.getEntity().hasMetadata("***")) return;

        event.setCancelled(true);
    }
}