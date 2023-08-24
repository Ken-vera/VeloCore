package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

import java.util.Map;
import java.util.UUID;

public class OnlineSession {

    private final Map<UUID, Long> playerOnlineSession;

    public OnlineSession(Map<UUID, Long> playerOnlineSession) {
        this.playerOnlineSession = playerOnlineSession;
    }

    @Subscribe
    public void onServerChange(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        playerOnlineSession.put(uuid, System.currentTimeMillis());
    }
}
