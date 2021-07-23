package com.zpedroo.voltzmachines.machine;

import com.zpedroo.voltzmachines.utils.formatter.NumberFormatter;
import de.tr7zw.nbtapi.NBTItem;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Machine {

    private ItemStack item;
    private Material block;
    private String type;
    private String typeTranslated;
    private String displayName;
    private Integer delay;
    private BigInteger amount;
    private BigInteger dropsValue;
    private BigInteger dropsPreviousValue;
    private BigInteger maxStack;
    private String permission;
    private List<String> commands;

    public Machine(ItemStack item, Material block, String type, String typeTranslated, String displayName, Integer delay, BigInteger amount, BigInteger dropsValue, BigInteger dropsPreviousValue, BigInteger maxStack, String permission, List<String> commands) {
        this.item = item;
        this.block = block;
        this.type = type;
        this.typeTranslated = typeTranslated;
        this.displayName = displayName;
        this.delay = delay;
        this.amount = amount;
        this.dropsValue = dropsValue;
        this.dropsPreviousValue = dropsPreviousValue;
        this.maxStack = maxStack;
        this.permission = permission;
        this.commands = commands;
    }

    public Material getBlock() {
        return block;
    }

    public ItemStack getDisplayItem() {
        return item.clone();
    }

    public ItemStack getItem(BigInteger amount, Integer integrity) {
        NBTItem nbt = new NBTItem(item.clone());
        nbt.setString("MachinesAmount", amount.toString());
        nbt.setString("MachinesType", getType());
        nbt.setInteger("MachinesIntegrity", integrity);

        ItemStack item = nbt.getItem();
        if (item.getItemMeta() != null) {
            String displayName = item.getItemMeta().hasDisplayName() ? item.getItemMeta().getDisplayName() : null;
            List<String> lore = item.getItemMeta().hasLore() ? item.getItemMeta().getLore() : null;
            ItemMeta meta = item.getItemMeta();

            if (displayName != null) meta.setDisplayName(StringUtils.replaceEach(displayName, new String[] {
                    "{amount}",
                    "{integrity}"
            }, new String[] {
                    NumberFormatter.getInstance().format(amount),
                    integrity.toString() + "%"
            }));

            if (lore != null) {
                List<String> newLore = new ArrayList<>(lore.size());

                for (String str : lore) {
                    newLore.add(StringUtils.replaceEach(str, new String[] {
                            "{amount}",
                            "{integrity}"
                    }, new String[] {
                            NumberFormatter.getInstance().format(amount),
                            integrity.toString() + "%"
                    }));
                }

                meta.setLore(newLore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public String getType() {
        return type;
    }

    public String getTypeTranslated() {
        return typeTranslated;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getDelay() {
        return delay;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public BigInteger getDropsValue() {
        return dropsValue;
    }

    public BigInteger getDropsPreviousValue() {
        return dropsPreviousValue;
    }

    public BigInteger getMaxStack() {
        return maxStack;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setDropsValue(BigInteger value) {
        this.dropsValue = value;
    }

    public void setDropsPreviousValue(BigInteger value) {
        this.dropsPreviousValue = value;
    }
}