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

    private Table<Inventory, Object, List<Action>> inventoryActions;

    public InventoryUtils() {
        instance = this;
        this.inventoryActions = HashBasedTable.create();
        VoltzMachines.get().getServer().getPluginManager().registerEvents(new ActionListeners(), VoltzMachines.get()); // register inventory listener
    }

    public void addAction(Inventory inventory, Object object, Runnable action, ActionType type) {
        List<Action> actions = hasAction(inventory, object) ? getActions(inventory, object) : new ArrayList<>(2);
        actions.add(new Action(type, object, action));

        inventoryActions.put(inventory, object, actions);
    }

    public Action getAction(Inventory inventory, Object object, ActionType actionType) {
        if (!hasAction(inventory, object)) return null;

        for (Action action : getActions(inventory, object)) {
            if (action.getType() != actionType) continue;

            return action;
        }

        return null;
    }

    public Boolean hasAction(Inventory inventory) {
        return inventoryActions.containsRow(inventory);
    }

    public Boolean hasAction(Inventory inventory, Object object) {
        return inventoryActions.row(inventory).containsKey(object);
    }

    public List<Action> getInventoryActions(Inventory inventory) {
        if (!hasAction(inventory)) return null;

        List<Action> ret = new ArrayList<>(inventoryActions.row(inventory).values().size());

        for (List<Action> actions : inventoryActions.values()) {
            ret.addAll(actions);
        }

        return ret;
    }

    public List<Action> getActions(Inventory inventory, Object object) {
        return inventoryActions.row(inventory).get(object);
    }

    public static class Action {

        private ActionType type;
        private Object object;
        private Runnable action;

        public Action(ActionType type, Object object, Runnable action) {
            this.type = type;
            this.object = object;
            this.action = action;
        }

        public ActionType getType() {
            return type;
        }

        public Runnable getAction() {
            return action;
        }

        public Object getObject() {
            return object;
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
            int slot = event.getSlot();

            Action action = null;

            try {
                action = getAction(inventory, item, ActionType.ALL_CLICKS);

                if (action == null) {
                    // try to found specific actions for items
                    switch (event.getClick()) {
                        case LEFT, SHIFT_LEFT -> action = getAction(inventory, item, ActionType.LEFT_CLICK);
                        case RIGHT, SHIFT_RIGHT -> action = getAction(event.getInventory(), item, ActionType.RIGHT_CLICK);
                    }
                }
            } finally {
                if (action != null) return;

                action = getAction(inventory, slot, ActionType.ALL_CLICKS);

                if (action == null) {
                    // try to found specific actions for items
                    switch (event.getClick()) {
                        case LEFT, SHIFT_LEFT -> action = getAction(inventory, slot, ActionType.LEFT_CLICK);
                        case RIGHT, SHIFT_RIGHT -> action = getAction(event.getInventory(), slot, ActionType.RIGHT_CLICK);
                    }
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