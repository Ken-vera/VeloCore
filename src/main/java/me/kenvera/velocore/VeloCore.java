package me.kenvera.velocore;

import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
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
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
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
    private boolean pluginEnabled = false;

    // FIXED
    public static VeloCore INSTANCE;
    private final File dataDirectory;
    private final Logger logger;
    private DataManager configManager;
    private RedisConnection redis;
//    private final ConcurrentHashMap<UUID, Long> playerOnlineSession1 = new ConcurrentHashMap<>();

    @Inject
    public VeloCore(ProxyServer proxy, Logger logger, @DataDirectory final Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory.toFile();
        this.configManager = new DataManager(proxy);
    }

    public static VeloCore getINSTANCE() {
        return INSTANCE;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("§f[§eVeloCore§f] §aPlugin is Loading...");
        logger.info("§f[§eVeloCore§f] §aLoading Config...");
        configManager.load();

        pluginEnabled = true;
        proxy.getConsoleCommandSource().sendMessage(Component.text());
        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §aPlugin Loaded!"));
        proxy.getConsoleCommandSource().sendMessage(Component.text());

        // SQLITE INITIATION
        dataBase = new SqlConnection(proxy, playerStaffChat, playerStaffChatMute);
        dataBase.loadTables();
        dataBase.loadStaffData();

        String redisHost = configManager.getString("redis.host", null);
        proxy.getConsoleCommandSource().sendMessage(Component.text(configManager.getString("redis.host", null)));
        int redisPort = configManager.getInt("redis.port", 0000);
        proxy.getConsoleCommandSource().sendMessage(Component.text(configManager.getInt("redis.port", 0000)));
        String redisPassword = configManager.getString("redis.password", null);
        proxy.getConsoleCommandSource().sendMessage(Component.text(configManager.getString("redis.password", null)));
        redis = new RedisConnection(proxy, redisHost, redisPort, redisPassword);

        // DISCORD INITIATION
        discordConnection = new DiscordConnection(proxy);
        staffChannel = new StaffChannel(proxy, playerStaffChat, playerStaffChatMute, discordConnection);
        discordChannel = new DiscordChannel(this, staffChannel, discordConnection);
        discordConnection.disconnect();
        discordConnection.connect("MTE0NTMyMTMzOTUyMDAzNjkzNA.GTGhdW.yvd6PWQ1W99QZ7fevuTYn8Px-ADW8FvvrKQBug", discordChannel);

        EventManager eventManager = proxy.getEventManager();
        CommandManager commandManager = proxy.getCommandManager();
        CommandMeta commandMetaSend = commandManager.metaBuilder("send").plugin(this).build();
        CommandMeta commandMetaStaff = commandManager.metaBuilder("staffchat").aliases("sc").plugin(this).build();
        CommandMeta commandMetaStaffList = commandManager.metaBuilder("stafflist").aliases("sl").plugin(this).build();
        CommandMeta commandMetaDataBase = commandManager.metaBuilder("database").aliases("db").plugin(this).build();

        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            commandManager.register(serverName, new Aliases(proxy, serverName));
        }

        BrigadierCommand commandSend = Send.createBrigadierCommand(proxy);
        BrigadierCommand commandStaff = StaffChat.createBrigadierCommand(proxy, playerStaffChat, playerStaffChatMute);
        BrigadierCommand commandStaffList = StaffList.createBrigadierCommand(proxy, playerOnlineSession);
        BrigadierCommand commandDataBase = DataBase.createBrigadierCommand(proxy, dataBase);
        commandManager.register(commandMetaSend, commandSend);
        commandManager.register(commandMetaStaff, commandStaff);
        commandManager.register(commandMetaStaffList, commandStaffList);
        commandManager.register(commandMetaDataBase, commandDataBase);

        commandManager.register("globallist", new GlobalList(proxy), "glist");
        commandManager.register("report", new ReportListener(proxy));
        commandManager.register("checkalts", new AltsChecker(proxy));
        commandManager.register("globalchat", new GlobalChat(proxy, redis), "gc");
        commandManager.register("find", new Find(proxy, playerOnlineSession));

        eventManager.register(this, new OnlineSession(proxy, playerOnlineSession));
        eventManager.register(this, new StaffSession(proxy, dataBase));
        eventManager.register(this, staffChannel);
        eventManager.register(this, discordChannel);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        discordConnection.disconnect();

        if (pluginEnabled) {
            dataBase.saveStaffData();
        }
        dataBase.closeDataSource();

        proxy.getConsoleCommandSource().sendMessage(Component.text());
        proxy.getConsoleCommandSource().sendMessage(Component.text("§f[§eVeloCore§f] §cPlugin Unloaded!"));
        proxy.getConsoleCommandSource().sendMessage(Component.text());

    }

    public Logger getLogger() {
        return logger;
    }
}
