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

    /**
     * Constructor
     */
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

    /**
     * Method to build without pages
     *
     * @return InventoryBuilder
     */
    public static InventoryBuilder build(Player player, Inventory defaultInventory, String title, List<ItemBuilder> builders) {
        return build(player, defaultInventory, title, builders, null, null, null, null);
    }

    /**
     * Method to build with pages
     *
     * @return InventoryBuilder
     */
    public static InventoryBuilder build(Player player, Inventory defaultInventory, String title, List<ItemBuilder> builders, Integer nextPageSlot, Integer previousPageSlot, ItemStack nextPageItem, ItemStack previousPageItem) {
        return new InventoryBuilder(player, defaultInventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    /**
     * @return player
     */
    private Player getPlayer() {
        return player;
    }

    /**
     * @return default inventory
     */
    private Inventory getDefaultInventory() {
        return defaultInventory;
    }

    /**
     * @return title of inventory
     */
    private String getTitle() {
        return title;
    }

    /**
     * @return size of inventory
     */
    private Integer getSize() {
        return size;
    }

    /**
     * @return previous page slot
     */
    private Integer getPreviousPageSlot() {
        return previousPageSlot;
    }

    /**
     * @return next page slot
     */
    private Integer getNextPageSlot() {
        return nextPageSlot;
    }

    /**
     * @return previous page item
     */
    private ItemStack getPreviousPageItem() {
        return previousPageItem;
    }

    /**
     * @return next page item
     */
    private ItemStack getNextPageItem() {
        return nextPageItem;
    }

    /**
     * @return Map with all inventory pages
     *
     * Key = Page number
     * Value = Inventory
     */
    private HashMap<Integer, Inventory> getInventories() {
        return inventories;
    }

    /**
     * Method to open a page
     *
     * @param page Inventory page
     */
    public void open(Integer page) {
        getPlayer().openInventory(getInventories().get(page));
    }

    /**
     * Method to create all inventories and pages
     *
     * @param builders List of ItemBuilders
     * @param page Current page
     */
    private void create(List<ItemBuilder> builders, Integer page) {
        Inventory inventory = Bukkit.createInventory(null, getSize(), getTitle());

        // clone all items
        for (int slot = 0; slot < getDefaultInventory().getSize(); ++slot) {
            ItemStack item = getDefaultInventory().getItem(slot);
            if (item == null || item.getType().equals(Material.AIR)) continue;

            inventory.setItem(slot, item);
        }

        if (InventoryUtils.getInstance().hasAction(getDefaultInventory())) {
            // clone all actions
            for (InventoryUtils.Action action : InventoryUtils.getInstance().getActions(getDefaultInventory())) {
                if (action == null) continue;

                InventoryUtils.getInstance().addAction(inventory, action.getItem(), action.getAction(), action.getClick());
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
                Validate.notNull(getNextPageItem(), "Next page item cannot be null!");
                Validate.notNull(getNextPageSlot(), "Next page slot cannot be null!");

                ItemStack item = getNextPageItem();
                Integer slot = getNextPageSlot();

                InventoryUtils.getInstance().addAction(inventory, item, () -> {
                    open(page+1);
                }, InventoryUtils.ActionClick.ALL);

                inventory.setItem(slot, item);

                getInventories().put(page, inventory);
                create(remaining, page+1);
                break;
            }

            ItemBuilder builder = builders.get(i);
            ItemStack item = builder.build();
            Integer slot = builder.getSlot();
            List<InventoryUtils.Action> actions = builder.getActions();

            if (actions != null && actions.size() != 0) {
                for (InventoryUtils.Action action : actions) {
                    InventoryUtils.getInstance().addAction(inventory, item, action.getAction(), action.getClick());
                }
            }

            inventory.setItem(slot, item);
            remaining.remove(builder);
        }

        if (page != 1) {
            Validate.notNull(getPreviousPageItem(), "Previous page item cannot be null!");
            Validate.notNull(getPreviousPageSlot(), "Previous page slot cannot be null!");

            ItemStack item = getPreviousPageItem();
            Integer slot = getPreviousPageSlot();

            InventoryUtils.getInstance().addAction(inventory, item, () -> {
                open(page-1);
            }, InventoryUtils.ActionClick.ALL);

            inventory.setItem(slot, item);
        }

        getInventories().put(page, inventory);
    }
}