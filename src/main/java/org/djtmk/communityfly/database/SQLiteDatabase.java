package org.djtmk.communityfly.database;

import org.bukkit.plugin.java.JavaPlugin;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class SQLiteDatabase implements Database {
    private final JavaPlugin plugin;
    private Connection connection;

    public SQLiteDatabase(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void initialize() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/flight_times.db");
            try (PreparedStatement stmt = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS flight_times (uuid TEXT PRIMARY KEY, time REAL)")) {
                stmt.execute();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite database: " + e.getMessage());
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
                "INSERT OR REPLACE INTO flight_times (uuid, time) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, time);
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
            plugin.getLogger().warning("Error closing SQLite database: " + e.getMessage());
        }
    }
}