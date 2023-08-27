package me.kenvera.velocore.discordshake;

import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.VeloCore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

public class DiscordConnection {
    private JDA jda;
    private VeloCore plugin;

    public DiscordConnection(VeloCore plugin) {
        this.plugin = plugin;
    }

    public void connect(String token) {
        try {
            jda = JDABuilder.createDefault(token)
                    .addEventListeners(/* Add your event listeners here */)
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
            targetChannel.sendMessage("From " + sender + ": " + message).queue();
        } else {
            System.out.println("Target channel not found.");
        }
    }
}
