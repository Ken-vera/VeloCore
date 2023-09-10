package me.kenvera.velocore;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.kenvera.velocore.commands.*;
import me.kenvera.velocore.discordshake.DiscordConnection;
import me.kenvera.velocore.listeners.DiscordChannel;
import me.kenvera.velocore.listeners.OnlineSession;
import me.kenvera.velocore.listeners.StaffChannel;
import me.kenvera.velocore.listeners.StaffSession;
import me.kenvera.velocore.managers.DataManager;
import me.kenvera.velocore.managers.RedisConnection;
import me.kenvera.velocore.managers.SqlConnection;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Plugin(
        id = "velocore",
        name = "VeloCore",
        version = "1.0",
        authors = "Kenvera, Mornov"
)
public final class VeloCore {
    private final ProxyServer proxy;
    private final Map<UUID, Long> playerOnlineSession = new HashMap<>();
    private final Map<UUID, Boolean> playerStaffChat = new HashMap<>();
    private final Map<UUID, Boolean> playerStaffChatMute = new HashMap<>();
    private SqlConnection dataBase;
    private StaffChannel staffChannel;
    private DiscordConnection discordConnection;
    private DiscordChannel discordChannel;

    // FIXED
    public static VeloCore INSTANCE;
    private final Logger logger;
    private DataManager configManager;
    private RedisConnection redis;
    private CommandManager commandManager;

    @Inject
    public VeloCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataBase = new SqlConnection(proxy, playerStaffChat, playerStaffChatMute);
        this.configManager = new DataManager(this);
        this.redis = new RedisConnection(proxy,
                configManager.getString("redis.host", "defaultValue"),
                configManager.getInt("redis.port", 0),
                configManager.getString("redis.password", "root"));
        this.discordConnection = new DiscordConnection(proxy);
        this.staffChannel = new StaffChannel(this);
        this.discordChannel = DiscordChannel.create(this).withStaffChannel(staffChannel).withDiscordConnection(discordConnection);
        this.commandManager = proxy.getCommandManager();
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("§f[§eVeloCore§f] §aPlugin is Loading...");
        logger.info("§f[§eVeloCore§f] §aLoading Config...");

        logger.info("");
        logger.info(getPrefix() + "§aPlugin Loaded!");
        logger.info("");

        // Command Register
        registerCommand(commandManager, "send", Send.createBrigadierCommand(proxy), null);
        registerCommand(commandManager, "staffchat", StaffChat.createBrigadierCommand(proxy, playerStaffChat, playerStaffChatMute), null, "sc");
        registerCommand(commandManager, "stafflist", StaffList.createBrigadierCommand(proxy, playerOnlineSession), null, "sl");
        registerCommand(commandManager, "database", DataBase.createBrigadierCommand(proxy, dataBase), null, "db");
        registerCommand(commandManager, "globallist", null, new GlobalList(proxy), "glist");
        registerCommand(commandManager, "report", null, new ReportListener(proxy));
        registerCommand(commandManager, "checkalts", null, new AltsChecker(proxy));
        registerCommand(commandManager, "globalchat", null, new GlobalChat(proxy, redis), "gc");
        registerCommand(commandManager, "find", null, new Find(proxy, playerOnlineSession));

        discordConnection.disconnect();
        discordConnection.connect("MTE0NTMyMTMzOTUyMDAzNjkzNA.GTGhdW.yvd6PWQ1W99QZ7fevuTYn8Px-ADW8FvvrKQBug", discordChannel);

        EventManager eventManager = proxy.getEventManager();

        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            commandManager.register(serverName, new Aliases(proxy, serverName));
        }

        eventManager.register(this, new OnlineSession(this));
        eventManager.register(this, new StaffSession(this));
        eventManager.register(this, staffChannel);
        eventManager.register(this, discordChannel);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        discordConnection.disconnect();

        dataBase.closeDataSource();

        logger.info("");
        logger.info(getPrefix() + "§cPlugin Unloaded!");
        logger.info("");
    }

    private void registerCommand(CommandManager commandManager, String label, BrigadierCommand brigadierCommand, SimpleCommand simpleCommand, String... aliases) {
        CommandMeta commandMeta = commandManager.metaBuilder(label).aliases(aliases).plugin(this).build();
        if (brigadierCommand != null) {
            commandManager.register(commandMeta, brigadierCommand);
        } else if (simpleCommand != null) {
            commandManager.register(commandMeta, simpleCommand);
        }
    }

    public Logger getLogger() {
        return logger;
    }
    public String getPrefix() {
        return "§f[§eVeloCore§f] ";
    }

    public Map<UUID, Boolean> getPlayerStaffChat() {
        return playerStaffChat;
    }

    public Map<UUID, Boolean> getPlayerStaffChatMute() {
        return playerStaffChatMute;
    }

    public Map<UUID, Boolean> getPlayerSession() {
        return playerOnlineSession;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public DiscordConnection getDiscordConnection() {
        return discordConnection;
    }

    public DiscordChannel getDiscordChannel() {
        return discordChannel;
    }

    public SqlConnection getSqlConnection() {
        return dataBase;
    }

    public RedisConnection getRedisConnection() {
        return redis;
    }
}
