package com.zpedroo.voltzmachines.utils.config;

import com.zpedroo.voltzmachines.utils.FileUtils;
import org.bukkit.ChatColor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class Settings {

    public static final BigInteger MIN_PRICE = new BigInteger(FileUtils.get().getString(FileUtils.Files.CONFIG, "Settings.price.min"));

    public static final BigInteger MAX_PRICE = new BigInteger(FileUtils.get().getString(FileUtils.Files.CONFIG, "Settings.price.max"));

    public static final Integer STACK_RADIUS = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.stack-radius");

    public static final Integer MACHINE_UPDATE = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.machine-update");

    public static final Integer TAX_REMOVE_STACK = FileUtils.get().getInt(FileUtils.Files.CONFIG, "Settings.tax-remove-stack");

    public static final Long SAVE_INTERVAL = FileUtils.get().getLong(FileUtils.Files.CONFIG, "Settings.save-interval");

    public static final String[] MACHINE_HOLOGRAM = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Settings.hologram")).toArray(new String[256]);

    public static Long NEXT_UPDATE = FileUtils.get().getLong(FileUtils.Files.CONFIG, "Settings.next-update"); // not final

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