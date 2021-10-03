package com.zpedroo.voltzmachines.utils.menu;

import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.PlayerMachine;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.utils.FileUtils;
import com.zpedroo.voltzmachines.listeners.PlayerChatListener;
import com.zpedroo.voltzmachines.listeners.PlayerGeneralListeners;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.objects.PlayerChat;
import com.zpedroo.voltzmachines.utils.builder.InventoryBuilder;
import com.zpedroo.voltzmachines.utils.builder.InventoryUtils;
import com.zpedroo.voltzmachines.utils.builder.ItemBuilder;
import com.zpedroo.voltzmachines.utils.enums.Action;
import com.zpedroo.voltzmachines.utils.enums.Permission;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import com.zpedroo.voltzmachines.utils.formatter.TimeFormatter;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.zpedroo.voltzmachines.utils.config.Messages.*;
import static com.zpedroo.voltzmachines.utils.config.Settings.*;

public class Menus {

    private static Menus instance;
    public static Menus getInstance() { return instance; }

    private InventoryUtils inventoryUtils;

    private ItemStack nextPageItem;
    private ItemStack previousPageItem;

    public Menus() {
        instance = this;
        this.inventoryUtils = new InventoryUtils();
        this.nextPageItem = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Next-Page").build();
        this.previousPageItem = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Previous-Page").build();
    }

