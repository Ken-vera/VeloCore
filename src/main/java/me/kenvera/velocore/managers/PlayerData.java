package me.kenvera.velocore.managers;

import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.List;

public class PlayerData {
    private final VeloCore plugin;
    private static final String MUTED_CRITERIA = "muted > ? OR muted IS NOT NULL";
    private static final String GET_GROUP = "SELECT `group` FROM CNS1_cnplayerdata_1.player_data WHERE uuid = ? LIMIT 1";
    private static final String SET_GROUP = "INSERT INTO CNS1_cnplayerdata_1.player_data (uuid, `group`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `group` = VALUES(`group`)";
    private static final String ADD_GROUP = "INSERT INTO CNS1_cnplayerdata_1.player_data (uuid, `group`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `group` = VALUES(`group`)";
    private static final String REMOVE_GROUP = "INSERT INTO CNS1_cnplayerdata_1.player_data (uuid, `group`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `group` = VALUES(`group`)";
    private static final String GET_PLAYER_DATA = "SELECT COUNT(*) FROM CNS1_cnplayerdata_1.player_data WHERE uuid = ? AND username = ?";
    private static final String INSERT_PLAYER_DATA = "INSERT INTO CNS1_cnplayerdata_1.player_data (uuid, username, `group`, first_join) VALUES (?, ?, ?, ?)";
    private static final String GET_ID = "SELECT uuid FROM CNS1_cnplayerdata_1.player_data WHERE username = ?";
    private static final String GET_USERNAMES = "SELECT username FROM CNS1_cnplayerdata_1.player_data LIMIT 50 OFFSET 0";
    private static final String GET_USERNAMES_FILTER = "SELECT username FROM CNS1_cnplayerdata_1.player_data WHERE username COLLATE latin1_general_ci LIKE ? LIMIT 50 OFFSET 0";
    private static final String GET_MUTE = "SELECT muted FROM CNS1_cnplayerdata_1.player_data WHERE " + MUTED_CRITERIA + " and uuid = ?";
    private static final String INSERT_MUTE = "INSERT INTO CNS1_cnplayerdata_1.player_data (uuid, muted) VALUES (?, ?) ON DUPLICATE KEY UPDATE muted = VALUES (muted)";
    public PlayerData(VeloCore plugin) {
        this.plugin = plugin;
    }

    public String getPrefix(UUID uuid, boolean formatted) {
        User user = plugin.getLuckPerms().getUserManager().getUser(uuid);
        CachedMetaData metaData = user.getCachedData().getMetaData();
        if (metaData != null) {

            if (formatted) {
                String prefix = metaData.getPrefix().replaceAll("&", "§");
                return prefix;
            } else {
                String prefix = metaData.getPrefix().replace("&.", "");
                return prefix;
            }
        }
        return "";
    }

