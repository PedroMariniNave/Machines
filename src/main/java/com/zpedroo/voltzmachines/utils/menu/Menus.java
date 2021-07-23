package com.zpedroo.voltzmachines.utils.menu;

import com.zpedroo.voltzmachines.FileUtils;
import com.zpedroo.voltzmachines.VoltzMachines;
import com.zpedroo.voltzmachines.listeners.PlayerChatListener;
import com.zpedroo.voltzmachines.listeners.PlayerGeneralListeners;
import com.zpedroo.voltzmachines.machine.Machine;
import com.zpedroo.voltzmachines.machine.PlayerMachine;
import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.objects.PlayerChat;
import com.zpedroo.voltzmachines.utils.builder.InventoryBuilder;
import com.zpedroo.voltzmachines.utils.builder.InventoryUtils;
import com.zpedroo.voltzmachines.utils.builder.ItemBuilder;
import com.zpedroo.voltzmachines.utils.enums.Action;
import com.zpedroo.voltzmachines.utils.enums.Permission;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
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
import java.util.concurrent.TimeUnit;

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
        List<ItemBuilder> builders = new ArrayList<>(32);

        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str).build();
            int slot = FileUtils.get().getInt(file, "Inventory.items." + str + ".slot");
            List<InventoryUtils.Action> actions = new ArrayList<>(1);
            String actionStr = FileUtils.get().getString(file, "Inventory.items." + str + ".action");

            if (!StringUtils.equals(actionStr, "NULL")) {
                switch (actionStr) {
                    case "OPEN_MACHINES" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                        openPlayerMachinesMenu(player, MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(player.getUniqueId()));
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));
                    case "OPEN_SHOP" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                        openShopMenu(player);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));
                }
            }

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        InventoryBuilder.build(player, inventory, title, builders);
    }

    public void openMachineMenu(Player player, PlayerMachine machine) {
        File folder = new File(VoltzMachines.get().getDataFolder() + "/machines/" + machine.getMachine().getType() + ".yml");
        FileConfiguration file = YamlConfiguration.loadConfiguration(folder);

        Manager manager = machine.getManager(player.getUniqueId());

        String title = ChatColor.translateAlternateColorCodes('&', file.getString("Machine-Menu.title"));
        int size = file.getInt("Machine-Menu.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(32);

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
                    NumberFormatter.getInstance().format(machine.getFuel()),
                    NumberFormatter.getInstance().format(machine.getDrops()),
                    NumberFormatter.getInstance().format(machine.getMachine().getDropsValue().multiply(machine.getDrops())),
                    NumberFormatter.getInstance().format(machine.getMachine().getDropsValue()),
                    NumberFormatter.getInstance().format(machine.getMachine().getDropsPreviousValue()),
                    getStatistics(machine.getMachine().getDropsValue(), machine.getMachine().getDropsPreviousValue()),
                    machine.isEnabled() ? ENABLED : DISABLED
            }).build();

            int slot = file.getInt("Machine-Menu.items." + items + ".slot");
            List<InventoryUtils.Action> actions = new ArrayList<>(1);
            String actionStr = file.getString("Machine-Menu.items." + items + ".action", "NULL");

            if (!StringUtils.equals(actionStr, "NULL")) {
                switch (actionStr.toUpperCase()) {
                    case "SWITCH" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                        if (machine.getFuel().signum() <= 0) {
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        if (machine.getIntegrity() <= 0) {
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        machine.switchStatus();
                        player.closeInventory();
                        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.5f);
                    }));
                    case "SELL_DROPS" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                        if (!player.getUniqueId().equals(machine.getOwnerUUID()) && !manager.can(Permission.SELL_DROPS)) {
                            player.sendMessage(NEED_PERMISSION);
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        if (machine.getDrops().signum() <= 0) {
                            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                            return;
                        }

                        machine.sellDrops(player);
                        openMachineMenu(player, machine);
                        player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.5f, 0.5f);
                    }));
                    case "MANAGERS" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                        openManagersMenu(player, machine);
                        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                    }));
                    case "REMOVE_STACK" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                        if (!player.getUniqueId().equals(machine.getOwnerUUID()) && !manager.can(Permission.REMOVE_STACK)) {
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
                    }));
                }
            }

            builders.add(ItemBuilder.build(item, slot, actions));
        }

        InventoryBuilder.build(player, inventory, title, builders);
    }

    public void openPlayerMachinesMenu(Player player, List<PlayerMachine> machines) {
        FileUtils.Files file = FileUtils.Files.PLAYERMACHINES;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(54);

        if (machines.size() <= 0) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.setItem(slot, item);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (PlayerMachine machine : machines) {
                if (++i >= slots.length) i = 0;

                ItemStack item = machine.getMachine().getDisplayItem();
                ItemMeta meta = item.getItemMeta();
                ArrayList<String> lore = new ArrayList<>(16);
                List<InventoryUtils.Action> actions = new ArrayList<>(1);

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
                            NumberFormatter.getInstance().format(machine.getFuel()),
                            machine.getIntegrity().toString() + "%",
                            machine.isEnabled() ? ENABLED : DISABLED
                    })));
                }

                meta.setDisplayName(machine.getMachine().getDisplayName());
                meta.setLore(lore);
                item.setItemMeta(meta);

                int slot = Integer.parseInt(slots[i]);

                actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
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
        getInventoryUtils().addAction(inventory, sellAll, () -> {
            BigInteger dropsPrice = getTotalDropsPrice(player);

            if (dropsPrice.signum() <= 0) {
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.5f, 0.5f);
                return;
            }

            for (PlayerMachine machine : MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(player.getUniqueId())) {
                if (machine == null) continue;
                if (machine.getDrops().signum() <= 0) continue;

                machine.sellDrops(player);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.ENTITY_HORSE_ARMOR, 0.5f, 0.5f);
        }, InventoryUtils.ActionClick.ALL);

        ItemStack enableAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Enable-All").build();
        int enableAllSlot = FileUtils.get().getInt(file, "Enable-All.slot");

        inventory.setItem(enableAllSlot, enableAll);
        getInventoryUtils().addAction(inventory, enableAll, () -> {
            for (PlayerMachine machine : MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(player.getUniqueId())) {
                if (machine == null) continue;
                if (machine.getFuel().signum() <= 0) continue;

                machine.setStatus(true);
            }

            player.closeInventory();
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.5f);
        }, InventoryUtils.ActionClick.ALL);

        ItemStack disableAll = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Disable-All").build();
        int disableAllSlot = FileUtils.get().getInt(file, "Disable-All.slot");

        inventory.setItem(disableAllSlot, disableAll);
        getInventoryUtils().addAction(inventory, disableAll, () -> {
            for (PlayerMachine machine : MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(player.getUniqueId())) {
                if (machine == null) continue;

                machine.setStatus(false);
            }

            openPlayerMachinesMenu(player, machines);
            player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 0.5f, 0.5f);
        }, InventoryUtils.ActionClick.ALL);

        int nextPageSlot = FileUtils.get().getInt(file, "Inventory.next-page-slot");
        int previousPageSlot = FileUtils.get().getInt(file, "Inventory.previous-page-slot");

        InventoryBuilder.build(player, inventory, title, builders, nextPageSlot, previousPageSlot, nextPageItem, previousPageItem);
    }

    public void openManagersMenu(Player player, PlayerMachine machine) {
        FileUtils.Files file = FileUtils.Files.MANAGERS;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(32);

        if (machine.getManagers().size() <= 0) {
            ItemStack item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Nothing").build();
            int slot = FileUtils.get().getInt(file, "Nothing.slot");

            inventory.setItem(slot, item);
        } else {
            String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
            int i = -1;

            for (Manager manager : machine.getManagers()) {
                if (++i >= slots.length) i = 0;
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
                List<InventoryUtils.Action> actions = new ArrayList<>(1);

                if (machine.getOwnerUUID().equals(player.getUniqueId()) || manager.can(Permission.REMOVE_FRIENDS)) {
                    ItemMeta meta = item.getItemMeta();
                    ArrayList<String> lore = meta.hasLore() ? (ArrayList<String>) meta.getLore() : new ArrayList<>();

                    for (String toAdd : FileUtils.get().getStringList(file, "Extra-Lore")) {
                        if (toAdd == null) break;

                        lore.add(ChatColor.translateAlternateColorCodes('&', toAdd));
                    }

                    meta.setLore(lore);
                    item.setItemMeta(meta);

                    actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
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
        getInventoryUtils().addAction(inventory, addFriend, () -> {
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
        }, InventoryUtils.ActionClick.ALL);

        ItemStack back = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Back").build();
        int backSlot = FileUtils.get().getInt(file, "Back.slot");

        inventory.setItem(backSlot, back);
        getInventoryUtils().addAction(inventory, back, () -> {
            openMachineMenu(player, machine);
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
        }, InventoryUtils.ActionClick.ALL);

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

                String actionStr = FileUtils.get().getString(file, "Inventory.items." + str + ".action");

                if (!StringUtils.equals(actionStr, "NULL")) {
                    switch (actionStr) {
                        case "BACK" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                            openManagersMenu(player, machine);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                        }));
                        case "REMOVE" -> actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                            machine.getManagers().remove(manager);
                            machine.setQueueUpdate(true);
                            openManagersMenu(player, machine);
                            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2f, 2f);
                        }));
                    }
                }
            } else {
                item = ItemBuilder.build(FileUtils.get().getFile(file).get(), "Inventory.items." + str + "." + (manager.can(permission) ? "true" : "false")).build();

                final Permission finalPermission = permission;
                actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
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

            Machine machine = MachineManager.getInstance().getMachine(str);
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
                    format(NEXT_UPDATE - System.currentTimeMillis())
            }).build();
            int slot = Integer.parseInt(slots[i]);
            actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                PlayerChatListener.getPlayerChat().put(player, new PlayerChat(player, machine, price, Action.BUY_MACHINE));
                player.closeInventory();
                clearChat(player);

                for (String msg : CHOOSE_AMOUNT) {
                    player.sendMessage(StringUtils.replaceEach(msg, new String[]{
                            "{price}",
                            "{drops_now}",
                            "{drops_previous}",
                            "{statistics}",
                            "{type}"
                    }, new String[]{
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

    public void openPresentMenu(Player player) {
        FileUtils.Files file = FileUtils.Files.PRESENT;

        String title = ChatColor.translateAlternateColorCodes('&', FileUtils.get().getString(file, "Inventory.title"));
        int size = FileUtils.get().getInt(file, "Inventory.size");

        Inventory inventory = Bukkit.createInventory(null, size, title);
        List<ItemBuilder> builders = new ArrayList<>(54);
        String[] slots = FileUtils.get().getString(file, "Inventory.slots").replace(" ", "").split(",");
        int i = -1;
        for (String str : FileUtils.get().getSection(file, "Inventory.items")) {
            if (++i >= slots.length) i = 0;

            Machine machine = MachineManager.getInstance().getMachine(str);
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
                    format(NEXT_UPDATE - System.currentTimeMillis())
            }).build();
            int slot = Integer.parseInt(slots[i]);
            actions.add(new InventoryUtils.Action(InventoryUtils.ActionClick.ALL, item, () -> {
                PlayerGeneralListeners.getChoosingPresent().remove(player);
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

        for (PlayerMachine machine : MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(player.getUniqueId())) {
            if (machine == null) continue;
            if (machine.getDrops().signum() <= 0) continue;

            dropsAmount = dropsAmount.add(machine.getDrops());
        }

        return dropsAmount;
    }

    private BigInteger getTotalDropsPrice(Player player) {
        BigInteger dropsPrice = BigInteger.ZERO;

        for (PlayerMachine machine : MachineManager.getInstance().getDataCache().getPlayerMachinesByUUID(player.getUniqueId())) {
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

    private String format(long nextUpdate) {
        long days = TimeUnit.MILLISECONDS.toDays(nextUpdate);
        long hours = TimeUnit.MILLISECONDS.toHours(nextUpdate) - (days * 24);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(nextUpdate) - (TimeUnit.MILLISECONDS.toHours(nextUpdate) * 60);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(nextUpdate) - (TimeUnit.MILLISECONDS.toMinutes(nextUpdate) * 60);

        StringBuilder builder = new StringBuilder();

        if (days > 0) builder.append(days).append(" ").append(days == 1 ? DAY : DAYS).append(" ");
        if (hours > 0) builder.append(hours).append(" ").append(hours == 1 ? HOUR : HOURS).append(" ");
        if (minutes > 0) builder.append(minutes).append(" ").append(minutes == 1 ? MINUTE : MINUTES).append(" ");
        if (seconds > 0) builder.append(seconds).append(" ").append(seconds == 1 ? SECOND : SECONDS);

        String ret = builder.toString();

        return ret.isEmpty() ? NOW : ret;
    }

    private void clearChat(Player player) {
        for (int i = 0; i < 25; ++i) {
            player.sendMessage("");
        }
    }

    private InventoryUtils getInventoryUtils() {
        return inventoryUtils;
    }
}