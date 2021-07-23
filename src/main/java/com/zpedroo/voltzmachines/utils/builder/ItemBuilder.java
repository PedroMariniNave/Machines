package com.zpedroo.voltzmachines.utils.builder;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack item;
    private Integer slot;
    private List<InventoryUtils.Action> actions;

    private static Method metaSetProfileMethod;
    private static Field metaProfileField;

    public ItemBuilder(Material material, int amount, short durability, Integer slot, List<InventoryUtils.Action> actions) {
        if (StringUtils.equals(material.toString(), "PLAYER_HEAD")) {
            this.item = new ItemStack(material, amount, (short) 3);
        } else {
            this.item = new ItemStack(material, amount, durability);
        }

        this.slot = slot;
        this.actions = actions;
    }

    public ItemBuilder(ItemStack item, Integer slot, List<InventoryUtils.Action> actions) {
        this.item = item;
        this.slot = slot;
        this.actions = actions;
    }

    public static ItemBuilder build(ItemStack item, Integer slot, List<InventoryUtils.Action> actions) {
        return new ItemBuilder(item, slot, actions);
    }

    public static ItemBuilder build(FileConfiguration file, String where) {
        return build(file, where, null, null, null);
    }

    public static ItemBuilder build(FileConfiguration file, String where, String[] placeholders, String[] replacers) {
        return build(file, where, placeholders, replacers, null);
    }

    public static ItemBuilder build(FileConfiguration file, String where, List<InventoryUtils.Action> actions) {
        return build(file, where, null, null, actions);
    }

    public static ItemBuilder build(FileConfiguration file, String where, String[] placeholders, String[] replacers, List<InventoryUtils.Action> actions) {
        String type = StringUtils.replace(file.getString(where + ".type"), " ", "").toUpperCase();
        short data = Short.parseShort(file.getString(where + ".data", "0"));
        int amount = file.getInt(where + ".amount", 1);
        int slot = file.getInt(where + ".slot", 0);

        Material material = Material.getMaterial(StringUtils.replaceEach(type, placeholders, replacers));
        Validate.notNull(material, "Material cannot be null! Check your item configs.");

        ItemBuilder builder = new ItemBuilder(material, amount, data, slot, actions);

        if (file.contains(where + ".name")) {
            String name = ChatColor.translateAlternateColorCodes('&', file.getString(where + ".name"));
            builder.setName(StringUtils.replaceEach(name, placeholders, replacers));
        }

        if (file.contains(where + ".lore")) {
            builder.setLore(file.getStringList(where + ".lore"), placeholders, replacers);
        }

        if (file.contains(where + ".owner")) {
            String owner = file.getString(where + ".owner");

            if (owner.length() <= 16) { // max player name lenght
                builder.setSkullOwner(StringUtils.replaceEach(owner, placeholders, replacers));
            } else {
                builder.setCustomTexture(owner);
            }
        }

        if (file.contains(where + ".glow") && file.getBoolean(where + ".glow")) {
            builder.setGlow();
        }

        if (file.contains(where + ".enchants")) {
            for (String str : file.getStringList(where + ".enchants")) {
                if (str == null) continue;

                String enchantment = StringUtils.replace(str, " ", "");

                if (StringUtils.contains(enchantment, ",")) {
                    String[] split = enchantment.split(",");
                    builder.addEnchantment(Enchantment.getByName(split[0]), Integer.parseInt(split[1]));
                } else {
                    builder.addEnchantment(Enchantment.getByName(enchantment));
                }
            }
        }

        if (file.contains(where + ".custom-model-data")) {
            builder.setCustomModelData(file.getInt(where + ".custom-model-data"));
        }

        return builder;
    }

    private void setName(String name) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        item.setItemMeta(meta);
    }

    private void setLore(List<String> lore, String[] placeholders, String[] replacers) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> newLore = new ArrayList<>(lore.size());

        for (String str : lore) {
            newLore.add(ChatColor.translateAlternateColorCodes('&', StringUtils.replaceEach(str, placeholders, replacers)));
        }

        meta.setLore(newLore);
        item.setItemMeta(meta);
    }

    private void addEnchantment(Enchantment enchantment) {
        addEnchantment(enchantment, 1);
    }

    private void addEnchantment(Enchantment enchantment, int level) {
        if (enchantment == null) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.addEnchant(enchantment, level, true);
        item.setItemMeta(meta);
    }

    private void setGlow() {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.addEnchant(Enchantment.LUCK, 1, false);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private void setCustomModelData(Integer value) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.setCustomModelData(value);
        item.setItemMeta(meta);
    }

    private void setSkullOwner(String owner) {
        if (owner == null || owner.isEmpty()) return;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta == null) return;

        meta.setOwner(owner);
        item.setItemMeta(meta);
    }

    private void setCustomTexture(String base64) {
        if (base64 == null || base64.isEmpty()) return;

        SkullMeta meta = (SkullMeta) item.getItemMeta();
        mutateItemMeta(meta, base64);
        item.setItemMeta(meta);
    }

    private void mutateItemMeta(SkullMeta meta, String b64) {
        try {
            if (metaSetProfileMethod == null) {
                metaSetProfileMethod = meta.getClass().getDeclaredMethod("setProfile", GameProfile.class);
                metaSetProfileMethod.setAccessible(true);
            }
            metaSetProfileMethod.invoke(meta, makeProfile(b64));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            // if in an older API where there is no setProfile method,
            // we set the profile field directly.
            try {
                if (metaProfileField == null) {
                    metaProfileField = meta.getClass().getDeclaredField("profile");
                    metaProfileField.setAccessible(true);
                }
                metaProfileField.set(meta, makeProfile(b64));

            } catch (NoSuchFieldException | IllegalAccessException ex2) {
                ex2.printStackTrace();
            }
        }
    }

    private GameProfile makeProfile(String b64) {
        // random uuid based on the b64 string
        UUID id = new UUID(
                b64.substring(b64.length() - 20).hashCode(),
                b64.substring(b64.length() - 10).hashCode()
        );
        GameProfile profile = new GameProfile(id, "Player");
        profile.getProperties().put("textures", new Property("textures", b64));
        return profile;
    }

    public ItemStack build() {
        return item.clone();
    }

    public Integer getSlot() {
        return slot;
    }

    public List<InventoryUtils.Action> getActions() {
        return actions;
    }
}