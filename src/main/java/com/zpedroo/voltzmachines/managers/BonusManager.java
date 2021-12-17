package com.zpedroo.voltzmachines.managers;

import com.zpedroo.voltzmachines.objects.Bonus;
import com.zpedroo.voltzmachines.utils.api.OfflinePlayerAPI;
import org.bukkit.entity.Player;

import java.math.BigInteger;

public class BonusManager {

    public static BigInteger applyBonus(String playerName, BigInteger value) {
        Player player = OfflinePlayerAPI.getPlayer(playerName);
        if (player == null || getPlayerBonus(player) <= 0) return value;

        return value.add(getBonusByValue(playerName, value));
    }

    public static BigInteger getBonusByValue(String playerName, BigInteger value) {
        Player player = OfflinePlayerAPI.getPlayer(playerName);
        if (player == null || getPlayerBonus(player) <= 0) return BigInteger.ZERO;

        BigInteger bonusPercentage = new BigInteger(String.format("%.0f", getPlayerBonus(player)));

        return value.subtract(value.multiply(bonusPercentage).divide(BigInteger.valueOf(100)));
    }

    public static double getPlayerBonus(Player player) {
        double ret = 0;

        for (Bonus bonus : DataManager.getInstance().getCache().getBonuses()) {
            if (player.hasPermission(bonus.getPermission())) ret += bonus.getBonusPercentage();
        }

        return ret;
    }
}