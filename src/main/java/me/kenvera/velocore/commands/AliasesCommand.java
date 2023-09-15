package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AliasesCommand implements SimpleCommand {
    private final ProxyServer proxy;
    private final String targetServerName;

    public AliasesCommand(ProxyServer proxy, String targetServerName) {
        this.proxy = proxy;
        this.targetServerName = targetServerName;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        List<String> listServers = new ArrayList<>(proxy
                .getAllServers()
                .stream()
                .map(server -> server.getServerInfo().getName())
                .toList());

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("This command can only be used by players."));
            return;
        }

        if (listServers.contains(invocation.alias())) {
            if (!player.hasPermission("velocity.connect." + invocation.alias())) {
                player.sendMessage(Component.text("§cYou are unable to connect to " + invocation.alias()));
                return;
            }

            Optional<RegisteredServer> targetServer = proxy.getServer(invocation.alias());
            if (targetServer.isEmpty()) {
                player.sendMessage(Component.text(invocation.alias() + " server is not available."));
                return;
            }
            if (player.getCurrentServer().get().getServerInfo().getName().equals(invocation.alias())) {
                player.sendMessage(Component.text("§cYou already connected to " + invocation.alias()));
            } else {
                if (player.hasPermission("velocity.connect." + targetServer.get().getServerInfo().getName())) {
                    player.createConnectionRequest(targetServer.get()).fireAndForget();
                    player.sendMessage(Component.text("Sending you to " + targetServer.get().getServerInfo().getName()));
                }
            }
        }
    }
}
