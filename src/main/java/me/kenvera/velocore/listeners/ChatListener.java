package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import me.kenvera.velocore.VeloCore;

public class ChatListener {
    private final VeloCore plugin;

    public ChatListener(VeloCore plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void mutePlayer(PlayerChatEvent e) {
        e.setResult(PlayerChatEvent.ChatResult.denied());
    }
}
