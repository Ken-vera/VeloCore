package me.kenvera.velocore.managers;

import me.kenvera.velocore.VeloCore;
import redis.clients.jedis.Jedis;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class BanManager {
    private final VeloCore plugin;
    public static final String BANED_CRITERIA = "purged = 0 and ((expire > ?) or (expire = NULL))";
    public static final String SET_TIMEZONE = "SET time_zone = '+07:00'";
    private static final String INSERT_BAN = "INSERT INTO phoenix.ban (uuid, player_name, issuer, reason, expire, banned_time) VALUES (?, ?, ?, ?, ?, ?)";
    private static final String GET_BAN = "SELECT id, reason, issuer, expire, banned_time FROM phoenix.ban WHERE " + BANED_CRITERIA + " and uuid = ? LIMIT 1";
    private static final String GET_BAN_HISTORY = "SELECT id, reason, until, bannedBy, reducedUntil, issuedAt, purged, reducedBy  FROM ban_bans WHERE user = ?";
    private static final String SET_USERNAME = "INSERT INTO ban_nameCache (user, username) VALUES (?, ?)";
    private static final String UPDATE_USERNAME = "UPDATE ban_nameCache SET username=? WHERE user=?";
    private static final String GET_USERNAME = "SELECT player_name FROM phoenix.player_data WHERE uuid=? LIMIT 1";
    private static final String GET_UUID = "SELECT uuid FROM phoenix.player_data WHERE player_name=? LIMIT 1";
    private static final String GET_ID = "SELECT id FROM phoenix.ban WHERE " + BANED_CRITERIA + " and uuid = ? LIMIT 1";
    private static final String PURGE_BANS = "UPDATE ban_bans SET purged=? WHERE " + BANED_CRITERIA + " and user = ?";
    private static final String PURGE_ID = "UPDATE phoenix.ban SET purged=?, purger=? WHERE uuid = ? AND id=?";
    private static final String PURGE_PLAYER = "UPDATE phoenix.ban SET purged=? WHERE uuid = ? AND id=?";
    private static final String REDUCE_BANS = "UPDATE ban_bans SET reducedUntil=?, reducedBy=?, reducedAt=? WHERE " + BANED_CRITERIA + " AND user=?";
    private static final String GET_USERNAMES_BASE = "SELECT username FROM ban_bans INNER JOIN ban_nameCache ON ban_bans.user = ban_nameCache.user WHERE GROUP BY username";
    private static final String GET_BAN_COUNT = "SELECT count(*) FROM ban_bans WHERE " + BANED_CRITERIA;

    public BanManager(VeloCore plugin) {
        this.plugin = plugin;

        try {
            PreparedStatement timezoneStatement = plugin.getSqlConnection().getConnection().prepareStatement(SET_TIMEZONE);
            timezoneStatement.executeUpdate();
            timezoneStatement.close();
        } catch (SQLException e) {
            // Handle any exceptions related to setting the timezone
            e.printStackTrace();
        }
    }

    //    REDIS CACHE
    public void addBanRedis(String uuid, String playerName, String issuer, String reason, long expire) {
        try (Jedis jedis = plugin.getRedisConnection().getJedis().getResource()) {
            String banKey = "ban:" + uuid;
            String expireTime = String.valueOf(expire);

            jedis.hset(banKey, "player_name", playerName);
            jedis.hset(banKey, "issuer", issuer);
            jedis.hset(banKey, "reason", reason);
            jedis.hset(banKey, "expire", expireTime);
            jedis.hset(banKey, "banned_time", String.valueOf(System.currentTimeMillis()));

            jedis.expire(banKey, ((expire - System.currentTimeMillis()) / 1000));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Ban getBanExpire(String uuid) {
        try (Jedis jedis = plugin.getRedisConnection().getJedis().getResource()) {
            String banKey = "ban:" + uuid;
            long id = -1;
            String issuer = jedis.hget(banKey, "issuer");
            String reason = jedis.hget(banKey, "reason");
            String expire = jedis.hget(banKey, "expire");
            long expireMillis = Long.parseLong(expire);
            long bannedTimeMillis = Long.parseLong(jedis.hget(banKey, "banned_time"));

            if (expire != null ) {
                return new Ban(
                        id,
                        uuid,
                        issuer,
                        reason,
                        expireMillis,
                        bannedTimeMillis);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //    MYSQL CACHE
    public void addBanSql(String uuid, String playerName, String issuer, String reason, long expire){
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(INSERT_BAN)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setString(3, issuer);
            statement.setString(4, reason);
            statement.setString(5, Utils.parseDateTime(expire, true));
            statement.setString(6, Utils.parseDateTime(System.currentTimeMillis(), true));

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addBanSql(String uuid, String playerName, String issuer, String reason) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_BAN)) {

            statement.setString(1, uuid.toString());
            statement.setString(2, playerName);
            statement.setString(3, issuer);
            statement.setString(4, reason);
            statement.setString(5, null);
            statement.setString(6, Utils.parseDateTime(System.currentTimeMillis(), true));

            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Ban getBan(String uuid){
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_BAN)) {

             statement.setString(1, Utils.parseDateTime(System.currentTimeMillis(), true));
             statement.setString(2, uuid);
             ResultSet result = statement.executeQuery();
             if (result.next()) {
                 long expireMillis;
                 if (result.getTimestamp("expire") != null) {
                     expireMillis = result.getTimestamp("expire").toInstant().minusMillis(25200000).toEpochMilli();
                 } else {
                     expireMillis = -1;
                 }
                 long bannedTimeMillis = result.getTimestamp("banned_time").toInstant().minusMillis(25200000).toEpochMilli();

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
                return result.getString("player_name");
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

    public void unBan(String uuid, String issuer, long id) throws SQLException {
        PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement(PURGE_ID);
        statement.setBoolean(1, true);
        statement.setString(2, issuer);
        statement.setString(3, uuid);
        statement.setLong(4, id);
        statement.executeUpdate();
    }

    public long getID(String uuid) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(GET_ID)) {
            ZonedDateTime currentTime = ZonedDateTime.now(ZoneId.of("Asia/Bangkok"));

            statement.setTimestamp(1, Timestamp.valueOf(currentTime.toLocalDateTime()));
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
}
