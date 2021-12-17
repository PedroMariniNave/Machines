package com.zpedroo.voltzmachines.listeners;

import com.zpedroo.voltzmachines.hooks.ProtocolLibHook;
import com.zpedroo.voltzmachines.utils.config.Messages;
import com.zpedroo.voltzmachines.utils.menu.Menus;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerGeneralListeners implements Listener {

    private static List<Player> choosingGift;

    static {
        choosingGift = new ArrayList<>(32);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType().equals(Material.AIR)) return;

        ItemStack item = event.getItem().clone();
        NBTItem nbt = new NBTItem(item);
        if (!nbt.hasKey("MachinesGift")) return;

        event.setCancelled(true);

        Player player = event.getPlayer();
        String giftOwner = nbt.getString("GiftOwner");
        if (!StringUtils.equals(player.getUniqueId().toString(), giftOwner)) {
            player.sendMessage(StringUtils.replaceEach(Messages.INCORRECT_GIFT_OWNER, new String[]{
                    "{owner}"
            }, new String[]{
                    Bukkit.getOfflinePlayer(UUID.fromString(giftOwner)).getName()
            }));
            return;
        }

        Menus.getInstance().openGiftMenu(player);
        choosingGift.add(player);
        player.playSound(player.getLocation(), Sound.ENDERDRAGON_WINGS, 0.5f, 10f);

        item.setAmount(1);
        player.getInventory().removeItem(item);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        ProtocolLibHook.removeViewer(event.getPlayer());
    }

    /*
    @EventHandler(priority = EventPriority.MONITOR)
    public void onClose(InventoryCloseEvent event) {
        if (!choosingGift.contains(event.getPlayer())) return;

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();
        new BukkitRunnable() {
            @Override
            public void run() {
                if (player == null || !player.isOnline()) return;
                if (inventory == null || inventory.getType() != InventoryType.CHEST) return;

                player.openInventory(inventory);
            }
        }.runTaskLater(VoltzMachines.get(), 1L);
    }
     */

    public static List<Player> getChoosingGift() {
        return choosingGift;
    }
}