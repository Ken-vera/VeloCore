package me.kenvera.velocore.managers;

import me.kenvera.velocore.VeloCore;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BanManager {
    private final VeloCore plugin;
//    public static final String BANED_CRITERIA = "purged = 0 and ((reducedUntil is NULL and (until = -1 or until > ?)) or (reducedUntil = -1 or reducedUntil > ?))";
    public static final String BANED_CRITERIA = "purged = 0 and ((expire < ?) or (expire = -1))";
    private static final String INSERT_BAN = "INSERT INTO phoenix.ban (uuid, player_name, issuer, reason, expire, banned_time) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String GET_BAN = "SELECT id, reason, issuer, expire, banned_time FROM phoenix.ban WHERE " + BANED_CRITERIA + " and uuid = ? LIMIT 1";
    private static final String GET_BAN_HISTORY = "SELECT id, reason, until, bannedBy, reducedUntil, issuedAt, purged, reducedBy  FROM ban_bans WHERE user = ?";
    private static final String SET_USERNAME = "INSERT INTO ban_nameCache (user, username) VALUES (?, ?)";
    private static final String UPDATE_USERNAME = "UPDATE ban_nameCache SET username=? WHERE user=?";
    private static final String GET_USERNAME = "SELECT player_name FROM phoenix.player_data WHERE uuid=? LIMIT 1";
    private static final String GET_UUID = "SELECT uuid FROM phoenix.player_data WHERE player_name=? LIMIT 1";
    private static final String PURGE_BANS = "UPDATE ban_bans SET purged=? WHERE " + BANED_CRITERIA + " and user = ?";
    private static final String PURGE_ID = "UPDATE phoenix.ban SET purged=? WHERE uuid = ? AND id=?";
    private static final String PURGE_PLAYER = "UPDATE phoenix.ban SET purged=? WHERE uuid = ? AND id=?";
    private static final String REDUCE_BANS = "UPDATE ban_bans SET reducedUntil=?, reducedBy=?, reducedAt=? WHERE " + BANED_CRITERIA + " AND user=?";
    private static final String GET_USERNAMES_BASE = "SELECT username FROM ban_bans INNER JOIN ban_nameCache ON ban_bans.user = ban_nameCache.user WHERE GROUP BY username";
    private static final String GET_BAN_COUNT = "SELECT count(*) FROM ban_bans WHERE " + BANED_CRITERIA;

    public BanManager(VeloCore plugin) {
        this.plugin = plugin;
    }

    public void addBan(String uuid, String playerName, String issuer, String reason, Long duration) throws SQLException {
        Long expire = System.currentTimeMillis() + duration;
        PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement(INSERT_BAN);
        statement.setString(1, uuid.toString());
        statement.setString(2, playerName);
        statement.setString(3, issuer);
        statement.setString(4, reason);
        statement.setString(5, expire.toString());
        statement.executeUpdate();
    }

    public void addBan(String uuid, String playerName, String issuer, String reason) throws SQLException {
        PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement(INSERT_BAN);
        statement.setString(1, uuid.toString());
        statement.setString(2, playerName);
        statement.setString(3, issuer);
        statement.setString(4, reason);
        statement.setLong(5, -1);
        statement.setString(6, String.valueOf(System.currentTimeMillis()));
        statement.executeUpdate();
    }

    public Ban getBan(String uuid) throws SQLException{
        PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement(GET_BAN);
        statement.setLong(1, System.currentTimeMillis());
        statement.setString(2, uuid);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return new Ban(result.getLong("id"), uuid, result.getString("issuer"), result.getString("reason"), result.getLong("expire"), result.getLong("banned_time"));
        } else {
            return null;
        }
    }

    public String getUsername(String uuid) throws SQLException {
        PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement(GET_USERNAME);
        statement.setString(1, uuid);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return result.getString("player_name");
        } else {
            return null;
        }
    }

    public String getUUID(String playerName) throws SQLException {
        PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement(GET_UUID);
        statement.setString(1, playerName);
        ResultSet result = statement.executeQuery();
        if (result.next()) {
            return result.getString("uuid");
        } else {
            return null;
        }
    }

    public void unBan(String uuid, String issuer, int id) throws SQLException {
        PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement(PURGE_ID);
        statement.setString(1, issuer);
        statement.setString(2, uuid);
        statement.setInt(3, id);
        statement.executeUpdate()
    }
}
