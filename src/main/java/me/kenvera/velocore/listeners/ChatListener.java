package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;

import java.util.concurrent.TimeUnit;

public class ChatListener {
    private final VeloCore plugin;

    public ChatListener(VeloCore plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void mutePlayer(PlayerChatEvent e) {
        Player player = e.getPlayer();
        Long mute = plugin.getPlayerData().isMuted(player);

        if (mute != null) {
            if (mute >= System.currentTimeMillis()) {
                Long duration = TimeUnit.MILLISECONDS.toSeconds(mute - System.currentTimeMillis());
                e.setResult(PlayerChatEvent.ChatResult.denied());
                player.sendMessage(Component.text("§cWhoops! You're temporarily muted. Chat freedom returns in §7" + duration + "s"));
            }
        }
    }
}
