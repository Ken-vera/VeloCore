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

    private static final String CREATE_TABLE_PLAYER_DATA = "CREATE TABLE IF NOT EXISTS phoenix.player_data (uuid VARCHAR(36) PRIMARY KEY, player_name VARCHAR(18), staff_channel BOOLEAN, staff_muted BOOLEAN)";
    private static final String CREATE_TABLE_BAN = "CREATE TABLE IF NOT EXISTS phoenix.ban (id INT(10) NOT NULL PRIMARY KEY AUTO_INCREMENT, uuid CHAR(50) NOT NULL, player_name CHAR(50) NOT NULL, issuer CHAR(50) NOT NULL, reason CHAR(50) NOT NULL)";
    private static final String INSERT_BAN = "INSERT INTO ban_bans (user, until, bannedBy, reason, issuedAt) VALUES (?, ?, ?, ?, ?)";
//    private static final String GET_BAN = "SELECT id, reason, until, bannedBy, reducedUntil, issuedAt FROM ban_bans WHERE " + BANED_CRITERIA + " and user = ? LIMIT 1";
    private static final String GET_BAN_HISTORY = "SELECT id, reason, until, bannedBy, reducedUntil, issuedAt, purged, reducedBy  FROM ban_bans WHERE user = ?";
    private static final String SET_USERNAME = "INSERT INTO ban_nameCache (user, username) VALUES (?, ?)";
    private static final String UPDATE_USERNAME = "UPDATE ban_nameCache SET username=? WHERE user=?";
    private static final String GET_USERNAME = "SELECT username FROM ban_nameCache WHERE user=? LIMIT 1";
    private static final String GET_UUID = "SELECT user FROM ban_nameCache WHERE username=? LIMIT 1";
//    private static final String PURGE_BANS = "UPDATE ban_bans SET purged=? WHERE " + BANED_CRITERIA + " and user = ?";
    private static final String PURGE_BAN = "UPDATE ban_bans SET purged=? WHERE user = ? AND id=?";
//    private static final String REDUCE_BANS = "UPDATE ban_bans SET reducedUntil=?, reducedBy=?, reducedAt=? WHERE " + BANED_CRITERIA + " AND user=?";
    private static final String GET_USERNAMES_BASE = "SELECT username FROM ban_bans INNER JOIN ban_nameCache ON ban_bans.user = ban_nameCache.user WHERE GROUP BY username";
//    private static final String GET_BAN_COUNT = "SELECT count(*) FROM ban_bans WHERE " + BANED_CRITERIA;

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
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
             statement.executeUpdate(CREATE_TABLE_PLAYER_DATA);
             statement.executeUpdate(CREATE_TABLE_BAN);
            plugin.getLogger().info(plugin.getPrefix() + "§9Loaded Table " + "'player_data'");
        } catch (SQLException e) {
            plugin.getLogger().error(plugin.getPrefix() + "§9Database Generation Failed!" + e);
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
