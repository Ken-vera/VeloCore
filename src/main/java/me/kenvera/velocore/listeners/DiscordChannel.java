package me.kenvera.velocore.listeners;

import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.discordshake.DiscordConnection;
import me.kenvera.velocore.managers.DataManager;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordChannel extends ListenerAdapter {
    private final StaffChannel staffChannel;
    private final DiscordConnection discord;
    private final DataManager config;
    public DiscordChannel(VeloCore plugin) {
        this.staffChannel = plugin.getStaffChannel();
        this.discord = plugin.getDiscordConnection();
        this.config = plugin.getConfigManager();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getAuthor().isBot()) {
            Long id = config.getLong("discord.staff-channel-id");
            TextChannel targetChannel = discord.getTextChannel(id);

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
