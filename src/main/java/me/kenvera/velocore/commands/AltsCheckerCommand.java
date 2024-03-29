package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

public class AltsCheckerCommand implements SimpleCommand {
    private final ProxyServer proxy;

    public AltsCheckerCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("This command can only be run by players."));
            return;
        }

        if (!player.hasPermission("velocity.scan")) {
            player.sendMessage(Component.text("You don't have permission to run this command."));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /checkalts <username>"));
            return;
        }

        String targetUsername = args[0].toLowerCase(); // Make username lowercase for case-insensitivity
        InetSocketAddress playerAddress = player.getRemoteAddress();

        List<Player> playersWithSameIp = proxy.getAllPlayers().stream()
                .filter(p -> !p.equals(player))
                .filter(p -> p.getRemoteAddress().getAddress().equals(playerAddress.getAddress()))
                .filter(p -> !p.getUsername().equalsIgnoreCase(targetUsername))
                .toList();

//        List<Player> playersWithSameIp = proxy.getAllPlayers().stream()
//                .filter(p -> p.getRemoteAddress().getAddress().getHostAddress()
//                        .equals(player.getRemoteAddress().getAddress().getHostAddress()))
//                .filter(p -> p.getUsername().equalsIgnoreCase(targetUsername))
//                .toList();

        if (playersWithSameIp.isEmpty()) {
            player.sendMessage(Component.text("No matching accounts found."));
            return;
        }

        player.sendMessage(Component.text("Accounts with the same IP as '" + targetUsername + "':"));
        for (Player p : playersWithSameIp) {
            player.sendMessage(Component.text("- " + p.getUsername())
                    .color(NamedTextColor.GREEN));
        }

        ServerConnection serverConnection = playersWithSameIp.get(0).getCurrentServer().orElse(null);
        if (serverConnection != null) {
            player.sendMessage(Component.text("Last joined server: " + serverConnection.getServerInfo().getName()));
        } else {
            player.sendMessage(Component.text(targetUsername + " has not joined the server."));
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1) {
            return proxy.getAllPlayers().stream()
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        if (args.length >= 1) {
            return Collections.emptyList();
        }
        return proxy.getAllPlayers().stream()
                .map(Player::getUsername)
                .toList();
    }
}
