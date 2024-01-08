package me.kenvera.velocore.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.Component;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.util.Objects;
import java.util.UUID;

public class StaffChannel {
    private final VeloCore plugin;
    public StaffChannel(VeloCore plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onStaffChat(PlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID uuid = sender.getUniqueId();
        String message = event.getMessage().replaceAll("&&", "§");
        User user = plugin.getLuckPerms().getUserManager().getUser(uuid);
        String server = sender.getCurrentServer().get().getServerInfo().getName();

        if (sender.getPermissionValue("velocity.staff") == Tristate.TRUE) {
            assert user != null;
            CachedMetaData metaData = user.getCachedData().getMetaData();
            String prefix = Objects.requireNonNull(metaData.getPrefix()).replaceAll("&", "§");
            boolean currentStatus = plugin.getPlayerStaffChat().getOrDefault(uuid, false);
            boolean muteStatus = plugin.getPlayerStaffChatMute().getOrDefault(uuid, false);

            if (currentStatus) {
                for (Player player : plugin.getProxy().getAllPlayers()) {
                    if (player.getPermissionValue("velocity.staff") == Tristate.TRUE) {
                        if (muteStatus) {
                            event.setResult(PlayerChatEvent.ChatResult.denied());
                            message = "§k" + message.replaceAll("\\.", "§k$0");
                            player.sendMessage(Component.text("§7[§cStaffChat§7] [§6" + server.toUpperCase() + "§7] " + prefix + " " + sender.getUsername() + " : §6" + message));
                        } else {
                            event.setResult(PlayerChatEvent.ChatResult.denied());
                            player.sendMessage(Component.text("§7[§cStaffChat§7] [§6" + server.toUpperCase() + "§7] " + prefix + " " + sender.getUsername() + " : §6" + message));
                        }
                    }
                }
                System.out.println("§7[§cStaffChat§7] [§6" + server.toUpperCase() + "§7] " + prefix + " " + sender.getUsername() + " : §6" + message);
                message = plugin.getConfigManager().getString("discord.staff-channel-prefix", null).replaceAll("%server%", server) + prefix.replaceAll("§", "") + " " + sender.getUsername() + " : §7" + message;
                sendDiscordChat(sender.getUsername(), message);
            }
        }
    }

    public void sendChat(String sender, String message) {
        for (Player player : plugin.getProxy().getAllPlayers()) {
            if (player.getPermissionValue("velocity.staff") == Tristate.TRUE) {
                player.sendMessage(Component.text("§7[§cStaffChat§7] [§6Discord§7] " + sender + " : §7" + message));
            }
        }
    }

    public void sendDiscordChat(String sender, String message) {
        Long targetChannelId = plugin.getConfigManager().getLong("discord.staff-channel-id");
        TextChannel targetChannel = plugin.getDiscordConnection().jda.getTextChannelById(targetChannelId);

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
