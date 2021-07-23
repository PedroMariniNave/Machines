package com.zpedroo.voltzmachines.commands;

import com.zpedroo.voltzmachines.machine.Machine;
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

import java.math.BigInteger;

public class MachinesCmd implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = sender instanceof Player ? (Player) sender : null;
        Player target = null;
        BigInteger amount = null;

        if (args.length > 0 && sender.hasPermission("machines.admin")) {
            String arg = args[0].toUpperCase();

            switch (arg) {
                case "GIVE":
                    if (args.length < 4) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    Machine machine = MachineManager.getInstance().getMachine(args[2]);

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

                    target.getInventory().addItem(machine.getItem(amount, 100));
                    return true;
                case "FUEL":
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
                case "PICKAXE":
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

                    target.getInventory().addItem(Items.getInstance().getRepair(percentage.intValue()));
                    return true;
                case "PRESENT":
                    if (args.length < 2) {
                        sender.sendMessage(Messages.MACHINE_USAGE);
                        return true;
                    }

                    target = Bukkit.getPlayer(args[1]);

                    if (target == null) {
                        sender.sendMessage(Messages.OFFLINE_PLAYER);
                        return true;
                    }

                    target.getInventory().addItem(Items.getInstance().getPresent());
                    return true;
                case "UPDATE":
                    MachineManager.getInstance().updatePrices(true);
                    return true;
            }
        }

        if (player == null) return true;

        Menus.getInstance().openMainMenu(player);
        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_SADDLE, 0.5f, 10f);
        return false;
    }
}