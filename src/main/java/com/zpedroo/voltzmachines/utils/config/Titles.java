package com.zpedroo.voltzmachines.utils.config;

import com.zpedroo.voltzmachines.FileUtils;
import org.bukkit.ChatColor;

public class Titles {

    public static final String WHEN_SELL_TITLE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Titles.when-sell.title"));

    public static final String WHEN_SELL_SUBTITLE = getColored(FileUtils.get().getString(FileUtils.Files.CONFIG, "Titles.when-sell.subtitle"));

    private static String getColored(String str) {
        return ChatColor.translateAlternateColorCodes('&', str);
    }
}