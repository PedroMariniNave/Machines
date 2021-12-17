package com.zpedroo.voltzmachines.objects;

public class Bonus {

    private final String permission;
    private final double bonusPercentage;

    public Bonus(String permission, double bonusPercentage) {
        this.permission = permission;
        this.bonusPercentage = bonusPercentage;
    }

    public String getPermission() {
        return permission;
    }

    public double getBonusPercentage() {
        return bonusPercentage;
    }
}