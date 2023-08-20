package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Send implements SimpleCommand {
    private final ProxyServer proxy;

    public Send(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("velocity.send")) {
            source.sendMessage(Component.text("§cYou don't have permission to run this command!"));
            return;
        }

        List<String> listServers = new ArrayList<>(proxy
                .getAllServers()
                .stream()
                .map(server -> server.getServerInfo().getName())
                .toList());

        if (invocation.arguments().length == 0) {
            source.sendMessage(Component.text("§cWrong command usage!"));
            return;
        }

        if (invocation.arguments().length == 1) {
            source.sendMessage(Component.text("§cWrong command usage!"));
            return;
        }

        if (Objects.equals(invocation.arguments()[0], "all")) {
            if (!source.hasPermission("velocity.send.all")) {
                source.sendMessage(Component.text("§cYou don't have permission to run this command!"));
                return;
            }
            if (listServers.contains(invocation.arguments()[1])) {
                Optional<RegisteredServer> targetServer = proxy.getServer(invocation.arguments()[1]);
                if (targetServer.isEmpty()) {
                    source.sendMessage(Component.text("§c" + invocation.arguments()[1] + " server is not available!"));
                    return;
                }
                if (isOffline(targetServer.get().getServerInfo().getName())) {
                    source.sendMessage(Component.text("§c" + invocation.arguments()[1] + " can't be reach!"));
                    return;
                }
                proxy
                        .getAllPlayers()
                        .stream()
                        .filter(player -> !Objects.equals(player.getCurrentServer().get().getServerInfo().getName(), invocation.arguments()[1]))
                        .forEach(player -> player.createConnectionRequest(targetServer.get()).fireAndForget());
                source.sendMessage(Component.text("§a" + "Succesfully sent " +
                        proxy.getAllPlayers()
                                .stream()
                                .filter(player -> !Objects.equals(player.getCurrentServer().get().getServerInfo().getName(), invocation.arguments()[1])).toList().size() +
                        " Players to " + targetServer.get().getServerInfo().getName()));
            } else {
                source.sendMessage(Component.text("§c" + invocation.arguments()[1] + " is not a valid server!"));
            }
        }

        if (Objects.equals(invocation.arguments()[0], "current")) {

        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments[0].equalsIgnoreCase("")) {
            String partialName = arguments[0].toLowerCase();

            List<String> onlinePlayer = proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(partialName))
                    .toList();

            return List.of(onlinePlayer.toString());
        }
        return List.of();
    }

    private boolean isOffline(String proxiedServer) {
        Optional<RegisteredServer> targetServer = proxy.getServer(proxiedServer);
        if (targetServer.isPresent()) {
            RegisteredServer server = targetServer.get();
            try {
                server.ping();
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return true;
    }
}
