package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.managers.Ban;
import me.kenvera.velocore.managers.Utils;
import net.kyori.adventure.text.Component;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import xyz.kyngs.librelogin.api.provider.LibreLoginProvider;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
        var api = ((LibreLoginProvider<Player, RegisteredServer>) proxy.getPluginManager().getPlugin("librelogin").orElseThrow().getInstance().orElseThrow()).getLibreLogin();
        if (!api.getAuthorizationProvider().isAuthorized(player)) {
            System.out.println(player.getUsername() + " is not authorized");
            System.out.println(api.getAuthorizationProvider().isAuthorized(player));
            if (!event.getServer().getServerInfo().getName().equalsIgnoreCase("lobby")) {
                System.out.println(player.getUsername() + " joining lobby");
            }
        }

    }

    @Subscribe
    public void onServerJoin(PostLoginEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getUsername();
        UUID uuid = player.getUniqueId();
        User user = plugin.getLuckPerms().getUserManager().getUser(uuid);
        assert user != null;
        Set<String> inheritedGroups = user.getNodes(NodeType.INHERITANCE).stream()
                .map(InheritanceNode::getGroupName)
                .collect(Collectors.toSet());

        if (plugin.getPlayerData().isExist(uuid.toString(), playerName)) {
            List<String> groupData = List.of(plugin.getPlayerData().getGroup(uuid.toString()).split(","));
            for (String group : groupData) {
                Group assignGroup = plugin.getLuckPerms().getGroupManager().getGroup(group);

                if (!plugin.getPlayerData().isOnGroup(player, group)) {
                    if (assignGroup != null) {
                        user.data().add(InheritanceNode.builder(assignGroup).build());
                    }
                }
            }

            for (String group : inheritedGroups) {
                if (!groupData.contains(group) && !group.equalsIgnoreCase("default")) {
                    Group assignGroup = plugin.getLuckPerms().getGroupManager().getGroup("default");
                    user.data().clear(NodeType.INHERITANCE::matches);
                    assert assignGroup != null;
                    user.data().add(InheritanceNode.builder(assignGroup).build());
                }
            }
            plugin.getLuckPerms().getUserManager().saveUser(user);

            if (player.hasPermission("velocity.staff")) {
                for (Player staff : proxy.getAllPlayers()) {
                    if (staff.hasPermission("velocity.staff")) {
                        staff.sendMessage(Component.text(""));
                        staff.sendMessage(Component.text("§7" + player.getUsername() + " has joined the server"));
                        staff.sendMessage(Component.text(""));
                    }
                }
            }
        } else {
            plugin.getLogger().info("&b" + playerName + " &cplayer data is not found! Generating a new one...");
            plugin.getPlayerData().savePlayerData(uuid.toString(), playerName);
        }
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        String uuid = event.getPlayer().getUniqueId().toString();
        Ban ban = plugin.getBanManager().getBan(uuid);

        if (ban != null) {
            event.setResult(ResultedEvent.ComponentResult.denied(Utils.formatBannedMessage(ban, plugin)));
        }
    }
}
