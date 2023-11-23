package me.kenvera.velocore.managers;

import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlayerData {
    private final VeloCore plugin;
    private static final String GET_GROUP = "SELECT `group` FROM phoenix.player_data WHERE uuid=? LIMIT 1";
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

    public boolean isOnGroup(Player player, String group) {
        if (player.hasPermission("group." + group)) {
            return true;
        } else {
            return false;
        }
    }
}
