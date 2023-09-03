package me.kenvera.velocore.datamanager;

import com.velocitypowered.api.proxy.ProxyServer;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class DataBase {
    private final ProxyServer proxy;
    private final Map<UUID, Boolean> playerStaffChat;
    private final Map<UUID, Boolean> playerStaffChatMute;
    private final HikariDataSource dataSource;
    public DataBase(ProxyServer proxy, Map<UUID, Boolean> playerStaffChat, Map<UUID, Boolean> playerStaffChatMute) {
        this.proxy = proxy;
        this.playerStaffChat = playerStaffChat;
        this.playerStaffChatMute = playerStaffChatMute;

        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eConnection Pool Initiating!"));
        HikariConfig config = new HikariConfig();
        config.setUsername("global-oska");
        config.setPassword("44213a6fa5b7d019f387478d6db19a6e70e27d5f2215928d1da52c1371fa93a9");
        config.setConnectionTimeout(7000);
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

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
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
            try (PreparedStatement statement = dataSource.getConnection().prepareStatement("INSERT INTO player_data (uuid, player_name, staff_channel, staff_muted) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE staff_channel = VALUES(staff_channel), staff_muted = VALUES(staff_muted)")) {
                for (Map.Entry<UUID, Boolean> entry : playerStaffChat.entrySet()) {
                    UUID uuid = entry.getKey();
                    String playerName = proxy.getPlayer(uuid).get().getUsername();
                    boolean staffChat = entry.getValue();
                    boolean staffChatMute = playerStaffChatMute.getOrDefault(uuid, false);
                    statement.setString(1, uuid.toString());
                    statement.setString(2, playerName);
                    statement.setBoolean(3, staffChat);
                    statement.setBoolean(4, staffChatMute);
                    statement.executeUpdate();
                }
                proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Database Saved Successfully!"));
            }
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cFailed Sending Data into Database!"));
            e.printStackTrace();
        } finally {
            try {
                dataSource.getConnection().close();
            } catch (SQLException e) {
                // Handle exceptions
            }
        }
    }

    public void saveStaffSession(UUID uuid, String playerName) {
        try {
            try (PreparedStatement statement = dataSource.getConnection().prepareStatement(
                    "INSERT INTO player_data (uuid, player_name, staff_channel, staff_muted) VALUES (?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE staff_channel = ?, staff_muted = ?")) {
                boolean staffChat = playerStaffChat.getOrDefault(uuid, false);
                boolean staffChatMute = playerStaffChatMute.getOrDefault(uuid, false);
                statement.setString(1, uuid.toString());
                statement.setString(2, playerName);
                statement.setBoolean(3, staffChat);
                statement.setBoolean(4, staffChatMute);
                statement.setBoolean(5, staffChat);
                statement.setBoolean(6, staffChatMute);
                statement.execute();
                closeConnection(dataSource.getConnection());
            }
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cError Saving Staff Data" + proxy.getPlayer(uuid).get().getUsername()));
            e.printStackTrace();
        } finally {
            try {
                dataSource.getConnection().close();
            } catch (SQLException e) {
                // Handle exceptions
            }
        }
    }

    public void loadStaffSession(UUID uuid) {
        try {
            try (PreparedStatement statement = dataSource.getConnection().prepareStatement("SELECT staff_channel, staff_muted FROM player_data WHERE uuid = ?")) {
                statement.setString(1, uuid.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        boolean staffChat = resultSet.getBoolean("staff_channel");
                        boolean staffChatMute = resultSet.getBoolean("staff_muted");
                        playerStaffChat.put(uuid, staffChat);
                        playerStaffChatMute.put(uuid, staffChatMute);

                    }
                }
            }
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cError Loading Staff Data" + proxy.getPlayer(uuid).get().getUsername()));
            e.printStackTrace();
        } finally {
            try {
                dataSource.getConnection().close();
            } catch (SQLException e) {
                // Handle exceptions
            }
        }
    }
}
