package com.zpedroo.voltzmachines.hooks;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook {

    private static VaultHook instance;
    public static VaultHook get() { return instance; }

    private static Economy economy;

    public VaultHook() {
        instance = this;
        this.hook();
    }

    public double getMoney(Player player) {
        return economy.getBalance(player);
    }

    public void removeMoney(Player player, double amount) {
        economy.withdrawPlayer(player.getName(), player.getWorld().getName(), amount);
    }

    private void hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return;

        economy = rsp.getProvider();
    }
}