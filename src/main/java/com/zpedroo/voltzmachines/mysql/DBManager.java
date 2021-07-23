package com.zpedroo.voltzmachines.mysql;

import com.zpedroo.voltzmachines.managers.MachineManager;
import com.zpedroo.voltzmachines.machine.Machine;
import com.zpedroo.voltzmachines.objects.Manager;
import com.zpedroo.voltzmachines.machine.PlayerMachine;
import org.bukkit.Location;

import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DBManager {

    private MachineManager manager;

    public DBManager() {
        this.manager = new MachineManager();
    }

    public void saveMachine(PlayerMachine machine) {
        if (contains(getManager().serializeLocation(machine.getLocation()), "location")) {
            String query = "UPDATE `" + DBConnection.TABLE + "` SET" +
                    "`location`='" + getManager().serializeLocation(machine.getLocation()) + "', " +
                    "`uuid`='" + machine.getOwnerUUID().toString() + "', " +
                    "`stack`='" + machine.getStack().toString() + "', " +
                    "`fuel`='" + machine.getFuel().toString() + "', " +
                    "`drops`='" + machine.getDrops().toString() + "', " +
                    "`integrity`='" + machine.getIntegrity().toString() + "', " +
                    "`type`='" + machine.getMachine().getType() + "', " +
                    "`managers`='" + getManager().serializeManagers(machine.getManagers()) + "' " +
                    "WHERE `location`='" + getManager().serializeLocation(machine.getLocation()) + "';";
            executeUpdate(query);
            return;
        }

        String query = "INSERT INTO `" + DBConnection.TABLE + "` (`location`, `uuid`, `stack`, `fuel`, `drops`, `integrity`, `type`, `managers`) VALUES " +
                "('" + getManager().serializeLocation(machine.getLocation()) + "', " +
                "'" + machine.getOwnerUUID().toString() + "', " +
                "'" + machine.getStack().toString() + "', " +
                "'" + machine.getFuel().toString() + "', " +
                "'" + machine.getDrops().toString() + "', " +
                "'" + machine.getIntegrity().toString() + "', " +
                "'" + machine.getMachine().getType() + "', " +
                "'" + getManager().serializeManagers(machine.getManagers()) + "');";
        executeUpdate(query);
    }

    public void deleteMachine(String location) {
        String query = "DELETE FROM `" + DBConnection.TABLE + "` WHERE `location`='" + location + "';";
        executeUpdate(query);
    }

    public HashMap<Location, PlayerMachine> getPlacedMachines() {
        HashMap<Location, PlayerMachine> machines = new HashMap<>(5120);

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet result = null;
        String query = "SELECT * FROM `" + DBConnection.TABLE + "`;";

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(query);
            result = preparedStatement.executeQuery();

            while (result.next()) {
                Location location = getManager().deserializeLocation(result.getString(1));
                UUID ownerUUID = UUID.fromString(result.getString(2));
                BigDecimal stack = result.getBigDecimal(3);
                BigDecimal fuel = result.getBigDecimal(4);
                BigDecimal drops = result.getBigDecimal(5);
                Integer integrity = result.getInt(6);
                Machine machine = getManager().getMachine(result.getString(7));
                List<Manager> managers = getManager().deserializeManagers(result.getString(8));
                PlayerMachine playerMachine = new PlayerMachine(location, ownerUUID, stack.toBigInteger(), fuel.toBigInteger(), drops.toBigInteger(), integrity, machine, managers);

                machines.put(location, playerMachine);

                List<PlayerMachine> machinesList = getManager().getDataCache().getPlayerMachinesByUUID(ownerUUID);
                machinesList.add(playerMachine);

                getManager().getDataCache().setUUIDMachines(ownerUUID, machinesList);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            closeConnection(connection, result, preparedStatement, null);
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
            closeConnection(connection, result, preparedStatement, null);
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
            closeConnection(connection, null, null, statement);
        }
    }

    private void closeConnection(Connection connection, ResultSet resultSet, PreparedStatement preparedStatement, Statement statement) {
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
        String query = "CREATE TABLE IF NOT EXISTS `" + DBConnection.TABLE + "` (`location` VARCHAR(255) NOT NULL, `uuid` VARCHAR(255) NOT NULL, `stack` DECIMAL(40,0) NOT NULL, `fuel` DECIMAL(40,0) NOT NULL, `drops` DECIMAL(40,0) NOT NULL, `integrity` INTEGER NOT NULL, `type` VARCHAR(32) NOT NULL, `managers` LONGTEXT NOT NULL, PRIMARY KEY(`location`));";
        executeUpdate(query);
    }

    private Connection getConnection() throws SQLException {
        return DBConnection.getInstance().getConnection();
    }

    private MachineManager getManager() {
        return manager;
    }
}