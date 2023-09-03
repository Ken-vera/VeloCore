package me.kenvera.velocore.discordshake;

import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.VeloCore;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.kyori.adventure.text.Component;

import java.lang.reflect.Proxy;

public class DiscordConnection {
    public JDA jda;
    private final ProxyServer proxy;
    public DiscordConnection(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public void connect(String token, ListenerAdapter listener) {
        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §eEstablishing Discord Connection!"));
        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_TYPING, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.DIRECT_MESSAGE_TYPING, GatewayIntent.DIRECT_MESSAGE_REACTIONS, GatewayIntent.GUILD_MEMBERS) // Add GUILD_MESSAGES and other necessary intents
                    .addEventListeners(listener)
                    .build();

            jda.awaitReady(); // Wait until JDA is fully initialized
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §9Connected to Discord!"));
        } catch (Exception e) {
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cDiscord Connection Failed!"));
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (jda != null) {
            jda.shutdownNow();
            proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cDisconnected from Discord!"));
        }
    }
}
