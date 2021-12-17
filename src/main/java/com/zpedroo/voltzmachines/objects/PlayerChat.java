package com.zpedroo.voltzmachines.objects;

import com.zpedroo.multieconomy.objects.Currency;
import com.zpedroo.voltzmachines.enums.PlayerAction;
import org.bukkit.entity.Player;

import java.math.BigInteger;

public class PlayerChat {

    private Player player;
    private PlacedMachine placedMachine;
    private Machine machine;
    private BigInteger price;
    private Currency currency;
    private PlayerAction playerAction;

    public PlayerChat(Player player, BigInteger price, Currency currency, PlayerAction playerAction) {
        this(player, null, null, price, currency, playerAction);
    }

    public PlayerChat(Player player, PlacedMachine placedMachine, PlayerAction playerAction) {
        this(player, placedMachine, null, null, null, playerAction);
    }

    public PlayerChat(Player player, Machine machine, BigInteger price, Currency currency, PlayerAction playerAction) {
        this(player, null, machine, price, currency, playerAction);
    }

    public PlayerChat(Player player, PlacedMachine placedMachine, Machine machine, BigInteger price, Currency currency, PlayerAction playerAction) {
        this.player = player;
        this.placedMachine = placedMachine;
        this.machine = machine;
        this.price = price;
        this.currency = currency;
        this.playerAction = playerAction;
    }

    public Player getPlayer() {
        return player;
    }

    public PlacedMachine getPlacedMachine() {
        return placedMachine;
    }

    public Machine getMachine() {
        return machine;
    }

    public BigInteger getPrice() {
        return price;
    }

    public Currency getCurrency() {
        return currency;
    }

    public PlayerAction getAction() {
        return playerAction;
    }
}