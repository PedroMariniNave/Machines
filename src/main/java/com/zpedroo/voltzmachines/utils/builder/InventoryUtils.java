package com.zpedroo.voltzmachines.utils.builder;

import com.zpedroo.voltzmachines.VoltzMachines;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;

import java.util.HashMap;
import java.util.Map;

public class InventoryUtils {

    private static InventoryUtils instance;
    public static InventoryUtils get() { return instance; }

    private Map<Player, InventoryBuilder> viewers;

    public InventoryUtils() {
        instance = this;
        this.viewers = new HashMap<>(16);
        VoltzMachines.get().getServer().getPluginManager().registerEvents(new ActionListeners(), VoltzMachines.get());
    }

    public Map<Player, InventoryBuilder> getViewers() {
        return viewers;
    }

    private class ActionListeners implements Listener {

        @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
        public void onClick(InventoryClickEvent event) {
            Player player = (Player) event.getWhoClicked();
            if (!viewers.containsKey(player)) return;

            InventoryBuilder inventory = viewers.get(player);
            if (inventory.getInventory().getViewers().isEmpty()) {
                viewers.remove(player);
                return;
            }

            event.setCancelled(true);
            if (event.getClickedInventory() == null) return;
            if (event.getClickedInventory().getType().equals(InventoryType.PLAYER)) return;

            int slot = event.getSlot();

            ActionType actionType = ActionType.ALL_CLICKS;
            if (inventory.getAction(slot, actionType) == null) {
                switch (event.getClick()) {
                    case LEFT:
                    case SHIFT_LEFT:
                        actionType = ActionType.LEFT_CLICK;
                        break;
                    case RIGHT:
                    case SHIFT_RIGHT:
                        actionType = ActionType.RIGHT_CLICK;
                        break;
                    case MIDDLE:
                        actionType = ActionType.SCROLL;
                        break;
                }
            }

            Action action = inventory.getAction(slot, actionType);
            if (action != null) action.getAction().run();
        }
    }

    public static class Action {

        private Runnable action;
        private ActionType actionType;

        public Action(Runnable action, ActionType actionType) {
            this.action = action;
            this.actionType = actionType;
        }

        public Runnable getAction() {
            return action;
        }

        public ActionType getActionType() {
            return actionType;
        }
    }

    public enum ActionType {
        LEFT_CLICK,
        RIGHT_CLICK,
        ALL_CLICKS,
        SCROLL
    }
}