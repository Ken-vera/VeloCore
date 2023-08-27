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
import me.kenvera.velocore.discordshake.DiscordConnection;
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
    private final Map<UUID, Boolean> playerStaffChat = new HashMap<>();
    private DiscordConnection discordConnection;
    @Inject
    public VeloCore(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        proxy.getConsoleCommandSource().sendMessage(Component.text());
        proxy.getConsoleCommandSource().sendMessage(Component.text("§eVeloCore §aby §bKenvera §ais enabled!"));
        proxy.getConsoleCommandSource().sendMessage(Component.text());

        // DISCORD INITIATION
        discordConnection = new DiscordConnection(this);
        discordConnection.sendStaffMessage("Ojan", "TEST SAYANG");

        EventManager eventManager = proxy.getEventManager();
        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMetaSend = commandManager.metaBuilder("send").plugin(this).build();
        CommandMeta commandMetaStaff = commandManager.metaBuilder("staffchat").aliases("sc").plugin(this).build();

        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            commandManager.register(serverName, new Aliases(proxy, serverName));
        }

        BrigadierCommand commandSend = Send.createBrigadierCommand(proxy);
        BrigadierCommand commandStaff = StaffChat.createBrigadierCommand(proxy, playerStaffChat);
        commandManager.register(commandMetaSend, commandSend);
        commandManager.register(commandMetaStaff, commandStaff);

        commandManager.register("stafflist", new List(proxy, playerOnlineSession), "sl");
        commandManager.register("globallist", new GlobalList(proxy), "glist");
        commandManager.register("report", new ReportListener(proxy));
        commandManager.register("checkalts", new AltsChecker(proxy));
        commandManager.register("find", new Find(proxy, playerOnlineSession));

        eventManager.register(this, new OnlineSession(proxy, playerOnlineSession));
        eventManager.register(this, new me.kenvera.velocore.listeners.StaffChat(proxy, playerStaffChat));
    }
}
