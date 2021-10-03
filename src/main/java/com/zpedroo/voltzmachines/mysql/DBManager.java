package com.zpedroo.voltzmachines.mysql;

import com.zpedroo.voltzmachines.managers.DataManager;
import com.zpedroo.voltzmachines.objects.Machine;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.objects.PlayerMachine;
import org.bukkit.Location;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class DBManager extends DataManager {

    public void saveMachine(PlayerMachine machine) {
        if (contains(serializeLocation(machine.getLocation()), "location")) {
            String query = "UPDATE `" + DBConnection.TABLE + "` SET" +
                    "`location`='" + serializeLocation(machine.getLocation()) + "', " +
                    "`uuid`='" + machine.getOwnerUUID().toString() + "', " +
                    "`stack`='" + machine.getStack().toString() + "', " +
                    "`fuel`='" + machine.getFuel().toString() + "', " +
                    "`drops`='" + machine.getDrops().toString() + "', " +
                    "`integrity`='" + machine.getIntegrity().toString() + "', " +
                    "`type`='" + machine.getMachine().getType() + "', " +
                    "`managers`='" + serializeManagers(machine.getManagers()) + "', " +
                    "`infinite_fuel`='" + (machine.hasInfiniteFuel() ? 1 : 0) + "', " +
                    "`infinite_integrity`='" + (machine.hasInfiniteIntegrity() ? 1 : 0) + "' " +
                    "WHERE `location`='" + serializeLocation(machine.getLocation()) + "';";
            executeUpdate(query);
            return;
        }

        String query = "INSERT INTO `" + DBConnection.TABLE + "` (`location`, `uuid`, `stack`, `fuel`, `drops`, `integrity`, `type`, `managers`, `infinite_fuel`, `infinite_integrity`) VALUES " +
                "('" + serializeLocation(machine.getLocation()) + "', " +
                "'" + machine.getOwnerUUID().toString() + "', " +
                "'" + machine.getStack().toString() + "', " +
                "'" + machine.getFuel().toString() + "', " +
                "'" + machine.getDrops().toString() + "', " +
                "'" + machine.getIntegrity().toString() + "', " +
                "'" + machine.getMachine().getType() + "', " +
                "'" + serializeManagers(machine.getManagers()) + "', " +
                "'" + (machine.hasInfiniteFuel() ? 1 : 0) + "', " +
                "'" + (machine.hasInfiniteIntegrity() ? 1 : 0) + "');";
        executeUpdate(query);
    }

    public void deleteMachine(String location) {
        String query = "DELETE FROM `" + DBConnection.TABLE + "` WHERE `location`='" + location + "';";
        executeUpdate(query);
    }

    public Map<Location, PlayerMachine> getPlacedMachines() {
        Map<Location, PlayerMachine> machines = new HashMap<>(5120);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        String query = "SELECT * FROM `" + DBConnection.TABLE + "`;";

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(query);
            result = preparedStatement.executeQuery();

            while (result.next()) {
                Location location = deserializeLocation(result.getString(1));
                UUID ownerUUID = UUID.fromString(result.getString(2));
                BigDecimal stack = result.getBigDecimal(3);
                BigDecimal fuel = result.getBigDecimal(4);
                BigDecimal drops = result.getBigDecimal(5);
                Integer integrity = result.getInt(6);
                Machine machine = getMachine(result.getString(7));
                List<Manager> managers = deserializeManagers(result.getString(8));
                Boolean infiniteFuel = result.getBoolean(9);
                Boolean infiniteIntegrity = result.getBoolean(10);
                PlayerMachine playerMachine = new PlayerMachine(location, ownerUUID, stack.toBigInteger(), fuel.toBigInteger(), drops.toBigInteger(), integrity, machine, managers, infiniteFuel, infiniteIntegrity);

                machines.put(location, playerMachine);

                List<PlayerMachine> machinesList = getCache().getPlayerMachinesByUUID(ownerUUID);
                machinesList.add(playerMachine);

                getCache().setUUIDMachines(ownerUUID, machinesList);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnections(connection, result, preparedStatement, null);
        }

        return machines;
    }

    private Boolean contains(String value, String column) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        String query = "SELECT `" + column + "` FROM `" + DBConnection.TABLE + "` WHERE `" + column + "`='" + value + "';";
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(query);
            result = preparedStatement.executeQuery();
            return result.next();
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            closeConnections(connection, result, preparedStatement, null);
        }

        return false;
    }

    private void executeUpdate(String query) {
        Connection connection = null;
        Statement statement = null;
        try {
            connection = getConnection();
            statement = connection.createStatement();
            statement.executeUpdate(query);
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            closeConnections(connection, null, null, statement);
        }
    }

    private void closeConnections(Connection connection, ResultSet resultSet, PreparedStatement preparedStatement, Statement statement) {
        try {
            if (connection != null) connection.close();
            if (resultSet != null) resultSet.close();
            if (preparedStatement != null) preparedStatement.close();
            if (statement != null) statement.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    protected void createTable() {
        String query = "CREATE TABLE IF NOT EXISTS `" + DBConnection.TABLE + "` (`location` VARCHAR(255), `uuid` VARCHAR(255), `stack` DECIMAL(40,0), `fuel` DECIMAL(40,0), `drops` DECIMAL(40,0), `integrity` INTEGER, `type` VARCHAR(32), `managers` LONGTEXT, `infinite_fuel` BOOLEAN, `infinite_integrity` BOOLEAN, PRIMARY KEY(`location`));";
        executeUpdate(query);
    }

    private Connection getConnection() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }
}