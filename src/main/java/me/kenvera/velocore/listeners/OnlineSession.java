package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.UUID;

public class OnlineSession {

    private final VeloCore plugin;
    private final ProxyServer proxy;

    public OnlineSession(VeloCore plugin) {
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
}
