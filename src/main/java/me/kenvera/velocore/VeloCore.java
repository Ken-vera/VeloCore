package me.kenvera.velocore;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.kenvera.velocore.commands.*;
import me.kenvera.velocore.listeners.OnlineSession;
import net.kyori.adventure.text.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Plugin(
        id = "velocore",
        name = "VeloCore",
        version = "1.0",
        authors = "Kenvera, Mornov"
)
public final class VeloCore{
    private final ProxyServer proxy;
    private final Map<UUID, Long> playerOnlineSession = new HashMap<>();
    @Inject
    public VeloCore(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        proxy.getConsoleCommandSource().sendMessage(Component.text());
        proxy.getConsoleCommandSource().sendMessage(Component.text("§eVeloCore §aby §bKenvera §ais enabled!"));
        proxy.getConsoleCommandSource().sendMessage(Component.text());

        initializeEventListeners();

        CommandManager commandManager = proxy.getCommandManager();

        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            commandManager.register(serverName, new Aliases(proxy, serverName));
        }

        commandManager.register("send", new Send(proxy));
        commandManager.register("stafflist", new List(proxy, playerOnlineSession), "sl");
        commandManager.register("globallist", new GlobalList(proxy), "glist");
        commandManager.register("report", new ReportListener(proxy));
        commandManager.register("checkalts", new AltsChecker(proxy));
        commandManager.register("find", new Find(proxy, playerOnlineSession));
    }

    public void initializeEventListeners() {
        EventManager eventManager = proxy.getEventManager();

        OnlineSession onlineSession = new OnlineSession(playerOnlineSession);

        eventManager.register(this, onlineSession);
    }
}
