package org.djtmk.communityfly.database;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class MySQLDatabase implements Database {
    private final JavaPlugin plugin;
    private Connection connection;

    public MySQLDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String dbName = plugin.getConfig().getString("database.mysql.database", "communityfly");
        String user = plugin.getConfig().getString("database.mysql.user", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "");
        String url = "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?useSSL=false";

        try {
            connection = DriverManager.getConnection(url, user, password);
            try (PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS flight_times (uuid VARCHAR(36) PRIMARY KEY, time DOUBLE)")) {
                stmt.execute();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize MySQL database: " + e.getMessage());
        }
    }

    @Override
    public double getFlightTime(UUID uuid) {
        try (PreparedStatement stmt = connection.prepareStatement("SELECT time FROM flight_times WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("time");
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error reading flight time: " + e.getMessage());
        }
        return 0.0;
    }

    @Override
    public void setFlightTime(UUID uuid, double time) {
        try (PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO flight_times (uuid, time) VALUES (?, ?) ON DUPLICATE KEY UPDATE time = ?")) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, time);
            stmt.setDouble(3, time);
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().warning("Error saving flight time: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error closing MySQL database: " + e.getMessage());
        }
    }
}