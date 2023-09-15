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

import java.sql.SQLException;
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

        if (player.hasPermission("velocity.staff")) {
            for (Player staff : proxy.getAllPlayers()) {
                if (staff.hasPermission("velocity.staff")) {
                    staff.sendMessage(Component.text("§7" + player + " has joined the server"));
                }
            }
        }
    }

    @Subscribe
    public void onPlayerLogin(LoginEvent event) {
        try {
            Ban ban = plugin.getBanManager().getBan(event.getPlayer().getUniqueId().toString());
            if (ban != null) {
                event.setResult(ResultedEvent.ComponentResult.denied(Utils.formatBannedMessage(ban, plugin)));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
