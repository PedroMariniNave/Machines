package com.zpedroo.voltzmachines.utils.api;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import xyz.xenondevs.particle.utils.ReflectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;

public class OfflinePlayerAPI {

    private static Object worldType;
    private static Method getServer;
    private static Method getWorldServer;
    private static Method getBukkitEntity;
    private static Constructor<?> gameProfileConstructor;
    private static Constructor<?> entityPlayerConstructor;
    private static Constructor<?> playerInteractManagerConstructor;

    static {
        try {
            Class<?> PlayerInteractManagerClass = ReflectionUtils.getNMSClass("PlayerInteractManager");
            Class<?> MinecraftServerClass = ReflectionUtils.getNMSClass("MinecraftServer");
            Class<?> EntityPlayerClass = ReflectionUtils.getNMSClass("EntityPlayer");
            Class<?> WorldServerClass = ReflectionUtils.getNMSClass("WorldServer");
            Class<?> WorldClass = ReflectionUtils.getNMSClass("World");
            Class<?> WorldTypeClass;
            Class<?> gameProfileClass;

            WorldTypeClass = int.class;
            worldType = 0;

            gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            gameProfileConstructor = gameProfileClass.getConstructor(UUID.class, String.class);

            getServer = MinecraftServerClass.getMethod("getServer");
            getWorldServer = MinecraftServerClass.getMethod("getWorldServer", WorldTypeClass);
            getBukkitEntity = EntityPlayerClass.getMethod("getBukkitEntity");
            entityPlayerConstructor = EntityPlayerClass.getConstructor(MinecraftServerClass, WorldServerClass, gameProfileClass, PlayerInteractManagerClass);
            playerInteractManagerConstructor = PlayerInteractManagerClass.getConstructor(WorldClass);
        } catch (Throwable e) {
            // ignore
        }
    }

    @SuppressWarnings("deprecation")
    public static Player getPlayer(String playerName) {
        Player testPlayer = Bukkit.getPlayerExact(playerName);
        if (testPlayer != null) return testPlayer;

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (!offlinePlayer.hasPlayedBefore()) return null;

            Object uuid = offlinePlayer.getUniqueId();
            Object gameProfile = gameProfileConstructor.newInstance(uuid, playerName);
            Object minecraftServer = getServer.invoke(null);
            Object worldServer = getWorldServer.invoke(minecraftServer, worldType);
            Object playerInteractManager = playerInteractManagerConstructor.newInstance(worldServer);
            Object entityPlayer = entityPlayerConstructor.newInstance(minecraftServer, worldServer, gameProfile, playerInteractManager);

            Player player = (Player) getBukkitEntity.invoke(entityPlayer);
            player.loadData();

            return player;
        } catch (Throwable e) {
            e.printStackTrace();
        }

        return null;
    }
}