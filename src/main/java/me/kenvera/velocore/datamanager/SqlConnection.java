package me.kenvera.velocore.datamanager;

import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class SqlConnection {
    private final ProxyServer proxy;
    private final Map<UUID, Boolean> playerStaffChat;
    private final Map<UUID, Boolean> playerStaffChatMute;
    private static HikariDataSource dataSource;
    public SqlConnection(ProxyServer proxy, Map<UUID, Boolean> playerStaffChat, Map<UUID, Boolean> playerStaffChatMute) {
        this.proxy = proxy;
        this.playerStaffChat = playerStaffChat;
        this.playerStaffChatMute = playerStaffChatMute;

        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eConnection Pool Initiating!"));
        HikariConfig config = new HikariConfig();
        config.setUsername("global-oska");
        config.setPassword("44213a6fa5b7d019f387478d6db19a6e70e27d5f2215928d1da52c1371fa93a9");
        config.setConnectionTimeout(10000);
        config.setMaximumPoolSize(10); // Adjust the pool size as needed
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://100.126.17.140:3306/phoenix?useSSL=false");
        dataSource = new HikariDataSource(config);

        try {
            Connection connection = dataSource.getConnection();
            closeConnection(connection);
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Connection Pool Established!"));
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cConnection Pool Initiation Failed!"));
            e.printStackTrace();
        }
    }

    public void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cConnection Pool Closed!"));
        }
    }

    public void closeConnection(Connection connection) {
        dataSource.evictConnection(connection);
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void loadTables() {
        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS player_data (uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(18), staff_channel BOOLEAN, staff_muted BOOLEAN)");
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cDatabase Table Not Found!"));
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eGenerating one..."));
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Generated New Database Table!"));
            e.printStackTrace();
        }
    }

    public void loadStaffData() {
        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eLoading Database!"));
        try {
            try (PreparedStatement statement = dataSource.getConnection().prepareStatement("SELECT uuid, staff_channel, staff_muted FROM player_data")) {
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                        boolean staffChat = resultSet.getBoolean("staff_channel");
                        boolean staffChatMute = resultSet.getBoolean("staff_muted");
                        playerStaffChat.put(uuid, staffChat);
                        playerStaffChatMute.put(uuid, staffChatMute);
                    }
                }
                proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Database Loaded Successfuly!"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                dataSource.getConnection().close();
            } catch (SQLException e) {
                // Handle exceptions
            }
        }
    }

    public void saveStaffData() {
        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eSending Data into Database!"));
        try {
            try (PreparedStatement insertStatement = dataSource.getConnection().prepareStatement(
                    "INSERT INTO phoenix.player_data (uuid, player_name, staff_channel, staff_muted) " +
                            "VALUES (?, ?, ?, ?) "
            );
            PreparedStatement updateStatement = dataSource.getConnection().prepareStatement(
                    "UPDATE phoenix.player_data SET staff_channel = ?, staff_muted = ? WHERE uuid = ?"
            )) {
                if (!playerStaffChat.isEmpty()) {
                    for (Map.Entry<UUID, Boolean> entry : playerStaffChat.entrySet()) {
                        UUID uuid = entry.getKey();
                        boolean staffChat = entry.getValue();
                        boolean staffChatMute = playerStaffChatMute.getOrDefault(uuid, false);

                        if (doesUUIDExist(uuid.toString())) {
                            updateStatement.setBoolean(1, staffChat);
                            updateStatement.setBoolean(2, staffChatMute);
                            updateStatement.setString(3, uuid.toString());
                            updateStatement.executeUpdate();
                            proxy.getConsoleCommandSource().sendMessage(Component.text("Updated record for UUID: " + uuid));
                        } else {
                            insertStatement.setString(1, uuid.toString());
                            insertStatement.setString(2, "Kenvera");
                            insertStatement.setBoolean(3, staffChat);
                            insertStatement.setBoolean(4, staffChatMute);
                            insertStatement.executeUpdate();
                            proxy.getConsoleCommandSource().sendMessage(Component.text("Inserted new record for UUID: " + uuid));
                        }
                    }
                    proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Database Saved Successfully!"));
                }
            }
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("Data save error occured!"));
            e.printStackTrace();
        }
    }

    private boolean doesUUIDExist(String uuid) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM phoenix.player_data WHERE uuid = ?"
             )) {
            preparedStatement.setString(1, uuid);

            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    int count = resultSet.getInt(1);
                    return count > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }
}