    public String getGroup(String uuid) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(GET_GROUP)) {

            statement.setString(1, uuid);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("group");
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setGroup(String uuid, String group) throws SQLException {
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(SET_GROUP)) {

            statement.setString(1, uuid);
            statement.setString(2, group);

            statement.executeUpdate();

            User user = plugin.getLuckPerms().getUserManager().getUser(UUID.fromString(uuid));
            Group assignGroup = plugin.getLuckPerms().getGroupManager().getGroup(group);
            if (assignGroup != null) {
                if (group.equalsIgnoreCase("default")) {
                    assert user != null;
                    user.data().clear(NodeType.INHERITANCE::matches);
                    plugin.getLuckPerms().getUserManager().saveUser(user);
                    plugin.getLogger().info("§aSuccesfully set " + user.getUsername() + "'s §aGroup to " + group);
                } else {
                    assert user != null;
                    user.data().clear(NodeType.INHERITANCE::matches);
                    user.data().add(InheritanceNode.builder(assignGroup).build());
                    plugin.getLuckPerms().getUserManager().saveUser(user);
                    plugin.getLogger().info("§aSuccesfully set " + user.getUsername() + "'s §aGroup to " + group);
                }
            }
        }
    }

    public void addGroup(String uuid, String group) throws SQLException {
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(ADD_GROUP)) {
            String[] currentGroups = getGroup(uuid).split(",");
            List<String> assignedGroups = new ArrayList<>();

            List<String> newGroups = new ArrayList<>(List.of(currentGroups));
            newGroups.add(group);

            String stringGroups = String.join(",", newGroups);
            statement.setString(1, uuid);
            statement.setString(2, stringGroups);

            statement.executeUpdate();

            User user = plugin.getLuckPerms().getUserManager().getUser(UUID.fromString(uuid));
            user.data().clear(NodeType.INHERITANCE::matches);
            for (String groupName : newGroups) {
                Group assignGroup = plugin.getLuckPerms().getGroupManager().getGroup(groupName);

                if (assignGroup != null) {
                    assert user != null;
                    user.data().add(InheritanceNode.builder(assignGroup).build());
                    assignedGroups.add(groupName);
                } else {
                    System.out.println("§cError processing " + group + " §cgroup!");
                }
            }
            plugin.getLuckPerms().getUserManager().saveUser(user);
            plugin.getLogger().info("§aSuccesfully set " + user.getUsername() + "'s §aGroup to " + assignedGroups);
        }
    }

    public void removeGroup(String uuid, String group) throws SQLException{
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(REMOVE_GROUP)) {
            String[] currentGroups = getGroup(uuid).split(",");

            List<String> newGroups = new ArrayList<>(List.of(currentGroups));
            newGroups.remove(group);

            String stringGroups = String.join(",", newGroups);
            statement.setString(1, uuid);
            statement.setString(2, stringGroups);

            statement.executeUpdate();

            User user = plugin.getLuckPerms().getUserManager().getUser(UUID.fromString(uuid));
            Group assignGroup = plugin.getLuckPerms().getGroupManager().getGroup(group);

            if (assignGroup != null) {
                assert user != null;
                user.data().remove(InheritanceNode.builder(assignGroup).build());
                plugin.getLuckPerms().getUserManager().saveUser(user);
                plugin.getLogger().info("§aSuccesfully remove " + group + " §afrom §b" + user.getUsername());
            } else {
                System.out.println("§cError processing " + group + " §cgroup!");
            }
        }
    }

    public void savePlayerData(String uuid, String playerName) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_PLAYER_DATA)) {

            statement.setString(1, uuid);
            statement.setString(2, playerName);
            statement.setString(3, "default");
            statement.setLong(4, System.currentTimeMillis());

            statement.executeUpdate();

            plugin.getLogger().info("&aGenerated &b" + playerName + " &aplayer data succesfully");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public String getUUID(String playerName) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(GET_ID)) {

            statement.setString(1, playerName);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                return result.getString("uuid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<String> getUsernames() {
        List<String> usernames = new ArrayList<>();

        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(GET_USERNAMES)) {

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                String playerName = result.getString("username");
                usernames.add(playerName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usernames;
    }

    public List<String> getUsernames(String filter) {
        List<String> usernames = new ArrayList<>();

        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(GET_USERNAMES_FILTER)) {

            statement.setString(1, filter + "%");

            ResultSet result = statement.executeQuery();
            while (result.next()) {
                String playerName = result.getString("username");
                usernames.add(playerName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return usernames;
    }

    public boolean isExist(String uuid, String playerName) {
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(GET_PLAYER_DATA)) {

            statement.setString(1, uuid);
            statement.setString(2, playerName);

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                int count = result.getInt(1);
                return count > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isOnGroup(Player player, String group) {
        return plugin.getLuckPerms().getUserManager().getUser(player.getUniqueId()).getPrimaryGroup().equalsIgnoreCase(group);
    }

    public Long isMuted(Player player) {
        System.out.println("ismuted " + player.getUsername());
        try (Connection connection = plugin.getSqlConnection().getConnection();
            PreparedStatement statement = connection.prepareStatement(GET_MUTE)) {

            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, player.getUniqueId().toString());

            ResultSet result = statement.executeQuery();
            if (result.next()) {
                Long expire = result.getLong("muted");
                return expire;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void setMuted(String uuid, Long duration) throws SQLException {
        System.out.println(uuid);
        try (Connection connection = plugin.getSqlConnection().getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_MUTE)) {

            statement.setString(1, uuid);
            if (duration != null) {
                statement.setLong(2, duration);
            } else {
                statement.setString(2, null);
            }

            statement.executeUpdate();
        }
    }
}
