package com.zpedroo.voltzmachines.utils.builder;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.zpedroo.voltzmachines.VoltzMachines;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class InventoryUtils {

    private static InventoryUtils instance;
    public static InventoryUtils getInstance() { return instance; }

    private Table<Inventory, ItemStack, List<Action>> inventoryActions;

    public InventoryUtils() {
        instance = this;
        this.inventoryActions = HashBasedTable.create();
        VoltzMachines.get().getServer().getPluginManager().registerEvents(new ActionListeners(), VoltzMachines.get()); // register inventory listener
    }

    public void addAction(Inventory inventory, ItemStack item, Runnable action, ActionType type) {
        List<Action> actions = hasAction(inventory, item) ? getActions(inventory, item) : new ArrayList<>(1);
        actions.add(new Action(type, item, action));

        inventoryActions.put(inventory, item, actions);
    }

    public Action getAction(Inventory inventory, ItemStack item, ActionType actionType) {
        if (!hasAction(inventory, item)) return null;

        for (Action action : getActions(inventory, item)) {
            if (action.getType() != actionType) continue;

            return action;
        }

        return null;
    }

    public Boolean hasAction(Inventory inventory) {
        return inventoryActions.containsRow(inventory);
    }

    public Boolean hasAction(Inventory inventory, ItemStack item) {
        return inventoryActions.row(inventory).containsKey(item);
    }

    public List<Action> getInventoryActions(Inventory inventory) {
        if (!hasAction(inventory)) return null;

        List<Action> ret = new ArrayList<>(inventoryActions.row(inventory).values().size());

        for (List<Action> actions : inventoryActions.values()) {
            ret.addAll(actions);
        }

        return ret;
    }

    public List<Action> getActions(Inventory inventory, ItemStack item) {
        return inventoryActions.row(inventory).get(item);
    }

    public static class Action {

        private ActionType type;
        private ItemStack item;
        private Runnable action;

        public Action(ActionType type, ItemStack item, Runnable action) {
            this.type = type;
            this.item = item;
            this.action = action;
        }

        public ActionType getType() {
            return type;
        }

        public Runnable getAction() {
            return action;
        }

        public ItemStack getItem() {
            return item;
        }

        public void run() {
            if (action == null) return;

            action.run();
        }
    }

    private class ActionListeners implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onClick(InventoryClickEvent event) {
            if (!hasAction(event.getInventory())) return;

            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType().equals(Material.AIR)) return;

            Inventory inventory = event.getInventory();
            ItemStack item = event.getCurrentItem().clone();

            Action action = getAction(inventory, item, ActionType.ALL_CLICKS);

            if (action == null) {
                // try to found specific actions for items
                switch (event.getClick()) {
                    case LEFT, SHIFT_LEFT -> action = getAction(inventory, item, ActionType.LEFT_CLICK);
                    case RIGHT, SHIFT_RIGHT -> action = getAction(inventory, item, ActionType.RIGHT_CLICK);
                }
            }

            if (action != null) action.run();
        }
    }

    public enum ActionType {
        LEFT_CLICK,
        RIGHT_CLICK,
        ALL_CLICKS
    }
}