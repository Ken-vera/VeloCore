package me.kenvera.velocore;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.kenvera.velocore.commands.*;
import me.kenvera.velocore.listeners.OnlineSession;
import me.kenvera.velocore.listeners.StaffChat;
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
    private final Map<UUID, Boolean> playerStaffChat = new HashMap<>();
    @Inject
    public VeloCore(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        proxy.getConsoleCommandSource().sendMessage(Component.text());
        proxy.getConsoleCommandSource().sendMessage(Component.text("§eVeloCore §aby §bKenvera §ais enabled!"));
        proxy.getConsoleCommandSource().sendMessage(Component.text());

        EventManager eventManager = proxy.getEventManager();
        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMeta = commandManager.metaBuilder("send").plugin(this).build();

        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            commandManager.register(serverName, new Aliases(proxy, serverName));
        }

        BrigadierCommand commandToRegister = Send.createBrigadierCommand(proxy);
        commandManager.register(commandMeta, commandToRegister);

        commandManager.register("stafflist", new List(proxy, playerOnlineSession), "sl");
        commandManager.register("globallist", new GlobalList(proxy), "glist");
        commandManager.register("report", new ReportListener(proxy));
        commandManager.register("checkalts", new AltsChecker(proxy));
        commandManager.register("find", new Find(proxy, playerOnlineSession));
        commandManager.register("staffchat", new me.kenvera.velocore.commands.StaffChat(proxy, playerStaffChat), "sc");

        eventManager.register(this, new OnlineSession(proxy, playerOnlineSession));
        eventManager.register(this, new StaffChat(proxy, playerStaffChat));
    }
}
