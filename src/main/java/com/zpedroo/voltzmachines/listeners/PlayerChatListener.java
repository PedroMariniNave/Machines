package com.zpedroo.voltzmachines.listeners;

import com.zpedroo.multieconomy.api.CurrencyAPI;
import com.zpedroo.multieconomy.objects.Currency;
import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlacedMachine;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.objects.PlayerChat;
import com.zpedroo.voltzmachines.utils.config.Messages;
import com.zpedroo.voltzmachines.enums.PlayerAction;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import com.zpedroo.voltzmachines.utils.item.Items;
import com.zpedroo.voltzmachines.utils.menu.Menus;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.math.BigInteger;
import java.util.*;

import static com.zpedroo.voltzmachines.utils.config.Messages.*;
import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class PlayerChatListener implements Listener {

    private static Map<Player, PlayerChat> playerChat;

    static {
        playerChat = new HashMap<>(16);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!playerChat.containsKey(event.getPlayer())) return;

        event.setCancelled(true);

        // sync method
        new BukkitRunnable() {
            @Override
            public void run() {
                PlayerChat playerChat = getPlayerChat().remove(event.getPlayer());
                Player player = playerChat.getPlayer();
                String msg = event.getMessage();
                PlayerAction playerAction = playerChat.getAction();

                PlacedMachine placedMachine = playerChat.getPlacedMachine();
                BigInteger price = playerChat.getPrice();
                Currency currency = playerChat.getCurrency();

                ItemStack itemToGive = null;

                BigInteger selectedAmount = null;
                if (StringUtils.equals(msg, "*")) {
                    selectedAmount = CurrencyAPI.getCurrencyAmount(player, currency).divide(price);
                } else {
                    selectedAmount = NumberFormatter.getInstance().filter(msg);
                }
                switch (playerAction) {
                    case BUY_FUEL:
                        itemToGive = Items.getInstance().getFuel(selectedAmount);
                        buy(player, itemToGive, currency, price, selectedAmount);
                        return;
                    case BUY_PICKAXE:
                        itemToGive = Items.getInstance().getPickaxe();
                        buy(player, itemToGive, currency, price, selectedAmount);
                        return;
                    case BUY_REPAIR:
                        itemToGive = Items.getInstance().getRepair(selectedAmount);
                        buy(player, itemToGive, currency, price, selectedAmount);
                        return;
                    case BUY_MACHINE:
                        Machine machine = playerChat.getMachine();
                        itemToGive = machine.getItem(selectedAmount, BigInteger.valueOf(100));
                        buy(player, itemToGive, currency, price, selectedAmount);
                        return;
                    case ADD_FRIEND:
                        Player target = Bukkit.getPlayer(msg);
                        player.sendMessage(WAIT);
                        if (target == null) {
                            player.sendMessage(OFFLINE_PLAYER);
                            return;
                        }

                        Manager manager = placedMachine.getManager(target.getUniqueId());
                        if (placedMachine.getOwnerUUID().equals(target.getUniqueId()) || manager != null) {
                            player.sendMessage(HAS_PERMISSION);
                            return;
                        }

                        placedMachine.getManagers().add(new Manager(target.getUniqueId(), new ArrayList<>(5)));
                        placedMachine.setUpdate(true);
                        Menus.getInstance().openManagersMenu(player, placedMachine);
                        return;
                    case REMOVE_STACK:
                        BigInteger stack = placedMachine.getStack();
                        BigInteger tax = BigInteger.valueOf(TAX_REMOVE_STACK);
                        if (StringUtils.equals(msg, "*")) {
                            if (stack.compareTo(tax) < 0) {
                                player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_MIN, new String[]{
                                        "{tax}"
                                }, new String[]{
                                        String.valueOf(tax)
                                }));
                                return;
                            }

                            BigInteger toGive = stack.subtract(stack.multiply(tax).divide(BigInteger.valueOf(100)));

                            placedMachine.removeStack(stack);
                            player.getInventory().addItem(placedMachine.getMachine().getItem(toGive, placedMachine.getIntegrity()));
                            player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_SUCCESSFUL, new String[]{
                                    "{lost}"
                            }, new String[]{
                                    NumberFormatter.getInstance().format(stack.subtract(toGive))
                            }));
                            return;
                        }

                        BigInteger toRemove = NumberFormatter.getInstance().filter(msg);
                        if (toRemove.signum() <= 0) {
                            player.sendMessage(INVALID_AMOUNT);
                            return;
                        }

                        if (toRemove.compareTo(stack) > 0) toRemove = stack;
                        if (toRemove.compareTo(tax) < 0) {
                            player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_MIN, new String[]{
                                    "{tax}"
                            }, new String[]{
                                    String.valueOf(tax)
                            }));
                            return;
                        }

                        BigInteger toGive = toRemove.subtract(toRemove.multiply(tax).divide(BigInteger.valueOf(100)));
                        placedMachine.removeStack(toRemove);
                        player.getInventory().addItem(placedMachine.getMachine().getItem(toGive, placedMachine.getIntegrity()));
                        player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_SUCCESSFUL, new String[]{
                                "{lost}"
                        }, new String[]{
                                NumberFormatter.getInstance().format(toRemove.subtract(toGive))
                        }));
                }
            }
        }.runTaskLater(VoltzMachines.get(), 0L);
    }

    private void buy(Player player, ItemStack item, Currency currency, BigInteger price, BigInteger amount) {
        if (amount.signum() <= 0) {
            player.sendMessage(INVALID_AMOUNT);
            return;
        }

        BigInteger currencyAmount = CurrencyAPI.getCurrencyAmount(player, currency);
        BigInteger finalPrice = price.multiply(amount);

        if (currencyAmount.compareTo(finalPrice) < 0) {
            player.sendMessage(StringUtils.replaceEach(INSUFFICIENT_CURRENCY, new String[]{
                    "{has}",
                    "{need}"
            }, new String[]{
                    NumberFormatter.getInstance().format(currencyAmount),
                    NumberFormatter.getInstance().format(finalPrice)
            }));
            return;
        }

        CurrencyAPI.removeCurrencyAmount(player, currency, finalPrice);

        player.getInventory().addItem(item);

        for (String purchasedMsg : Messages.SUCCESSFUL_PURCHASED) {
            player.sendMessage(StringUtils.replaceEach(purchasedMsg, new String[]{
                    "{item}",
                    "{amount}",
                    "{price}"
            }, new String[]{
                    item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : item.getType().toString(),
                    NumberFormatter.getInstance().format(amount),
                    StringUtils.replaceEach(currency.getAmountDisplay(), new String[]{ "{amount}" }, new String[]{ NumberFormatter.getInstance().format(finalPrice) })
            }));
        }

        player.playSound(player.getLocation(), Sound.ITEM_PICKUP, 0.5f, 100f);
    }

    public static Map<Player, PlayerChat> getPlayerChat() {
        return playerChat;
    }
}