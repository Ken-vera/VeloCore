package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.managers.RedisConnection;
import net.kyori.adventure.text.Component;

import java.util.Arrays;

public class GlobalChatCommand implements SimpleCommand {
    private final ProxyServer proxy;
    private final RedisConnection connection;

    public GlobalChatCommand(ProxyServer proxy, RedisConnection connection){
        this.proxy = proxy;
        this.connection = connection;
    }
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 1) {
            source.sendMessage(Component.text("§cUsage: /globalchat <message>"));
            return;
        }

        if (!(source instanceof Player player)){
            return;
        }

        String message = String.join(" ", args);

        connection.getJedis();
        connection.publish("globalmessage:" + player.getCurrentServer().get().getServerInfo().getName().toUpperCase() + ":" + player.getGameProfile().getName() + ":" + message);
    }
}
