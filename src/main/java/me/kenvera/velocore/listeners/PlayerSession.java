package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.managers.Ban;
import me.kenvera.velocore.managers.Utils;
import net.kyori.adventure.text.Component;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;

import java.util.UUID;

public class PlayerSession {

    private final VeloCore plugin;
    private final ProxyServer proxy;

    public PlayerSession(VeloCore plugin) {
        this.plugin = plugin;
        this.proxy = plugin.getProxy();
    }

    @Subscribe
    public void onServerChange(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        plugin.getPlayerSession().put(uuid, System.currentTimeMillis());
    }

    @Subscribe
    public void onServerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        User user = plugin.getLuckPerms().getUserManager().getUser(uuid);

        String group = plugin.getPlayerData().getGroup(uuid.toString());
        Group assignGroup = plugin.getLuckPerms().getGroupManager().getGroup(group);

        if (!plugin.getPlayerData().isOnGroup(player, group)) {
            if (assignGroup != null) {
                assert user != null;
                user.data().clear(NodeType.INHERITANCE::matches);
                user.data().add(InheritanceNode.builder(assignGroup).build());
                plugin.getLuckPerms().getUserManager().saveUser(user);
            }
        }

        if (player.hasPermission("velocity.staff")) {
            for (Player staff : proxy.getAllPlayers()) {
                if (staff.hasPermission("velocity.staff")) {
                    staff.sendMessage(Component.text("§7" + player.getUsername() + " has joined the server"));
                }
            }
        }
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
    //        CHECK IF THERES A RECORD FROM REDIS FIRST
        Ban ban;
        String uuid = event.getPlayer().getUniqueId().toString();
        ban = plugin.getBanManager().getBanExpire(uuid);

        if (ban != null) {
            event.setResult(ResultedEvent.ComponentResult.denied(Utils.formatBannedMessage(ban, plugin)));
        } else {
            //        IF NULL THEN CHECK RECORD FROM MYSQL
            ban = plugin.getBanManager().getBan(uuid);

            if (ban != null) {
                event.setResult(ResultedEvent.ComponentResult.denied(Utils.formatBannedMessage(ban, plugin)));
            }
        }
    }
}
