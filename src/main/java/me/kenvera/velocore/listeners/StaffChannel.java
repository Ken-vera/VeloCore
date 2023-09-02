package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.discordshake.DiscordConnection;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class StaffChannel {
    private final ProxyServer proxy;
    private final Map<UUID, Boolean> playerStaffChat;
    private final LuckPerms luckPerms;
    private final DiscordConnection discordConnection;
    public StaffChannel(ProxyServer proxy, Map<UUID, Boolean> playerStaffChat, DiscordConnection discordConnection) {
        this.proxy = proxy;
        this.playerStaffChat = playerStaffChat;
        this.luckPerms = LuckPermsProvider.get();
        this.discordConnection = discordConnection;
    }

    @Subscribe
    public void onStaffChat(PlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID uuid = sender.getUniqueId();
        String message = event.getMessage().replaceAll("&&", "§");
        User user = luckPerms.getUserManager().getUser(uuid);
        String server = sender.getCurrentServer().get().getServerInfo().getName();

        if (sender.getPermissionValue("velocity.staff") == Tristate.TRUE) {
            assert user != null;
            CachedMetaData metaData = user.getCachedData().getMetaData();
            String prefix = Objects.requireNonNull(metaData.getPrefix()).replaceAll("&", "§");
            boolean currentStatus = playerStaffChat.getOrDefault(uuid, false);

            if (currentStatus) {
                for (Player player : proxy.getAllPlayers()) {
                    if (player.getPermissionValue("velocity.staff") == Tristate.TRUE) {
                        event.setResult(PlayerChatEvent.ChatResult.denied());
                        player.sendMessage(Component.text("§7[§cStaffChat§7] [§6" + server.toUpperCase() + "§7] " + prefix + " " + sender.getUsername() + " : §7" + message));
                    }
                }
                System.out.println("§7[§cStaffChat§7] [§6" + server.toUpperCase() + "§7] " + prefix + " " + sender.getUsername() + " : §7" + message);
                message = "[" + server + "] " + prefix.replaceAll("§.", "") + " " + sender.getUsername() + " : " + message;
                sendDiscordChat(sender.getUsername(), message);
            }
        }
    }

    public void sendChat(String sender, String message) {
        for (Player player : proxy.getAllPlayers()) {
            if (player.getPermissionValue("velocity.staff") == Tristate.TRUE) {
                player.sendMessage(Component.text("§7[§cStaffChat§7] [§6Discord§7] " + sender + " : §7" + message));
            }
        }
    }

    public void sendDiscordChat(String sender, String message) {
        String targetChannelId = "1145334804913586216"; // Replace with your target channel ID

        // Get the TextChannel instance using the provided channel ID
        TextChannel targetChannel = discordConnection.jda.getTextChannelById(targetChannelId);

        if (targetChannel != null) {
            MessageCreateData data = new MessageCreateBuilder()
                    .setContent(message)
                    .setSuppressedNotifications(true)
                    .build();
            targetChannel.sendMessage(data).queue();
        } else {
            System.out.println("Target channel not found.");
        }
    }
}
