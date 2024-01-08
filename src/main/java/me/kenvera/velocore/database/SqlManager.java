package me.kenvera.velocore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.kenvera.velocore.VeloCore;

import java.sql.*;
import java.util.Map;
import java.util.UUID;

public class SqlManager {
    private final VeloCore plugin;
    private static HikariDataSource dataSource;

    private static final String CREATE_TABLE_STAFF_DATA = "CREATE TABLE IF NOT EXISTS CNS1_cnplayerdata_1.staff_data (uuid VARCHAR(36) PRIMARY KEY, staff_username VARCHAR(18), staff_channel BOOLEAN, staff_muted BOOLEAN)";
    private static final String CREATE_TABLE_PLAYER_DATA = "CREATE TABLE IF NOT EXISTS CNS1_cnplayerdata_1.player_data (uuid VARCHAR(36) PRIMARY KEY, username VARCHAR(18), `group` VARCHAR(255), whitelisted BOOLEAN, muted LONGTEXT, first_join LONGTEXT, last_join LONGTEXT)";
    private static final String CREATE_TABLE_STAFF_HISTORY = "CREATE TABLE IF NOT EXISTS CNS1_cnplayerdata_1.staff_log (id INT AUTO_INCREMENT PRIMARY KEY, staff_username VARCHAR(36), staff_rank VARCHAR(18), staff_command TEXT, timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP)";
    private static final String CREATE_TABLE_BAN = "CREATE TABLE IF NOT EXISTS CNS1_cnplayerdata_1.ban (id INT AUTO_INCREMENT PRIMARY KEY, uuid VARCHAR(36) NOT NULL, username VARCHAR(50) NOT NULL, issuer VARCHAR(50) NOT NULL, reason VARCHAR(50) NOT NULL, expire LONGTEXT NOT NULL, banned_time LONGTEXT NOT NULL, purged BOOLEAN NOT NULL DEFAULT false, purged_time LONGTEXT, purger VARCHAR(50), global BOOLEAN NOT NULL DEFAULT false, mix BOOLEAN NOT NULL DEFAULT false)";
    private static final String INSERT_STAFF_DATA = "INSERT INTO CNS1_cnplayerdata_1.staff_data (uuid, staff_username, staff_channel, staff_muted) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE staff_channel = VALUES(staff_channel), staff_muted = VALUES(staff_muted)";

    public SqlManager(VeloCore plugin) {
        this.plugin = plugin;

        String host = plugin.getConfigManager().getString("mysql.host", null);
        String database = plugin.getConfigManager().getString("mysql.database", null);

        plugin.getLogger().warn(plugin.getPrefix() + "§eConnection Pool initiating!");
        HikariConfig config = new HikariConfig();
        config.setUsername(plugin.getConfigManager().getString("mysql.user", null));
        config.setPassword(plugin.getConfigManager().getString("mysql.password", null));
        config.setConnectionTimeout(10000);
        config.setMaximumPoolSize(20);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl("jdbc:mysql://" + host + ":3306/" + database + "?allowPublicKeyRetrieval=true&useSSL=false");
        config.setMaxLifetime(30000);
        dataSource = new HikariDataSource(config);

        try {
            Connection connection = dataSource.getConnection();
            closeConnection(connection);
            plugin.getLogger().info(plugin.getPrefix() + "§9Connection Pool established!");
        } catch (SQLException e) {
            plugin.getLogger().error(plugin.getPrefix() + "§cConnection Pool initiation failed!" + e);
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
            plugin.getLogger().info(plugin.getPrefix() + "§cConnection Pool closed!");
        }
    }

    public void closeConnection(Connection connection) {
        dataSource.evictConnection(connection);
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void loadTables() {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
             statement.executeUpdate(CREATE_TABLE_PLAYER_DATA);
             plugin.getLogger().info(plugin.getPrefix() + "§9Loaded Table " + "'player_data'");
        } catch (SQLException e) {
             plugin.getLogger().error(plugin.getPrefix() + "§9Database generation failed!");
             e.printStackTrace();
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
             statement.executeUpdate(CREATE_TABLE_STAFF_DATA);
             plugin.getLogger().info(plugin.getPrefix() + "§9Loaded Table " + "'staff_data'");
        } catch (SQLException e) {
             plugin.getLogger().error(plugin.getPrefix() + "§9Database generation failed!");
             e.printStackTrace();
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
             statement.executeUpdate(CREATE_TABLE_STAFF_HISTORY);
             plugin.getLogger().info(plugin.getPrefix() + "§9Loaded Table " + "'staff_history'");
        } catch (SQLException e) {
             plugin.getLogger().error(plugin.getPrefix() + "§9Database generation failed!");
             e.printStackTrace();
        }

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
             statement.executeUpdate(CREATE_TABLE_BAN);
             plugin.getLogger().info(plugin.getPrefix() + "§9Loaded Table " + "'ban'");
        } catch (SQLException e) {
             plugin.getLogger().error(plugin.getPrefix() + "§9Database generation failed!");
             e.printStackTrace();
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
            plugin.getLogger().info(plugin.getPrefix() + "§9Loaded Staff Data!");
        } catch (SQLException e) {
            plugin.getLogger().error(plugin.getPrefix() + "§eError when loading Staff Data!" + e);
            e.printStackTrace();
        }
    }

    public void saveStaffData() {
        plugin.getLogger().warn(plugin.getPrefix() + "§eSaving Staff Data...");

        if (!plugin.getPlayerStaffChat().isEmpty()) {
            try (Connection connection = getConnection();
                PreparedStatement statement = connection.prepareStatement(INSERT_STAFF_DATA)) {
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

                    plugin.getLogger().info(plugin.getPrefix() + "§9Staff Data saved successfully!");
                }
            } catch (SQLException e) {
                plugin.getLogger().error(plugin.getPrefix() + "§can error occured when saving Staff Data!" + e);
                e.printStackTrace();
            }
        }
    }
}
