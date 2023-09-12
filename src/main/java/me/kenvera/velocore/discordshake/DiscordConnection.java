package me.kenvera.velocore.discordshake;

import me.kenvera.velocore.VeloCore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class DiscordConnection {
    public JDA jda;
    private final VeloCore plugin;
    public DiscordConnection(VeloCore plugin) {
        this.plugin = plugin;
    }

    public void connect(String token, ListenerAdapter listener) {
        plugin.getLogger().warn(plugin.getPrefix() + "§eEstablishing Discord Connection!");
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(
                            GatewayIntent.MESSAGE_CONTENT,
                            GatewayIntent.GUILD_MESSAGES,
                            GatewayIntent.GUILD_MESSAGE_TYPING,
                            GatewayIntent.GUILD_MESSAGE_REACTIONS,
                            GatewayIntent.DIRECT_MESSAGES,
                            GatewayIntent.DIRECT_MESSAGE_TYPING,
                            GatewayIntent.DIRECT_MESSAGE_REACTIONS,
                            GatewayIntent.GUILD_MEMBERS) // Add GUILD_MESSAGES and other necessary intents
                    .addEventListeners(listener)
                    .build();

            jda.awaitReady(); // Wait until JDA is fully initialized
            plugin.getLogger().info(plugin.getPrefix() + "§9Connected to Discord!");
        } catch (Exception e) {
            plugin.getLogger().error(plugin.getPrefix() + "§cDiscord Connection Failed!" + e);
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (jda != null) {
            jda.shutdownNow();
            plugin.getLogger().info(plugin.getPrefix() + "§cDisconnected from Discord!");
        }
    }

    public TextChannel getTextChannel(Long id) {
        return jda.getTextChannelById(id);
    }
}
