package com.zpedroo.voltzmachines.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private static Economy economy;

    public void hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;

        economy = rsp.getProvider();
    }

    public static double getMoney(Player player) {
        return economy.getBalance(player);
    }

    public static void removeMoney(Player player, double amount) {
        economy.withdrawPlayer(player.getName(), player.getWorld().getName(), amount);
    }
}