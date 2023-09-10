package me.kenvera.velocore.listeners;

import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.discordshake.DiscordConnection;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordChannel extends ListenerAdapter {
    private final VeloCore plugin;
    private StaffChannel staffChannel;
    private DiscordConnection discordConnection;

    // Constructor using method chaining for concise initialization
    public static DiscordChannel create(VeloCore plugin) {
        return new DiscordChannel(plugin);
    }

    private DiscordChannel(VeloCore plugin) {
        this.plugin = plugin;
        this.staffChannel = null; // Initialize to your default value or set via a method
        this.discordConnection = null; // Initialize to your default value or set via a method
    }

    // Optional setters for StaffChannel and DiscordConnection
    public DiscordChannel withStaffChannel(StaffChannel staffChannel) {
        this.staffChannel = staffChannel;
        return this;
    }

    public DiscordChannel withDiscordConnection(DiscordConnection discordConnection) {
        this.discordConnection = discordConnection;
        return this;
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

