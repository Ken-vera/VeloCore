package me.kenvera.velocore.discordshake;

import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.VeloCore;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.*;

public class DiscordConnection {
    private JDA jda;
    private VeloCore plugin;

    public DiscordConnection(VeloCore plugin) {
        this.plugin = plugin;
        disconnect();
        connect("MTE0NTMyMTMzOTUyMDAzNjkzNA.GTGhdW.yvd6PWQ1W99QZ7fevuTYn8Px-ADW8FvvrKQBug");
    }
    private class MessageListener extends ListenerAdapter {
        @Override
        public void onMessageReceived(MessageReceivedEvent event) {
            if (!event.getAuthor().isBot()) { // Check if the sender is not a bot
                TextChannel targetChannel = jda.getTextChannelById("1145334804913586216"); // Replace with your target channel ID

                if (targetChannel != null && event.getChannel().equals(targetChannel)) {
                    String author = event.getAuthor().getName();
                    String messageContent = event.getMessage().getContentDisplay();

                    // Check if the author is not the bot
                    if (!author.contains("PyroGuardian")) {
                        System.out.println("Discord message from " + author + ": " + messageContent);
                    }
                }
            }
        }
    }


    public void connect(String token) {
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.GUILD_MEMBERS) // Add GUILD_MESSAGES and other necessary intents
                    .addEventListeners(new MessageListener())
                    .build();

            jda.awaitReady(); // Wait until JDA is fully initialized
            System.out.println("Connected to Discord!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (jda != null) {
            jda.shutdown();
            System.out.println("Disconnected from Discord.");
        }
    }
    public void sendStaffMessage(String sender, String message) {
        String targetChannelId = "1145334804913586216"; // Replace with your target channel ID

        // Get the TextChannel instance using the provided channel ID
        TextChannel targetChannel = jda.getTextChannelById(targetChannelId);

        if (targetChannel != null) {
            EmbedBuilder embed = new EmbedBuilder();
            embed.setColor(Color.GREEN)
                    .setTitle("Staff Message")
                    .setDescription(message)
                    .addField("Sent by", sender, false)
                    .setTimestamp(java.time.Instant.now());

            targetChannel.sendMessageEmbeds(embed.build()).queue();
        } else {
            System.out.println("Target channel not found.");
        }
    }
}
