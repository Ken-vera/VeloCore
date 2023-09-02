package me.kenvera.velocore.listeners;

import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.discordshake.DiscordConnection;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordChannel extends ListenerAdapter {
    private final VeloCore plugin;
    private final StaffChannel staffChannel;
    private final DiscordConnection discordConnection;

    public DiscordChannel(VeloCore plugin, StaffChannel staffChannel, DiscordConnection discordConnection) {
        this.plugin = plugin;
        this.staffChannel = staffChannel;
        this.discordConnection = discordConnection;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) { // Check if the sender is not a bot
            TextChannel targetChannel = discordConnection.jda.getTextChannelById("1145334804913586216"); // Replace with your target channel ID

            if (targetChannel != null && event.getChannel().equals(targetChannel)) {
                String author = event.getAuthor().getEffectiveName();
                String messageContent = event.getMessage().getContentDisplay();

                // Check if the author is not the bot
                if (!author.contains("PyroGuardian")) {
                    System.out.println("§7[§cStaffChat§7] [§6Discord§7] " + author + " : §7" + messageContent);
                    staffChannel.sendChat(author, messageContent);
                }
            }
        }
    }
}

