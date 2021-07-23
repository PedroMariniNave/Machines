package com.zpedroo.voltzmachines.utils.item;

import com.zpedroo.voltzmachines.FileUtils;
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
    private ItemStack pickaxe;
    private ItemStack repair;
    private ItemStack present;

    public Items() {
        instance = this;
        this.fuel = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Fuel").build();
        this.pickaxe = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Pickaxe").build();
        this.repair = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Repair").build();
        this.present = ItemBuilder.build(FileUtils.get().getFile(FileUtils.Files.CONFIG).get(), "Present").build();
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
                    NumberFormatter.getInstance().formatDecimal(Double.valueOf(percentage))
            }));

            if (lore != null) {
                List<String> newLore = new ArrayList<>(lore.size());

                for (String str : lore) {
                    newLore.add(StringUtils.replaceEach(str, new String[] {
                            "{percentage}"
                    }, new String[] {
                            NumberFormatter.getInstance().formatDecimal(Double.valueOf(percentage))
                    }));
                }

                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public ItemStack getPresent() {
        NBTItem nbt = new NBTItem(present.clone());
        nbt.addCompound("MachinesPresent");

        return nbt.getItem();
    }
}
