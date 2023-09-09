package me.kenvera.velocore.managers;

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
        config.setMaximumPoolSize(20); // Adjust the pool size as needed
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://100.126.17.140:3306/phoenix?useSSL=false");
        config.setMaxLifetime(30000);
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

    public int getActiveConnections() {
        return dataSource.getHikariPoolMXBean().getActiveConnections();
    }

    public int getTotalConnections() {
        return dataSource.getHikariPoolMXBean().getTotalConnections();
    }

    public int getIdleConnections() {
        return dataSource.getHikariPoolMXBean().getIdleConnections();
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

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void loadTables() {
        String sql = "CREATE TABLE IF NOT EXISTS player_data (uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(18), staff_channel BOOLEAN, staff_muted BOOLEAN)";

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
             statement.execute(sql);
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Loaded Table " + "'player_data'"));
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Database Generation Failed!"));
            e.printStackTrace();
        }
    }

    public void testDb() throws SQLException{
        String sql = "SELECT uuid, staff_channel, staff_muted FROM player_data";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                boolean staffChat = resultSet.getBoolean("staff_channel");
                boolean staffChatMute = resultSet.getBoolean("staff_muted");
                playerStaffChat.put(uuid, staffChat);
                playerStaffChatMute.put(uuid, staffChatMute);
            }
        }

    }

    public void loadStaffData() {
        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eLoading Staff Data..."));
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, staff_channel, staff_muted FROM staff_data");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                boolean staffChat = resultSet.getBoolean("staff_channel");
                boolean staffChatMute = resultSet.getBoolean("staff_muted");
                playerStaffChat.put(uuid, staffChat);
                playerStaffChatMute.put(uuid, staffChatMute);
            }
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Table Loaded Successfuly!"));
        } catch (SQLException e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eError when loading Staff Data!"));
            e.printStackTrace();
        }
    }

    public void saveStaffData() {
        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eInserting Data into Table!"));

        if (!playerStaffChat.isEmpty()) {
            try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO phoenix.staff_data (uuid, player_name, staff_channel, staff_muted) " +
                                "VALUES (?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE staff_channel = VALUES(staff_channel), staff_muted = VALUES(staff_muted)")) {
                for (Map.Entry<UUID, Boolean> entry : playerStaffChat.entrySet()) {
                    UUID uuid = entry.getKey();
                    String playerName = proxy.getPlayer(uuid).get().getGameProfile().getName();
                    boolean staffChat = entry.getValue();
                    boolean staffChatMute = playerStaffChatMute.getOrDefault(uuid, false);

                    statement.setString(1, uuid.toString());
                    statement.setString(2, playerName);
                    statement.setBoolean(3, staffChat);
                    statement.setBoolean(4, staffChatMute);

                    statement.executeUpdate();

                    proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Record Inserted Successfully!"));
                }
            } catch (SQLException e) {
                proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §can Error Occured When Inserting Record!"));
                e.printStackTrace();
            }
        }
    }
}
