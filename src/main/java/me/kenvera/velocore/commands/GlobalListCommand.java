package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GlobalListCommand implements SimpleCommand {
    private final ProxyServer proxy;

    public GlobalListCommand(ProxyServer proxy) {
        this.proxy = proxy;
    }
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("velocity.globallist")) {
            source.sendMessage(Component.text("§cYou don't have permission to run this command!"));
            return;
        }

        Map<String, java.util.List<String>> playersByServer = new HashMap<>();

        for (Player player : proxy.getAllPlayers()) {
            String serverName = player.getCurrentServer().map(server -> server.getServerInfo().getName()).orElse("N/A");
            playersByServer.computeIfAbsent(serverName, k -> new ArrayList<>()).add(player.getUsername());
        }

        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§aPlayers online by server:"));
        for (Map.Entry<String, java.util.List<String>> entry : playersByServer.entrySet()) {
            String serverName = entry.getKey();
            java.util.List<String> players = entry.getValue();

            String playerList = String.join(", ", players);
            source.sendMessage(Component.text("[" + serverName + "] : " + playerList));
        }
        source.sendMessage(Component.text(""));
    }
}
