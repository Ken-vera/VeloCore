package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.UUID;

public class StaffChat {
    private final ProxyServer proxy;
    private final Map<UUID, Boolean> playerStaffChat;
    public StaffChat(ProxyServer proxy, Map<UUID, Boolean> playerStaffChat) {
        this.proxy = proxy;
        this.playerStaffChat = playerStaffChat;
    }

    @Subscribe
    public void onChat(PlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID uuid = sender.getUniqueId();
        String message = event.getMessage();

        if (playerStaffChat.get(uuid).equals(true)) {
            event.setResult(PlayerChatEvent.ChatResult.denied());
            for (Player player : proxy.getAllPlayers()) {
                if (player.hasPermission("velocity.staff")) {
                    player.sendMessage(Component.text("[Staff] " + message));
                }
            }
        }
    }
}
