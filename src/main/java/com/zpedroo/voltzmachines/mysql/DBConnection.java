package com.zpedroo.voltzmachines.mysql;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

public class DBConnection {

    private static DBConnection instance;
    public static DBConnection getInstance() { return instance; }

    protected static final String TABLE = "machines";

    private HikariDataSource hikari;
    private DBManager dbManager;

    public DBConnection(FileConfiguration file) {
        instance = this;
        this.dbManager = new DBManager();
        this.hikari = new HikariDataSource();

        enable(file);
        getDBManager().createTable();
    }

    private void enable(FileConfiguration file) {
        hikari.setDataSourceClassName("com.mysql.cj.jdbc.MysqlDataSource");
        hikari.addDataSourceProperty("serverName", file.getString("MySQL.host"));
        hikari.addDataSourceProperty("port", file.getInt("MySQL.port"));
        hikari.addDataSourceProperty("databaseName", file.getString("MySQL.database"));
        hikari.addDataSourceProperty("user", file.getString("MySQL.username"));
        hikari.addDataSourceProperty("password", file.getString("MySQL.password"));
        hikari.setMaximumPoolSize(10);
    }

    public void closeConnection() {
        if (hikari == null) return;

        hikari.close();
    }

    public Connection getConnection() throws SQLException {
        return hikari.getConnection();
    }

    public DBManager getDBManager() {
        return dbManager;
    }
}