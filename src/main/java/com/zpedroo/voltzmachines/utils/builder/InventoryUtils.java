package com.zpedroo.voltzmachines.utils.builder;

import com.zpedroo.voltzmachines.VoltzMachines;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InventoryUtils {

    private static InventoryUtils instance;
    public static InventoryUtils getInstance() { return instance; }

    /**
     * Map with all inventory actions
     *
     * Key = Inventory
     * Value = List of Actions
     */
    private HashMap<Inventory, List<Action>> inventoryActions;

    /**
     * Constructor
     */
    public InventoryUtils() {
        instance = this;
        this.inventoryActions = new HashMap<>(64);
        VoltzMachines.get().getServer().getPluginManager().registerEvents(new ActionListeners(), VoltzMachines.get()); // register inventory listener
    }

    /**
     * Method to add new action
     * to an inventory
     *
     * @param inventory that will have the action
     * @param item that will be used to execute the action
     * @param action that will be executed
     * @param click type
     */
    public void addAction(Inventory inventory, ItemStack item, Runnable action, ActionClick click) {
        List<Action> actions = getInventoryActions().containsKey(inventory) ? getInventoryActions().get(inventory) : new ArrayList<>(40);

        actions.add(new Action(click, item, action));

        getInventoryActions().put(inventory, actions);
    }

    /**
     * Method to get actions
     *
     * @param inventory that have the action
     * @param item that have the action
     * @param click type
     * @return action based on specifications or null
     */
    public Action getAction(Inventory inventory, ItemStack item, ActionClick click) {
        for (Action action : getInventoryActions().get(inventory)) {
            if (action == null) continue;

            if (action.getClick() == click && action.getItem().equals(item)) return action;
        }

        return null;
    }

    /**
     * Boolean to check if
     * inventory has actions
     *
     * @param inventory that will be checked
     * @return true or false
     */
    public Boolean hasAction(Inventory inventory) {
        return getInventoryActions().containsKey(inventory);
    }

    /**
     * Method to get all actions
     * from a specific inventory
     *
     * @param inventory that have the actions
     * @return list of actions
     */
    public List<Action> getActions(Inventory inventory) {
        return getInventoryActions().get(inventory);
    }

    /**
     * Map with all inventory actions
     *
     * Key = Inventory
     * Value = List of Actions
     */
    private HashMap<Inventory, List<Action>> getInventoryActions() {
        return inventoryActions;
    }

    public static class Action {

        private ActionClick click;
        private ItemStack item;
        private Runnable action;

        public Action(ActionClick click, ItemStack item, Runnable action) {
            this.click = click;
            this.item = item;
            this.action = action;
        }

        public ActionClick getClick() {
            return click;
        }

        public ItemStack getItem() {
            return item;
        }

        public Runnable getAction() {
            return action;
        }

        public void run() {
            if (action == null) return;

            action.run();
        }
    }

    public class ActionListeners implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onClick(InventoryClickEvent event) {
            if (!InventoryUtils.getInstance().getInventoryActions().containsKey(event.getInventory())) return;

            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType().equals(Material.AIR)) return;

            ItemStack item = event.getCurrentItem().clone();
            Action action = getAction(event.getInventory(), item, ActionClick.ALL);

            if (action == null) {
                // try to found specific actions
                switch (event.getClick()) {
                    case LEFT, SHIFT_LEFT -> action = getAction(event.getInventory(), item, ActionClick.LEFT);
                    case RIGHT, SHIFT_RIGHT -> action = getAction(event.getInventory(), item, ActionClick.RIGHT);
                }
            }

            if (action == null) return;

            action.run();
        }
    }

    public enum ActionClick {
        LEFT,
        RIGHT,
        ALL
    }
}