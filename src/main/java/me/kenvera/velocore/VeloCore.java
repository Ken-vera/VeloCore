package me.kenvera.velocore;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.kenvera.velocore.commands.Aliases;
import me.kenvera.velocore.commands.List;
import me.kenvera.velocore.commands.Send;
import net.kyori.adventure.text.Component;

@Plugin(
        id = "velocore",
        name = "VeloCore",
        version = "1.0-SNAPSHOT"
)
public final class VeloCore{
    private final ProxyServer proxy;
    @Inject
    public VeloCore(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        proxy.getConsoleCommandSource().sendMessage(Component.text());
        proxy.getConsoleCommandSource().sendMessage(Component.text("§eVeloCore §aby §bKenvera §ais enabled!"));
        proxy.getConsoleCommandSource().sendMessage(Component.text());

        CommandManager commandManager = proxy.getCommandManager();

        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            commandManager.register(serverName, new Aliases(proxy, serverName));
            commandManager.register("send", new Send(proxy));
            commandManager.register("list", new List(proxy), "stafflist");
        }
    }
}
