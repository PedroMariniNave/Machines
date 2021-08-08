package com.zpedroo.voltzmachines.listeners;

import com.zpedroo.voltzmachines.hooks.WorldGuardHook;
import com.zpedroo.voltzmachines.machine.Machine;
import com.zpedroo.voltzmachines.machine.PlayerMachine;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.objects.Manager;
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

public class MachineListeners implements Listener {

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getItemInHand() == null || event.getItemInHand().getType().equals(Material.AIR)) return;

        final ItemStack item = event.getItemInHand().clone();
        NBTItem nbt = new NBTItem(item);
        if (nbt.hasKey("MachinesFuel") || nbt.hasKey("MachinesInfiniteFuel") || nbt.hasKey("MachinesRepair")) event.setCancelled(true);
        if (!nbt.hasKey("MachinesAmount")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        Block block = event.getBlockPlaced();

        if (!WorldGuardHook.getInstance().canBuild(player, block.getLocation())) return;

        Machine machine = getManager().getDataCache().getMachines().get(nbt.getString("MachinesType").toUpperCase());
        if (machine == null) return;

        if (!StringUtils.equals(machine.getPermission(), "NULL")) {
            if (!player.hasPermission(machine.getPermission())) {
                player.sendMessage(Messages.MACHINE_PERMISSION);
                return;
            }
        }

        BigInteger stack = new BigInteger(nbt.getString("MachinesAmount"));
        Object[] objects = getManager().getNearMachines(player, block, stack, machine.getType());
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

                            PlayerMachine nearMachine = getManager().getMachine(blocks.getLocation());
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

            playerMachine = new PlayerMachine(block.getLocation(), player.getUniqueId(), stack.subtract(overLimit), BigInteger.ZERO, BigInteger.ZERO, integrity, machine, new ArrayList<>(), false);
            playerMachine.cache();
            playerMachine.setQueueUpdate(true);
        }

        item.setAmount(1);
        player.getInventory().removeItem(item);
        if (overLimit.compareTo(BigInteger.ZERO) >= 1) player.getInventory().addItem(machine.getItem(overLimit, integrity));
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (!(event.getAction() == Action.RIGHT_CLICK_BLOCK)) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        PlayerMachine machine = getManager().getMachine(block.getLocation());
        if (machine == null) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        if (!machine.canInteract(player)) {
            player.sendMessage(Messages.NEED_PERMISSION);
            return;
        }

        ItemStack item = player.getItemInHand().clone();

        if (item.getType() != Material.AIR) {
            NBTItem nbt = new NBTItem(item);

            if (nbt.hasKey("MachinesInfiniteFuel")) {
                if (machine.isInfinite()) return;

                machine.setInfinite(true);
                item.setAmount(1);
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
                player.getInventory().removeItem(item);
                return;
            }

            if (nbt.hasKey("MachinesFuel")) {
                BigInteger amount = new BigInteger(nbt.getString("MachinesFuel"));

                machine.addFuel(amount);
                item.setAmount(1);
                player.playSound(player.getLocation(), Sound.BLOCK_LAVA_AMBIENT, 0.5f, 10f);
                player.getInventory().removeItem(item);
                return;
            }

            if (nbt.hasKey("MachinesRepair")) {
                Integer percentage = nbt.getInteger("MachinesRepair");

                Integer overLimit = 0;

                if (machine.getIntegrity() >= 100) return;

                if (machine.getIntegrity() + percentage > 100) {
                    overLimit = percentage - (100 - machine.getIntegrity());
                }

                machine.addIntegrity(percentage - overLimit);
                item.setAmount(1);
                player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 0.5f, 0.5f);
                player.getInventory().removeItem(item);
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

        PlayerMachine machine = getManager().getMachine(block.getLocation());
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

        machine.delete();
        player.getInventory().addItem(machine.getMachine().getItem(toGive, machine.getIntegrity()));
        if (toGive.compareTo(machine.getStack()) < 0) {
            player.sendMessage(StringUtils.replaceEach(Messages.INCORRECT_PICKAXE, new String[]{
                    "{lost}"
            }, new String[]{
                    NumberFormatter.getInstance().format(machine.getStack().subtract(toGive))
            }));
        }

        if (machine.isInfinite()) {
            player.getInventory().addItem(Items.getInstance().getInfiniteFuel());
        }

        if (machine.getFuel().signum() <= 0) return;

        player.getInventory().addItem(Items.getInstance().getFuel(machine.getFuel()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDespawn(ItemDespawnEvent event) {
        if (StringUtils.equals(event.getEntity().getName(), "Machine Item")) {
            event.setCancelled(true);
        }
    }

    private MachineManager getManager() {
        return MachineManager.getInstance();
    }
}