package com.zpedroo.voltzmachines.utils.config;

import com.zpedroo.voltzmachines.FileUtils;
import org.bukkit.ChatColor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    public static BigInteger MIN_PRICE = new BigInteger(FileUtils.get().getString(FileUtils.Files.CONFIG, "Settings.price.min"));

    public static BigInteger MAX_PRICE = new BigInteger(FileUtils.get().getString(FileUtils.Files.CONFIG, "Settings.price.max"));

    public static Integer STACK_RADIUS = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.stack-radius");

    public static Integer MACHINE_UPDATE = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.machine-update");

    public static Integer TAX_REMOVE_STACK = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.tax-remove-stack");

    public static Long SAVE_INTERVAL = FileUtils.get().getLong(FileUtils.Files.CONFIG, "Settings.save-interval");

    public static Long NEXT_UPDATE = FileUtils.get().getLong(FileUtils.Files.CONFIG, "Settings.next-update");

    public static String[] MACHINE_HOLOGRAM = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Settings.hologram")).toArray(new String[256]);

    private static String getColored(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    private static List<String> getColored(List<String> list) {
        List<String> colored = new ArrayList<>();
        for (String str : list) {
            colored.add(getColored(str));
        }

        return colored;
    }
}