    public void openMainMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.MAIN;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);

        for (String items : FileUtils.get().getSection(file, "Inventory.items")) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + items).build();
            int slot = FileUtils.get().getInt(file, "Inventory.items." + items + ".slot");
            String action = FileUtils.get().getString(file, "Inventory.items." + items + ".action");

            switch (action.toUpperCase()) {
                case "MACHINES" -> inventoryUtils.addAction(inventory, item, () -> {
                    openPlayerMachinesMenu(player, DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId()));
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "SHOP" -> inventoryUtils.addAction(inventory, item, () -> {
                    openShopMenu(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "TOP" -> inventoryUtils.addAction(inventory, item, () -> {
                    openTopMachinesMenu(player);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);
            }

            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    public void openMachineMenu(Player player, PlayerMachine machine) {
        File folder = new File(VoltzMachines.get().getDataFolder() + "/machines/" + machine.getMachine().getType() + ".yml");
        FileConfiguration file = YamlConfiguration.loadConfiguration(folder);

        Manager manager = machine.getManager(player.getUniqueId());

        String title = ChatColor.translateAlternateColorCodes('&', file.getString("Machine-Menu.title"));
        int size = file.getInt("Machine-Menu.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);

        for (String items : file.getConfigurationSection("Machine-Menu.items").getKeys(false)) {
            ItemStack item = ItemBuilder.build(file, "Machine-Menu.items." + items, new String[]{
                    "{owner}",
                    "{type}",
                    "{stack}",
                    "{fuel}",
                    "{drops}",
                    "{price}",
                    "{single_price}",
                    "{drops_previous}",
                    "{statistics}",
                    "{status}"
            }, new String[]{
                    Bukkit.getOfflinePlayer(machine.getOwnerUUID()).getName(),
                    machine.getMachine().getTypeTranslated(),
                    NumberFormatter.getInstance().format(machine.getStack()),
                    machine.hasInfiniteFuel() ? "∞" : NumberFormatter.getInstance().format(machine.getFuel()),
                    NumberFormatter.getInstance().format(machine.getDrops()),
                    NumberFormatter.getInstance().format(machine.getMachine().getDropsValue().multiply(machine.getDrops())),
                    NumberFormatter.getInstance().format(machine.getMachine().getDropsValue()),
                    NumberFormatter.getInstance().format(machine.getMachine().getDropsPreviousValue()),
                    getStatistics(machine.getMachine().getDropsValue(), machine.getMachine().getDropsPreviousValue()),
                    machine.isEnabled() ? ENABLED : DISABLED
            }).build();

            int slot = file.getInt("Machine-Menu.items." + items + ".slot");
            String action = file.getString("Machine-Menu.items." + items + ".action", "NULL");

            switch (action.toUpperCase()) {
                case "SWITCH" -> inventoryUtils.addAction(inventory, item, () -> {
                    if (!machine.hasInfiniteFuel() && machine.getFuel().signum() <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    if (machine.getIntegrity() <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    machine.switchStatus();
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 100f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "SELL_DROPS" -> inventoryUtils.addAction(inventory, item, () -> {
                    if (!player.getUniqueId().equals(machine.getOwnerUUID()) && (manager != null && !manager.can(Permission.SELL_DROPS))) {
                        player.sendMessage(NEED_PERMISSION);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    if (machine.getDrops().signum() <= 0) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    machine.sellDrops(player);
                    player.closeInventory();
                    player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.5f, 0.5f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "MANAGERS" -> inventoryUtils.addAction(inventory, item, () -> {
                    openManagersMenu(player, machine);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }, InventoryUtils.ActionType.ALL_CLICKS);

                case "REMOVE_STACK" -> inventoryUtils.addAction(inventory, item, () -> {
                    if (!player.getUniqueId().equals(machine.getOwnerUUID()) && (manager != null && !manager.can(Permission.REMOVE_STACK))) {
                        player.sendMessage(NEED_PERMISSION);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, machine, Action.REMOVE_STACK));
                    player.closeInventory();
                    clearChat(player);

                    for (String msg : REMOVE_STACK) {
                        player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                                "{tax}"
                        }, new String[]{
                                String.valueOf(TAX_REMOVE_STACK)
                        }));
                    }
                }, InventoryUtils.ActionType.ALL_CLICKS);
            }

            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    public void openTopMachinesMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.TOP_MACHINES;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);

        String[] topSlots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");

        Map<UUID, BigInteger> topMachines = DataManager.getInstance().getCache().getTopMachines();
        int pos = 0;

        for (Map.Entry<UUID, BigInteger> entry : topMachines.entrySet()) {
            UUID uuid = entry.getKey();
            BigInteger stack = entry.getValue();

            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Item", new String[]{
                    "{player}",
                    "{machines}",
                    "{pos}"
            }, new String[]{
                    Bukkit.getOfflinePlayer(uuid).getName(),
                    NumberFormatter.getInstance().format(stack),
                    String.valueOf(++pos)
            }).build();

            int slot = Integer.parseInt(topSlots[pos-1]);

            inventoryUtils.addAction(inventory, item, null, InventoryUtils.ActionType.ALL_CLICKS);

            inventory.setItem(slot, item);
        }

        player.openInventory(inventory);
    }

    public void openPlayerMachinesMenu(Player player, List<PlayerMachine> machines) {
        FileUtils.Files file = FileUtils.Files.PLAYER_MACHINES;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(machines.size());

        if (machines.size() <= 0) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.setItem(slot, item);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (PlayerMachine machine : machines) {
                if (++i >= slots.length) i = 0;

                List<String> lore = new ArrayList<>(8);
                List<InventoryUtils.Action> actions = new ArrayList<>(1);

                ItemStack item = machine.getMachine().getDisplayItem();
                ItemMeta meta = item.getItemMeta();

                for (String toAdd : FileUtils.get().getStringList(file, "Item-Lore")) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', StringUtils.replaceEach(toAdd, new String[]{
                            "{stack}",
                            "{drops}",
                            "{fuel}",
                            "{integrity}",
                            "{status}"
                    }, new String[]{
                            NumberFormatter.getInstance().format(machine.getStack()),
                            NumberFormatter.getInstance().format(machine.getDrops()),
                            machine.hasInfiniteFuel() ? "∞" : NumberFormatter.getInstance().format(machine.getFuel()),
                            machine.hasInfiniteIntegrity() ? "∞" : machine.getIntegrity().toString() + "%",
                            machine.isEnabled() ? ENABLED : DISABLED
                    })));
                }

                meta.setDisplayName(machine.getMachine().getDisplayName());
                meta.setLore(lore);
                item.setItemMeta(meta);

                int slot = Integer.parseInt(slots[i]);

                actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                    openMachineMenu(player, machine);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }));

                builders.add(ItemBuilder.build(item, slot, actions));
            }
        }

        ItemStack sellAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Sell-All", new String[]{
                "{drops}",
                "{price}"
        }, new String[]{
                NumberFormatter.getInstance().format(getTotalDrops(player)),
                NumberFormatter.getInstance().format(getTotalDropsPrice(player))
        }).build();
        int sellAllSlot = FileUtils.get().getInt(file, "Sell-All.slot");

        inventory.setItem(sellAllSlot, sellAll);
        inventoryUtils.addAction(inventory, sellAll, () -> {
            BigInteger dropsPrice = getTotalDropsPrice(player);

            if (dropsPrice.signum() <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                return;
            }

            for (PlayerMachine machine : DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId())) {
                if (machine == null) continue;
                if (machine.getDrops().signum() <= 0) continue;

                machine.sellDrops(player);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.5f, 100f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        ItemStack enableAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Enable-All").build();
        int enableAllSlot = FileUtils.get().getInt(file, "Enable-All.slot");

        inventory.setItem(enableAllSlot, enableAll);
        inventoryUtils.addAction(inventory, enableAll, () -> {
            for (PlayerMachine machine : DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId())) {
                if (machine == null) continue;
                if (!machine.hasInfiniteFuel() && machine.getFuel().signum() <= 0) continue;

                machine.setStatus(true);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 100f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        ItemStack disableAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Disable-All").build();
        int disableAllSlot = FileUtils.get().getInt(file, "Disable-All.slot");

        inventory.setItem(disableAllSlot, disableAll);
        inventoryUtils.addAction(inventory, disableAll, () -> {
            for (PlayerMachine machine : DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId())) {
                if (machine == null) continue;

                machine.setStatus(false);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 0.5f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    public void openManagersMenu(Player player, PlayerMachine machine) {
        FileUtils.Files file = FileUtils.Files.MANAGERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(machine.getManagers().size());

        if (machine.getManagers().size() <= 0) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.setItem(slot, item);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (Manager manager : machine.getManagers()) {
                if (++i >= slots.length) i = 0;

                List<InventoryUtils.Action> actions = new ArrayList<>(1);

                ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Item", new String[]{
                        "{player}",
                        "{add_stack}",
                        "{remove_stack}",
                        "{add_friends}",
                        "{remove_friends}",
                        "{sell_drops}"
                }, new String[]{
                        Bukkit.getOfflinePlayer(manager.getUUID()).getName(),
                        manager.can(Permission.ADD_STACK) ? TRUE : FALSE,
                        manager.can(Permission.REMOVE_STACK) ? TRUE : FALSE,
                        manager.can(Permission.ADD_FRIENDS) ? TRUE : FALSE,
                        manager.can(Permission.REMOVE_FRIENDS) ? TRUE : FALSE,
                        manager.can(Permission.SELL_DROPS) ? TRUE : FALSE
                }).build();
                int slot = Integer.parseInt(slots[i]);

                if (machine.getOwnerUUID().equals(player.getUniqueId()) || manager.can(Permission.REMOVE_FRIENDS)) {
                    ItemMeta meta = item.getItemMeta();
                    List<String> lore = meta.hasLore() ? (ArrayList<String>) meta.getLore() : new ArrayList<>();

                    for (String toAdd : FileUtils.get().getStringList(file, "Extra-Lore")) {
                        if (toAdd == null) break;

                        lore.add(ChatColor.translateAlternateColorCodes('&', toAdd));
                    }

                    meta.setLore(lore);
                    item.setItemMeta(meta);

                    actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                        openPermissionsMenu(player, machine, manager);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));
                }

                builders.add(ItemBuilder.build(item, slot, actions));
            }
        }

        Manager manager = machine.getManager(player.getUniqueId());

        ItemStack addFriend = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Add-Friend").build();
        int addFriendSlot = FileUtils.get().getInt(file, "Add-Friend.slot");

        inventory.setItem(addFriendSlot, addFriend);
        inventoryUtils.addAction(inventory, addFriend, () -> {
            if (machine.getOwnerUUID().equals(player.getUniqueId()) || (manager != null && manager.can(Permission.ADD_FRIENDS))) {
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, machine, Action.ADD_FRIEND));
                player.closeInventory();
                clearChat(player);

                for (String msg : ADD_FRIEND) {
                    player.sendMessage(msg);
                }
                return;
            }

            player.sendMessage(NEED_PERMISSION);
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        ItemStack back = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Back").build();
        int backSlot = FileUtils.get().getInt(file, "Back.slot");

        inventory.setItem(backSlot, back);
        inventoryUtils.addAction(inventory, back, () -> {
            openMachineMenu(player, machine);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
        }, InventoryUtils.ActionType.ALL_CLICKS);

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    public void openPermissionsMenu(Player player, PlayerMachine machine, Manager manager) {
        FileUtils.Files file = FileUtils.Files.PERMISSIONS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(5);

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            Permission permission = null;

            try {
                permission = Permission.valueOf(str.toUpperCase());
            } catch (Exception ex) {
                // ignore
            }

            ItemStack item = null;
            int slot = FileUtils.get().getInt(file, "Inventory.items." + str + ".slot");
            List<InventoryUtils.Action> actions = new ArrayList<>(1);

            if (permission == null) {
                item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                        "{friend}"
                }, new String[]{
                        Bukkit.getOfflinePlayer(manager.getUUID()).getName()
                }).build();

                String action = FileUtils.get().getString(file, "Inventory.items." + str + ".action");

                switch (action.toUpperCase()) {
                    case "BACK" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                        openManagersMenu(player, machine);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));

                    case "REMOVE" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                        machine.getManagers().remove(manager);
                        machine.setQueueUpdate(true);
                        openManagersMenu(player, machine);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));
                }
            } else {
                item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str + "." + (manager.can(permission) ? "true" : "false")).build();

                final Permission finalPermission = permission;

                actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                    if (!player.getUniqueId().equals(machine.getOwnerUUID())) {
                        player.sendMessage(ONLY_OWNER);
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                        return;
                    }

                    manager.set(finalPermission, !manager.can(finalPermission));
                    machine.setQueueUpdate(true);
                    openPermissionsMenu(player, machine, manager);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                }));
            }

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        InventoryBuilder.build(player, inventory, title, builders);
    }

    public void openShopMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.SHOP;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(54);
        String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
        int i = -1;

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            if (++i >= slots.length) i = 0;

            Machine machine = DataManager.getInstance().getMachine(str);
            if (machine == null) continue;

            List<InventoryUtils.Action> actions = new ArrayList<>(1);
            BigInteger price = new BigInteger(FileUtils.get().getString(file, "Inventory.items." + str + ".price", "0"));
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                    "{price}",
                    "{drops_now}",
                    "{drops_previous}",
                    "{statistics}",
                    "{type}",
                    "{update}"
            }, new String[]{
                    NumberFormatter.getInstance().format(price),
                    NumberFormatter.getInstance().format(machine.getDropsValue()),
                    NumberFormatter.getInstance().format(machine.getDropsPreviousValue()),
                    getStatistics(machine.getDropsValue(), machine.getDropsPreviousValue()),
                    machine.getTypeTranslated(),
                    TimeFormatter.getInstance().format(NEXT_UPDATE - System.currentTimeMillis())
            }).build();
            int slot = Integer.parseInt(slots[i]);

            actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, machine, price, Action.BUY_MACHINE));
                player.closeInventory();
                clearChat(player);

                for (String msg : CHOOSE_AMOUNT) {
                    player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                            "{machine}",
                            "{price}",
                            "{drops_now}",
                            "{drops_previous}",
                            "{statistics}",
                            "{type}"
                    }, new String[]{
                            machine.getDisplayName(),
                            NumberFormatter.getInstance().format(price),
                            NumberFormatter.getInstance().format(machine.getDropsValue()),
                            NumberFormatter.getInstance().format(machine.getDropsPreviousValue()),
                            getStatistics(machine.getDropsValue(), machine.getDropsPreviousValue()),
                            machine.getTypeTranslated()
                    }));
                }
            }));

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    public void openGiftMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.GIFT;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(54);
        String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
        int i = -1;
        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            if (++i >= slots.length) i = 0;

            Machine machine = DataManager.getInstance().getMachine(str);
            if (machine == null) continue;

            List<InventoryUtils.Action> actions = new ArrayList<>(1);
            BigInteger machinesAmount = new BigInteger(FileUtils.get().getString(file, "Inventory.items." + str + ".machines-amount", "1"));
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str, new String[]{
                    "{drops_now}",
                    "{drops_previous}",
                    "{statistics}",
                    "{type}",
                    "{update}"
            }, new String[]{
                    NumberFormatter.getInstance().format(machine.getDropsValue()),
                    NumberFormatter.getInstance().format(machine.getDropsPreviousValue()),
                    getStatistics(machine.getDropsValue(), machine.getDropsPreviousValue()),
                    machine.getTypeTranslated(),
                    TimeFormatter.getInstance().format(NEXT_UPDATE - System.currentTimeMillis())
            }).build();
            int slot = Integer.parseInt(slots[i]);

            actions.add(new InventoryUtils.Action(InventoryUtils.ActionType.ALL_CLICKS, item, () -> {
                PlayerGeneralListeners.getChoosingGift().remove(player);
                player.getInventory().addItem(machine.getItem(machinesAmount, 100));
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 10f);
                player.closeInventory();
            }));

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    private BigInteger getTotalDrops(Player player) {
        BigInteger dropsAmount = BigInteger.ZERO;

        for (PlayerMachine machine : DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId())) {
            if (machine == null) continue;
            if (machine.getDrops().signum() <= 0) continue;

            dropsAmount = dropsAmount.add(machine.getDrops());
        }

        return dropsAmount;
    }

    private BigInteger getTotalDropsPrice(Player player) {
        BigInteger dropsPrice = BigInteger.ZERO;

        for (PlayerMachine machine : DataManager.getInstance().getCache().getPlayerMachinesByUUID(player.getUniqueId())) {
            if (machine == null) continue;
            if (machine.getDrops().signum() <= 0) continue;

            dropsPrice = dropsPrice.add(machine.getDrops().multiply(machine.getMachine().getDropsValue()));
        }

        return dropsPrice;
    }

    private String getStatistics(BigInteger value1, BigInteger value2) {
        StringBuilder ret = new StringBuilder();

        if (value1.compareTo(value2) > 0) {
            ret.append("§a⬆");
        } else {
            ret.append("§c⬇");
        }

        double increase = value1.doubleValue() - value2.doubleValue();
        double divide = increase / value2.doubleValue();

        ret.append(NumberFormatter.getInstance().formatDecimal(divide * 100)).append("%");
        return ret.toString().replace("-", "");
    }

    private void clearChat(Player player) {
        for (int i = 0; i < 25; ++i) {
            player.sendMessage("");
        }
    }
}