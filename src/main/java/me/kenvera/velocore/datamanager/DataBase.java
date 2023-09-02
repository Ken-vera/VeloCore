package me.kenvera.velocore.datamanager;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class DataBase {
    private final ProxyServer proxy;
    private static Connection connection;
    private final Map<UUID, Boolean> playerStaffChat;
    public DataBase(ProxyServer proxy, Map<UUID, Boolean> playerStaffChat) {
        this.proxy = proxy;
        this.playerStaffChat = playerStaffChat;
    }

    public void connect(String databasePath) throws SQLException {
        if (connection != null) {
            return;
        }
        try {
            Class.forName("org.sqlite.JDBC");
            String url = "jdbc:sqlite:" + databasePath;
            connection = DriverManager.getConnection(url);
            createTables();
            proxy.getConsoleCommandSource().sendMessage(Component.text("Database Connection Initiated!"));
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() throws SQLException {
        if (connection != null) {
            connection.close();
            proxy.getConsoleCommandSource().sendMessage(Component.text("Database Connection Closed!"));
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public void createTables() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "staff_channel BOOLEAN" +
                    "staff_muted BOOLEAN" +
                    ")");
            proxy.getConsoleCommandSource().sendMessage(Component.text("Database Table Not Found!"));
            proxy.getConsoleCommandSource().sendMessage(Component.text("Created new Database Table"));
        }
    }

    public void loadStaffData() {
        try {
            connection = getConnection();
            try (PreparedStatement statement = connection.prepareStatement("SELECT uuid, staff_channel FROM player_data")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        boolean staffChat = resultSet.getBoolean("staff_channel");
                        playerStaffChat.put(uuid, staffChat);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveStaffData() {
        try {
            connection = getConnection();
            try (PreparedStatement statement = connection.prepareStatement("INSERT OR REPLACE INTO player_data (uuid, staff_channel) VALUES (?, ?)")) {
                for (Map.Entry<UUID, Boolean> entry : playerStaffChat.entrySet()) {
                    UUID uuid = entry.getKey();
                    boolean staffChat = entry.getValue();
                    statement.setString(1, uuid.toString());
                    statement.setBoolean(2, staffChat);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
