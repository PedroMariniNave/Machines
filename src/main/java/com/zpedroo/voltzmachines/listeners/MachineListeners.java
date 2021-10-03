package com.zpedroo.voltzmachines.listeners;

import com.zpedroo.voltzmachines.hooks.WorldGuardHook;
import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlayerMachine;
import com.zpedroo.voltzmachines.utils.config.Messages;
import com.zpedroo.voltzmachines.utils.config.Settings;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import com.zpedroo.voltzmachines.utils.item.Items;
import com.zpedroo.voltzmachines.utils.menu.Menus;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
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

public class MachineListeners implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        final ItemStack item = event.getItemInHand();
        if (item.getType().equals(Material.AIR)) return;

        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey("MachinesFuel") || nbt.hasKey("MachinesInfiniteFuel") || nbt.hasKey("MachinesRepair")) event.setCancelled(true);
        if (!nbt.hasKey("MachinesAmount")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (!WorldGuardHook.getInstance().canBuild(player, block.getLocation())) return;

        Machine machine = DataManager.getInstance().getMachine(nbt.getString("MachinesType").toUpperCase());
        if (machine == null) return;

        if (!StringUtils.equals(machine.getPermission(), "NULL")) {
            if (!player.hasPermission(machine.getPermission())) {
                player.sendMessage(Messages.MACHINE_PERMISSION);
                return;
            }
        }

        BigInteger stack = new BigInteger(nbt.getString("MachinesAmount"));
        Object[] objects = MachineManager.getInstance().getNearMachines(player, block, stack, machine.getType());
        PlayerMachine playerMachine = objects != null ? (PlayerMachine) objects[0] : null;
        BigInteger overLimit = null;
        Integer integrity = nbt.getInteger("MachinesIntegrity");

        if (playerMachine != null) {
            if (!playerMachine.canInteract(player)) return;

            overLimit = (BigInteger) objects[1];
            playerMachine.addStack(stack.subtract(overLimit));
        } else {
            int radius = Settings.STACK_RADIUS;

            if (radius > 0) {
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (!WorldGuardHook.getInstance().canBuild(player, new Location(block.getWorld(), x, y, z))) continue;

                            Block blocks = block.getRelative(x, y, z);
                            if (blocks.getType().equals(Material.AIR)) continue;

                            PlayerMachine nearMachine = DataManager.getInstance().getMachine(blocks.getLocation());
                            if (nearMachine == null) continue;

                            player.sendMessage(Messages.NEAR_MACHINE);
                            return;
                        }
                    }
                }
            }

            if (machine.getMaxStack().signum() > 0 && stack.compareTo(machine.getMaxStack()) > 0) {
                overLimit = stack.subtract(machine.getMaxStack());
            } else overLimit = BigInteger.ZERO;

            playerMachine = new PlayerMachine(block.getLocation(), player.getUniqueId(), stack.subtract(overLimit), BigInteger.ZERO, BigInteger.ZERO, integrity, machine, new ArrayList<>(), false, false);
            playerMachine.cache();
            playerMachine.setQueueUpdate(true);
        }

        item.setAmount(item.getAmount() - 1);

        if (overLimit.compareTo(BigInteger.ZERO) >= 1) player.getInventory().addItem(machine.getItem(overLimit, integrity));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        if (item == null || item.getType().equals(Material.AIR)) return;

        PlayerMachine machine = null;
        if (event.getClickedBlock() != null) machine = DataManager.getInstance().getMachine(event.getClickedBlock().getLocation());
        if (machine != null) return;

        NBTItem nbt = new NBTItem(item);

        List<PlayerMachine> machines = DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId());

        if (nbt.hasKey("MachinesFuel")) {

            event.setCancelled(true);

            if (machines.isEmpty()) {
                player.sendMessage(Messages.ZERO_MACHINES_FUEL);
                return;
            }

            Integer machinesWithInfiniteEnergy = 0;
            for (PlayerMachine playerMachine : machines) {
                if (playerMachine.hasInfiniteFuel()) ++machinesWithInfiniteEnergy;
            }

            Integer machinesAmount = machines.size();
            BigInteger amount = new BigInteger(nbt.getString("MachinesFuel"));
            BigInteger toAdd = amount.divide(BigInteger.valueOf(machinesAmount - machinesWithInfiniteEnergy));
            while (toAdd.compareTo(BigInteger.ONE) < 0) {
                toAdd = amount.divide(BigInteger.valueOf(--machinesAmount));
            }

            for (int i = 0; i < machinesAmount; ++i) {
                PlayerMachine playerMachine = machines.get(i);
                if (playerMachine.hasInfiniteFuel()) continue;

                playerMachine.addFuel(toAdd);
            }

            item.setAmount(item.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
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
            Integer percentage = nbt.getInteger("MachinesRepair");
            Integer toAdd = percentage / machinesAmount;
            Integer amountRepaired = 0;
            while (toAdd < 1) {
                toAdd = percentage / --machinesAmount;
            }

            Integer overLimit = 0;

            for (int i = 0; i < machinesAmount; ++i) {
                PlayerMachine playerMachine = machines.get(i);
                Integer excess = 0;
                if (playerMachine.getIntegrity() >= 100 || playerMachine.hasInfiniteIntegrity() || playerMachine.getIntegrity() + toAdd > 100) {
                    excess = toAdd - (100 - playerMachine.getIntegrity());
                    overLimit += excess;
                }

                if (toAdd - excess <= 0) continue;

                Integer toRepair = toAdd - excess;
                amountRepaired += toRepair;

                playerMachine.addIntegrity(toRepair);
            }

            if (overLimit / toAdd == machinesAmount) return;

            item.setAmount(item.getAmount() - 1);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
            player.sendMessage(StringUtils.replaceEach(Messages.SUCCESSFUL_REPAIRED, new String[]{
                    "{machines}",
                    "{repair}"
            }, new String[]{
                    NumberFormatter.getInstance().formatDecimal(machinesAmount.doubleValue()),
                    NumberFormatter.getInstance().format(BigInteger.valueOf(amountRepaired / machinesAmount))
            }));
            if (overLimit > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onMachineInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        Block block = event.getClickedBlock();
        if (block == null || block.getType().equals(Material.AIR)) return;

        PlayerMachine machine = DataManager.getInstance().getMachine(block.getLocation());
        if (machine == null) return;

        event.setCancelled(true);

        if (!machine.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        if (item != null && item.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(item);

            if (nbt.hasKey("MachinesInfiniteFuel")) {
                if (machine.hasInfiniteFuel()) return;

                machine.setInfiniteFuel(true);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("MachinesInfiniteRepair")) {
                if (machine.hasInfiniteIntegrity()) return;

                machine.setInfiniteIntegrity(true);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
                return;
            }

            if (nbt.hasKey("MachinesFuel")) {
                if (machine.hasInfiniteFuel()) return;

                BigInteger amount = new BigInteger(nbt.getString("MachinesFuel"));

                machine.addFuel(amount);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
                return;
            }

            if (nbt.hasKey("MachinesRepair")) {
                if (machine.getIntegrity() >= 100 || machine.hasInfiniteIntegrity()) return;

                Integer percentage = nbt.getInteger("MachinesRepair");
                Integer overLimit = 0;

                if (machine.getIntegrity() + percentage > 100) {
                    overLimit = percentage - (100 - machine.getIntegrity());
                }

                machine.addIntegrity(percentage - overLimit);
                item.setAmount(item.getAmount() - 1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
                if (overLimit > 0) player.getInventory().addItem(Items.getInstance().getRepair(overLimit));
                return;
            }
        }

        Menus.getInstance().openMachineMenu(player, machine);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.5f, 2f);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        PlayerMachine machine = DataManager.getInstance().getMachine(block.getLocation());
        if (machine == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!machine.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        ItemStack item = event.getPlayer().getItemInHand().clone();
        BigInteger toGive = machine.getStack();

        Boolean correctPickaxe = false;

        if (item.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(item);

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

        if (machine.hasInfiniteFuel()) {
            player.getInventory().addItem(Items.getInstance().getInfiniteFuel());
        }

        if (machine.hasInfiniteIntegrity()) {
            player.getInventory().addItem(Items.getInstance().getInfiniteRepair());
        }

        if (machine.getDrops().signum() > 0) machine.sellDrops(player);

        machine.delete();
        player.getInventory().addItem(machine.getMachine().getItem(toGive, machine.getIntegrity()));

        if (machine.getFuel().signum() <= 0) return;

        player.getInventory().addItem(Items.getInstance().getFuel(machine.getFuel()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDespawn(ItemDespawnEvent event) {
        if (!event.getEntity().hasMetadata("Machine Item")) return;

        event.setCancelled(true);
    }
}