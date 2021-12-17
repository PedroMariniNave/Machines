package com.zpedroo.voltzmachines.commands;

import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.utils.config.Messages;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import com.zpedroo.voltzmachines.utils.item.Items;
import com.zpedroo.voltzmachines.utils.menu.Menus;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.math.BigInteger;

public class MachinesCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;

        if (args.length > 0) {
            Player target = null;
            BigInteger amount = null;
            ItemStack item = null;

            switch (args[0].toUpperCase()) {
                case "SHOP":
                    if (player == null) return true;

                    Menus.getInstance().openShopMenu(player);
                    return true;
                case "TOP":
                    if (player == null) return true;

                    Menus.getInstance().openTopMachinesMenu(player);
                    return true;
                case "GIVE":
                    if (!sender.hasPermission("machines.admin")) break;

                    if (args.length < 4) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    Machine machine = DataManager.getInstance().getMachine(args[2]);

                    if (machine == null) {
                        sender.sendMessage(Messages.INVALID_MACHINE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[3]);

                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(machine.getItem(amount, BigInteger.valueOf(100)));
                    return true;
                case "FUEL":
                    if (!sender.hasPermission("machines.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);

                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(Items.getInstance().getFuel(amount));
                    return true;
                case "INFINITE_FUEL":
                    if (!sender.hasPermission("machines.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);

                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    if (amount.compareTo(BigInteger.valueOf(2304)) > 0) amount = BigInteger.valueOf(2304);

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    item = Items.getInstance().getInfiniteFuel();
                    item.setAmount(amount.intValue());

                    target.getInventory().addItem(item);
                    return true;
                case "PICKAXE":
                    if (!sender.hasPermission("machines.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);

                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    for (int i = 0; i < amount.intValue(); ++i) {
                        target.getInventory().addItem(Items.getInstance().getPickaxe());
                    }
                    return true;
                case "REPAIR":
                    if (!sender.hasPermission("machines.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    BigInteger percentage = NumberFormatter.getInstance().filter(args[2]);

                    if (percentage.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(Items.getInstance().getRepair(percentage));
                    return true;
                case "INFINITE_REPAIR":
                    if (!sender.hasPermission("machines.admin")) break;

                    if (args.length < 3) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    amount = NumberFormatter.getInstance().filter(args[2]);

                    if (amount.signum() <= 0) {
                        sender.sendMessage(Messages.INVALID_AMOUNT);
                        return true;
                    }

                    if (amount.compareTo(BigInteger.valueOf(2304)) > 0) amount = BigInteger.valueOf(2304);

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    item = Items.getInstance().getInfiniteRepair();
                    item.setAmount(amount.intValue());

                    target.getInventory().addItem(item);
                    return true;
                case "GIFT":
                    if (!sender.hasPermission("machines.admin")) break;

                    if (args.length < 2) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(Items.getInstance().getGift(target));
                    return true;
                case "UPDATE":
                    if (!sender.hasPermission("machines.admin")) break;

                    MachineManager.updatePrices(true);
                    return true;
            }
        }

        if (player == null) return true;

        Menus.getInstance().openMainMenu(player);
        player.playSound(player.getLocation(), Sound.HORSE_SADDLE, 0.5f, 10f);
        return false;
    }
}