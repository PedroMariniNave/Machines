package com.zpedroo.voltzmachines.utils.config;

import com.zpedroo.voltzmachines.utils.FileUtils;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;

public class Messages {

    public static final String ZERO_MACHINES_FUEL = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.zero-machines-fuel"));

    public static final String ZERO_MACHINES_REPAIR = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.zero-machines-repair"));

    public static final String SUCCESSFUL_FUELED = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.successful-fueled"));

    public static final String SUCCESSFUL_REPAIRED = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.successful-repaired"));

    public static final String NEED_PERMISSION = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.need-permission"));

    public static final String REMOVE_STACK_MIN = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.remove-stack-min"));

    public static final String REMOVE_STACK_SUCCESSFUL = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.remove-stack-successful"));

    public static final String INCORRECT_PICKAXE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.incorrect-pickaxe"));

    public static final String ONLY_OWNER = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.only-owner"));

    public static final String OFFLINE_PLAYER = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.offline-player"));

    public static final String INVALID_MACHINE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.invalid-machine"));

    public static final String INVALID_AMOUNT = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.invalid-amount"));

    public static final String MACHINE_USAGE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.machine-usage"));

    public static final String HAS_PERMISSION = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.has-permission"));

    public static final String MACHINE_PERMISSION = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.machine-permission"));

    public static final String BUY_ALL_ZERO = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.buy-all-zero"));

    public static final String INSUFFICIENT_MONEY = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.insufficient-money"));

    public static final String NEAR_MACHINE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.near-machine"));

    public static final String ENABLED = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.enabled"));

    public static final String DISABLED = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.disabled"));

    public static final String TRUE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.true"));

    public static final String FALSE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.false"));

    public static final String WAIT = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Messages.wait"));

    public static final String SECOND = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.second"));

    public static final String SECONDS = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.seconds"));

    public static final String MINUTE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.minute"));

    public static final String MINUTES = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.minutes"));

    public static final String HOUR = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.hour"));

    public static final String HOURS = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.hours"));

    public static final String DAY = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.day"));

    public static final String DAYS = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.days"));

    public static final String NOW = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Time-Formatter.now"));

    public static final List<String> NEW_QUOTATION = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Messages.new-quotation"));

    public static final List<String> ADD_FRIEND = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Messages.add-friend"));

    public static final List<String> CHOOSE_AMOUNT = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Messages.choose-amount"));

    public static final List<String> SUCCESSFUL_PURCHASED = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Messages.successful-purchased"));

    public static final List<String> REMOVE_STACK = getColored(FileUtils.get().getStringList(FileUtils.Files.CONFIG, "Messages.remove-stack"));

    private static String getColored(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }

    private static List<String> getColored(List<String> list) {
        List<String> colored = new ArrayList<>(list.size());
        for (String str : list) {
            colored.add(getColored(str));
        }

        return colored;
    }
}
