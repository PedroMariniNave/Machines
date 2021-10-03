package com.zpedroo.voltzmachines.objects;

import com.zpedroo.voltzmachines.utils.enums.Action;
import org.bukkit.entity.Player;

import java.math.BigInteger;

public class PlayerChat {

    private Player player;
    private PlayerMachine playerMachine;
    private Machine machine;
    private BigInteger price;
    private Action action;

    public PlayerChat(Player player, PlayerMachine playerMachine, Action action) {
        this.player = player;
        this.playerMachine = playerMachine;
        this.action = action;
    }

    public PlayerChat(Player player, Machine machine, BigInteger price, Action action) {
        this.player = player;
        this.machine = machine;
        this.price = price;
        this.action = action;
    }

    public Player getPlayer() {
        return player;
    }

    public PlayerMachine getPlayerMachine() {
        return playerMachine;
    }


    public Machine getMachine() {
        return machine;
    }

    public BigInteger getPrice() {
        return price;
    }

    public Action getAction() {
        return action;
    }
}