package com.zpedroo.voltzmachines.utils.item;

import com.zpedroo.voltzmachines.utils.FileUtils;
import com.zpedroo.voltzmachines.utils.builder.ItemBuilder;
import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Items {

    private static Items instance;
    public static Items getInstance() { return instance; }

    private ItemStack fuel;
    private ItemStack infiniteFuel;
    private ItemStack infiniteRepair;
    private ItemStack pickaxe;
    private ItemStack repair;
    private ItemStack gift;

    public Items() {
        instance = this;
        this.fuel = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Fuel").build();
        this.infiniteFuel = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Infinite-Fuel").build();
        this.infiniteRepair = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Infinite-Repair").build();
        this.pickaxe = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Pickaxe").build();
        this.repair = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Repair").build();
        this.gift = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Gift").build();
    }

    public ItemStack getFuel(BigInteger amount) {
        NBTItem nbt = new NBTItem(fuel.clone());
        nbt.setString("MachinesFuel", amount.toString());

        ItemStack item = nbt.getItem();

        if (item.getItemMeta() != null) {
            String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
            List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : null;
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) meta.setDisplayName(StringUtils.replaceEach(displayName, new String[] {
                    "{amount}"
            }, new String[] {
                    NumberFormatter.getInstance().format(amount)
            }));

            if (lore != null) {
                List<String> newLore = new ArrayList<>(lore.size());

                for (String str : lore) {
                    newLore.add(StringUtils.replaceEach(str, new String[] {
                            "{amount}"
                    }, new String[] {
                            NumberFormatter.getInstance().format(amount)
                    }));
                }

                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getInfiniteFuel() {
        NBTItem nbt = new NBTItem(infiniteFuel.clone());
        nbt.addCompound("MachinesInfiniteFuel");

        return nbt.getItem();
    }

    public ItemStack getInfiniteRepair() {
        NBTItem nbt = new NBTItem(infiniteRepair.clone());
        nbt.addCompound("MachinesInfiniteRepair");

        return nbt.getItem();
    }

    public ItemStack getPickaxe() {
        NBTItem nbt = new NBTItem(pickaxe.clone());
        nbt.addCompound("MachinesPickaxe");

        return nbt.getItem();
    }

    public ItemStack getRepair(Integer percentage) {
        NBTItem nbt = new NBTItem(repair.clone());
        nbt.setInteger("MachinesRepair", percentage);

        ItemStack item = nbt.getItem();

        if (item.getItemMeta() != null) {
            String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
            List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : null;
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) meta.setDisplayName(StringUtils.replaceEach(displayName, new String[] {
                    "{percentage}"
            }, new String[] {
                    NumberFormatter.getInstance().formatDecimal(percentage.doubleValue())
            }));

            if (lore != null) {
                List<String> newLore = new ArrayList<>(lore.size());

                for (String str : lore) {
                    newLore.add(StringUtils.replaceEach(str, new String[] {
                            "{percentage}"
                    }, new String[] {
                            NumberFormatter.getInstance().formatDecimal(percentage.doubleValue())
                    }));
                }

                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getGift() {
        NBTItem nbt = new NBTItem(gift.clone());
        nbt.addCompound("MachinesGift");

        return nbt.getItem();
    }
}
