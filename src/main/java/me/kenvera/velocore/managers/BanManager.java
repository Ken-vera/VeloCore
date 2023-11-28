package me.kenvera.velocore.managers;

import me.kenvera.velocore.VeloCore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BanManager {
    private final VeloCore plugin;
    private static final String BANNED_CRITERIA = "purged = 0 and ((expire > ?) or (expire = -1))";
    private static final String SET_TIMEZONE = "SET time_zone = '+07:00'";
    private static final String INSERT_BAN = "INSERT INTO CNS1_cnplayerdata_1.ban (uuid, username, issuer, reason, expire, banned_time) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String GET_BAN = "SELECT id, reason, issuer, expire, banned_time FROM CNS1_cnplayerdata_1.ban WHERE " + BANNED_CRITERIA + " and uuid = ? LIMIT 1";
    private static final String GET_BANNED = "SELECT DISTINCT username FROM CNS1_cnplayerdata_1.ban WHERE " + BANNED_CRITERIA;
    private static final String GET_BAN_HISTORY = "SELECT id, reason, until, bannedBy, reducedUntil, issuedAt, purged, reducedBy  FROM ban_bans WHERE uuid = ?";
    private static final String GET_USERNAME = "SELECT username FROM CNS1_cnplayerdata_1.player_data WHERE uuid=? LIMIT 1";
    private static final String GET_UUID = "SELECT uuid FROM CNS1_cnplayerdata_1.player_data WHERE username=? LIMIT 1";
    private static final String GET_ID = "SELECT id FROM CNS1_cnplayerdata_1.ban WHERE " + BANNED_CRITERIA + " and uuid = ? LIMIT 1";
    private static final String PURGE_BANS = "UPDATE ban_bans SET purged=? WHERE " + BANNED_CRITERIA + " and user = ?";
    private static final String UNBAN_ID = "UPDATE CNS1_cnplayerdata_1.ban SET purged = ?, purged_time = ?, purger = ? WHERE uuid = ? AND id = ?";
    private static final String REDUCE_BANS = "UPDATE ban_bans SET reducedUntil=?, reducedBy=?, reducedAt=? WHERE " + BANNED_CRITERIA + " AND user=?";
    private static final String GET_BAN_COUNT = "SELECT count(*) FROM ban_bans WHERE " + BANNED_CRITERIA;

    public BanManager(VeloCore plugin) {
        this.plugin = plugin;

        try {
            PreparedStatement timezoneStatement = plugin.getSqlConnection().getConnection().prepareStatement(SET_TIMEZONE);
            timezoneStatement.executeUpdate();
            timezoneStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addBan(String uuid, String playerName, String issuer, String reason, long expire){
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_BAN)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setString(3, issuer);
            statement.setString(4, reason);
            statement.setLong(5, expire);
            statement.setLong(6, System.currentTimeMillis());

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Ban getBan(String uuid){
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_BAN)) {

             statement.setLong(1, System.currentTimeMillis());
             statement.setString(2, uuid);
             ResultSet result = statement.executeQuery();
             if (result.next()) {
                 long expireMillis = result.getLong("expire");
                 long bannedTimeMillis = result.getLong("banned_time");

                 return new Ban(
                        result.getLong("id"),
                        uuid,
                        result.getString("issuer"),
                        result.getString("reason"),
                        expireMillis,
                        bannedTimeMillis);
             } else {
                 return null;
             }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUsername(String uuid) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_USERNAME)) {

            statement.setString(1, uuid);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("username");
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getUUID(String playerName) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_UUID)) {

            statement.setString(1, playerName);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("uuid");
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void unBan(String uuid, String issuer, long id) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(UNBAN_ID)) {

            statement.setBoolean(1, true);
            statement.setLong(2, System.currentTimeMillis());
            statement.setString(3, issuer);
            statement.setString(4, uuid);
            statement.setLong(5, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public long getID(String uuid) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(GET_ID)) {

            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, uuid);
            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getLong("id");
            } else {
                return 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<String> getBanned() {
        List<String> banned = new ArrayList<>();

        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_BANNED)) {

            statement.setLong(1, System.currentTimeMillis());

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                banned.add(result.getString("username"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return banned;
    }
}
