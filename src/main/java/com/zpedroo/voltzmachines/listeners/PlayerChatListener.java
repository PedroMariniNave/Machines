package com.zpedroo.voltzmachines.listeners;

import br.com.devpaulo.legendchat.api.events.ChatMessageEvent;
import com.zpedroo.voltzmachines.hooks.VaultHook;
import com.zpedroo.voltzmachines.machine.Machine;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.objects.PlayerChat;
import com.zpedroo.voltzmachines.machine.PlayerMachine;
import com.zpedroo.voltzmachines.utils.enums.Action;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import com.zpedroo.voltzmachines.utils.menu.Menus;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;

import static com.zpedroo.voltzmachines.utils.config.Messages.*;
import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class PlayerChatListener implements Listener {

    private static HashMap<Player, PlayerChat> playerChat;

    static {
        playerChat = new HashMap<>(16);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(ChatMessageEvent event) {
        if (!getPlayerChat().containsKey(event.getSender())) return;

        event.setCancelled(true);

        PlayerChat playerChat = getPlayerChat().remove(event.getSender());
        Player player = playerChat.getPlayer();
        String msg = event.getMessage();
        Action action = playerChat.getAction();

        PlayerMachine playerMachine = playerChat.getPlayerMachine();
        Machine machine = playerChat.getMachine();
        switch (action) {
            case BUY_MACHINE -> {
                BigInteger price = playerChat.getPrice();
                BigInteger money = new BigInteger(String.format("%.0f", VaultHook.getMoney(player)));
                BigInteger amount = null;
                if (StringUtils.equals(msg, "*")) {
                    amount = money.divide(price);

                    if (amount.signum() <= 0) {
                        player.sendMessage(BUY_ALL_ZERO);
                        return;
                    }

                    VaultHook.removeMoney(player, amount.doubleValue());
                    player.getInventory().addItem(machine.getItem(amount, 100));
                    player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 0.5f);
                    return;
                }

                amount = NumberFormatter.getInstance().filter(msg);
                if (amount.signum() <= 0) {
                    player.sendMessage(INVALID_AMOUNT);
                    return;
                }

                if (money.compareTo(price.multiply(amount)) < 0) {
                    player.sendMessage(INSUFFICIENT_MONEY);
                    return;
                }

                VaultHook.removeMoney(player, price.multiply(amount).doubleValue());
                player.getInventory().addItem(machine.getItem(amount, 100));
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 0.5f);
            }
            case ADD_FRIEND -> {
                Player target = Bukkit.getPlayer(msg);
                player.sendMessage(WAIT);
                if (target == null) {
                    player.sendMessage(OFFLINE_PLAYER);
                    return;
                }

                Manager manager = playerMachine.getManager(target.getUniqueId());
                if (playerMachine.getOwnerUUID().equals(target.getUniqueId()) || manager != null) {
                    player.sendMessage(HAS_PERMISSION);
                    return;
                }

                playerMachine.getManagers().add(new Manager(target.getUniqueId(), new ArrayList<>(5)));
                playerMachine.setQueueUpdate(true);
                Menus.getInstance().openManagersMenu(player, playerMachine);
            }
            case REMOVE_STACK -> {
                BigInteger stack = playerMachine.getStack();
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

                    playerMachine.removeStack(stack);
                    player.getInventory().addItem(playerMachine.getMachine().getItem(toGive, playerMachine.getIntegrity()));
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
                playerMachine.removeStack(toRemove);
                player.getInventory().addItem(playerMachine.getMachine().getItem(toGive, playerMachine.getIntegrity()));
                player.sendMessage(StringUtils.replaceEach(REMOVE_STACK_SUCCESSFUL, new String[]{
                        "{lost}"
                }, new String[]{
                        NumberFormatter.getInstance().format(toRemove.subtract(toGive))
                }));
            }
        }
    }

    public static HashMap<Player, PlayerChat> getPlayerChat() {
        return playerChat;
    }
}