package com.zpedroo.voltzmachines.utils.builder;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class InventoryBuilder {

    private Player player;
    private String title;
    private Integer size;
    private Integer nextPageSlot;
    private Integer previousPageSlot;
    private ItemStack nextPageItem;
    private ItemStack previousPageItem;
    private Inventory defaultInventory;
    private HashMap<Integer, Inventory> inventories;

    public InventoryBuilder(Player player, Inventory defaultInventory, String title, List<ItemBuilder> builders, Integer nextPageSlot, Integer previousPageSlot, ItemStack nextPageItem, ItemStack previousPageItem) {
        this.player = player;
        this.defaultInventory = defaultInventory;
        this.title = title;
        this.size = defaultInventory.getSize();
        this.nextPageSlot = nextPageSlot;
        this.previousPageSlot = previousPageSlot;
        this.nextPageItem = nextPageItem;
        this.previousPageItem = previousPageItem;
        this.inventories = new HashMap<>(32);
        this.create(builders, 1);
        this.open(1);
    }

    public static InventoryBuilder build(Player player, Inventory defaultInventory, String title, List<ItemBuilder> builders) {
        return build(player, defaultInventory, title, builders, null, null, null, null);
    }

    public static InventoryBuilder build(Player player, Inventory defaultInventory, String title, List<ItemBuilder> builders, Integer nextPageSlot, Integer previousPageSlot, ItemStack nextPageItem, ItemStack previousPageItem) {
        return new InventoryBuilder(player, defaultInventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    private Player getPlayer() {
        return player;
    }

    private Inventory getDefaultInventory() {
        return defaultInventory;
    }

    private String getTitle() {
        return title;
    }

    private Integer getSize() {
        return size;
    }

    private Integer getPreviousPageSlot() {
        return previousPageSlot;
    }

    private Integer getNextPageSlot() {
        return nextPageSlot;
    }

    private ItemStack getPreviousPageItem() {
        return previousPageItem;
    }

    private ItemStack getNextPageItem() {
        return nextPageItem;
    }

    private HashMap<Integer, Inventory> getInventories() {
        return inventories;
    }

    public void open(Integer page) {
        getPlayer().openInventory(getInventories().get(page));
    }

    private void create(List<ItemBuilder> builders, Integer page) {
        Inventory inventory = Bukkit.createInventory(null, size, title);

        // clone all items
        for (int slot = 0; slot < defaultInventory.getSize(); ++slot) {
            ItemStack item = defaultInventory.getItem(slot);
            if (item == null || item.getType().equals(Material.AIR)) continue;

            inventory.setItem(slot, item);
        }

        if (InventoryUtils.getInstance().hasAction(getDefaultInventory())) {
            // clone all actions
            for (InventoryUtils.Action action : InventoryUtils.getInstance().getInventoryActions(defaultInventory)) {
                if (action == null) continue;

                InventoryUtils.getInstance().addAction(inventory, action.getObject(), action.getAction(), action.getType());
            }
        }

        List<ItemBuilder> remaining = new ArrayList<>(builders);
        // Slots can be duplicated because of multiple pages, but Sets don't accept
        // repeated values, so we can get the amount of slots per page with that.
        Set<Integer> slots = new HashSet<>(builders.size());
        new HashSet<>(builders).forEach(builder -> {
            slots.add(builder.getSlot());
        });
        int itemsPerPage = slots.size();

        for (int i = 0; i < builders.size(); ++i) {
            if (i >= itemsPerPage) {
                Validate.notNull(nextPageItem, "Next page item cannot be null!");
                Validate.notNull(nextPageSlot, "Next page slot cannot be null!");

                InventoryUtils.getInstance().addAction(inventory, nextPageSlot, () -> {
                    open(page+1);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                inventory.setItem(nextPageSlot, nextPageItem);

                inventories.put(page, inventory);
                create(remaining, page+1);
                break;
            }

            ItemBuilder builder = builders.get(i);
            ItemStack item = builder.build();
            Integer slot = builder.getSlot();
            List<InventoryUtils.Action> actions = builder.getActions();

            if (actions != null && actions.size() != 0) {
                for (InventoryUtils.Action action : actions) {
                    InventoryUtils.getInstance().addAction(inventory, slot, action.getAction(), action.getType());
                }
            }

            inventory.setItem(slot, item);
            remaining.remove(builder);
        }

        if (page != 1) {
            Validate.notNull(getPreviousPageItem(), "Previous page item cannot be null!");
            Validate.notNull(getPreviousPageSlot(), "Previous page slot cannot be null!");

            InventoryUtils.getInstance().addAction(inventory, previousPageSlot, () -> {
                open(page-1);
            }, InventoryUtils.ActionType.ALL_CLICKS);

            inventory.setItem(previousPageSlot, previousPageItem);
        }

        inventories.put(page, inventory);
    }
}