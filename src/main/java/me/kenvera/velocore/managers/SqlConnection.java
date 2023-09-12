package me.kenvera.velocore.managers;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.kenvera.velocore.VeloCore;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class SqlConnection {
    private final VeloCore plugin;
    private static HikariDataSource dataSource;
    public SqlConnection(VeloCore plugin) {
        this.plugin = plugin;

        plugin.getLogger().warn(plugin.getPrefix() + "§eConnection Pool Initiating!");
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
            plugin.getLogger().info(plugin.getPrefix() + "§9Connection Pool Established!");
        } catch (SQLException e) {
            plugin.getLogger().error(plugin.getPrefix() + "§cConnection Pool Initiation Failed!" + e);
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
            plugin.getLogger().info(plugin.getPrefix() + "§cConnection Pool Closed!");
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
            plugin.getLogger().info(plugin.getPrefix() + "§9Loaded Table " + "'player_data'");
        } catch (SQLException e) {
            plugin.getLogger().error(plugin.getPrefix() + "§9Database Generation Failed!" + e);
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
                plugin.getPlayerStaffChat().put(uuid, staffChat);
                plugin.getPlayerStaffChatMute().put(uuid, staffChatMute);
            }
        }

    }

    public void loadStaffData() {
        plugin.getLogger().warn(plugin.getPrefix() + "§eLoading Staff Data...");
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT uuid, staff_channel, staff_muted FROM staff_data");
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                UUID uuid = UUID.fromString(resultSet.getString("uuid"));
                boolean staffChat = resultSet.getBoolean("staff_channel");
                boolean staffChatMute = resultSet.getBoolean("staff_muted");
                plugin.getPlayerStaffChat().put(uuid, staffChat);
                plugin.getPlayerStaffChatMute().put(uuid, staffChatMute);
            }
            plugin.getLogger().info(plugin.getPrefix() + "§9Table Loaded Successfuly!");
        } catch (SQLException e) {
            plugin.getLogger().error(plugin.getPrefix() + "§eError when loading Staff Data!" + e);
            e.printStackTrace();
        }
    }

    public void saveStaffData() {
        plugin.getLogger().warn(plugin.getPrefix() + "§eInserting Data into Table!");

        if (!plugin.getPlayerStaffChat().isEmpty()) {
            try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO phoenix.staff_data (uuid, player_name, staff_channel, staff_muted) " +
                                "VALUES (?, ?, ?, ?) " +
                                "ON DUPLICATE KEY UPDATE staff_channel = VALUES(staff_channel), staff_muted = VALUES(staff_muted)")) {
                for (Map.Entry<UUID, Boolean> entry : plugin.getPlayerStaffChat().entrySet()) {
                    UUID uuid = entry.getKey();
                    String playerName = plugin.getProxy().getPlayer(uuid).get().getGameProfile().getName();
                    boolean staffChat = entry.getValue();
                    boolean staffChatMute = plugin.getPlayerStaffChatMute().getOrDefault(uuid, false);

                    statement.setString(1, uuid.toString());
                    statement.setString(2, playerName);
                    statement.setBoolean(3, staffChat);
                    statement.setBoolean(4, staffChatMute);

                    statement.executeUpdate();

                    plugin.getLogger().info(plugin.getPrefix() + "§9Inserted Record Successfully!");
                }
            } catch (SQLException e) {
                plugin.getLogger().error(plugin.getPrefix() + "§can Error Occured When Inserting Record!" + e);
                e.printStackTrace();
            }
        }
    }
}
