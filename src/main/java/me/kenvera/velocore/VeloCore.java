package me.kenvera.velocore;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import lombok.Getter;
import me.kenvera.velocore.commands.*;
import me.kenvera.velocore.commands.chat.DonatorChatCommand;
import me.kenvera.velocore.commands.chat.GlobalChatCommand;
import me.kenvera.velocore.commands.chat.StaffChatCommand;
import me.kenvera.velocore.database.DataManager;
import me.kenvera.velocore.database.RedisManager;
import me.kenvera.velocore.database.SqlManager;
import me.kenvera.velocore.discordshake.DiscordConnection;
import me.kenvera.velocore.donation.DonationAnnouncement;
import me.kenvera.velocore.listeners.*;
import me.kenvera.velocore.managers.Ban;
import me.kenvera.velocore.managers.BanManager;
import me.kenvera.velocore.managers.PlayerData;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Plugin(
        id = "velocore",
        name = "VeloCore",
        version = "1.0",
        authors = "Kenvera, Mornov",
        dependencies = {
                @Dependency(id="luckperms")
        }
)
public final class VeloCore {
    @Getter
    private final ProxyServer proxy;
    private final Map<UUID, Long> playerOnlineSession = new HashMap<>();
    @Getter
    private final Map<UUID, Boolean> playerStaffChat = new HashMap<>();
    @Getter
    private final Map<UUID, Boolean> playerStaffChatMute = new HashMap<>();
    private SqlManager dataBase;
    @Getter
    private StaffChannel staffChannel;
    @Getter
    private DiscordConnection discordConnection;
    @Getter
    private DiscordChannel discordChannel;
    @Getter
    private BanManager banManager;

    // FIXED
    @Getter
    private final Logger logger;
    @Getter
    private DataManager configManager;
    @Getter
    private RedisManager redis;
    private final CommandManager commandManager;
    @Getter
    private PlayerData playerData;
    @Getter
    private Ban ban;
    private final Cache<String, Cache<UUID, Long>> cooldowns = Caffeine.newBuilder().build();

    @Inject
    public VeloCore(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.commandManager = proxy.getCommandManager();
    }

    @Subscribe (order = PostOrder.LAST)
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.warn("§f[§eVeloCore§f] §aPlugin is Loading...");
        logger.warn("§f[§eVeloCore§f] §aRegistering Components...");

        registerComponents();

        logger.info("");
        logger.info(getPrefix() + "§aPlugin Loaded!");
        logger.info("");

        // Command Register
        registerCommand(commandManager, "send", SendCommand.createBrigadierCommand(this), null);
        registerCommand(commandManager, "staffchat", StaffChatCommand.createBrigadierCommand(this), null, "sc");
        registerCommand(commandManager, "stafflist", StaffListCommand.createBrigadierCommand(this), null, "sl");
        registerCommand(commandManager, "database", DataBaseCommand.createBrigadierCommand(this), null, "db");
        registerCommand(commandManager, "globallist", null, new GlobalListCommand(proxy), "glist");
        registerCommand(commandManager, "report", ReportCommand.createBrigadierCommand(this), null);
        registerCommand(commandManager, "checkalts", null, new AltsCheckerCommand(proxy));
        registerCommand(commandManager, "globalchat", GlobalChatCommand.createBrigadierCommand(this), null, "gc");
        registerCommand(commandManager, "donatorchat", DonatorChatCommand.createBrigadierCommand(this), null,  "dc");
        registerCommand(commandManager, "find", FindCommand.createBrigadierCommand(this), null);
        registerCommand(commandManager, "donationannouncement", null, new DonationAnnouncement(proxy));
        registerCommand(commandManager, "ban", BanCommand.createBrigadierCommand(this), null, "vban");
        registerCommand(commandManager, "tempban", TempBanCommand.createBrigadierCommand(this), null, "vtempban");
        registerCommand(commandManager, "unban", UnBanCommand.createBrigadierCommand(this), null, "vunban", "pardon");
        registerCommand(commandManager, "debug", Debug.createBrigadierCommand(this), null);
        registerCommand(commandManager, "velocore", new ReloadCommand(this).createBrigadierCommand(), null);
        registerCommand(commandManager, "group", GroupCommand.createBrigadierCommand(this), null);
        registerCommand(commandManager, "mute", MuteCommand.createBrigadierCommand(this), null);
        registerCommand(commandManager, "unmute", UnMuteCommand.createBrigadierCommand(this), null);

        discordConnection.disconnect();
        discordConnection.connect(configManager.getString("discord.token", null), discordChannel);

        EventManager eventManager = proxy.getEventManager();

        for (RegisteredServer server : proxy.getAllServers()) {
            String serverName = server.getServerInfo().getName();
            commandManager.register(serverName, new AliasesCommand(proxy, serverName));
        }

        eventManager.register(this, new PlayerSession(this));
        eventManager.register(this, new StaffSession(this));
        eventManager.register(this, staffChannel);
        eventManager.register(this, discordChannel);
        eventManager.register(this, new ChatListener(this));

        dataBase.loadTables();
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        discordConnection.disconnect();
        dataBase.closeDataSource();
        redis.close();

        logger.info("");
        logger.info(getPrefix() + "§cPlugin Unloaded!");
        logger.info("");
    }

    private void registerComponents() {
        PluginContainer luckpermsPlugin = proxy.getPluginManager().getPlugin("luckperms").orElse(null);
        if (luckpermsPlugin == null || luckpermsPlugin.getInstance().isEmpty()) {
            getLogger().error("LuckPerms is not loaded or enabled. VeloCore depends on LuckPerms.");
        } else {
            configManager = new DataManager(this);
            dataBase = new SqlManager(this);
            if (redis != null) {
                System.out.println(redis);
                redis.close();
            }
            redis = new RedisManager(this,
                    configManager.getString("redis.host", "defaultValue"),
                    configManager.getInt("redis.port", 0),
                    configManager.getString("redis.password", "root"));
            discordConnection = new DiscordConnection(this);
            staffChannel = new StaffChannel(this);
            discordChannel = new DiscordChannel(this);
            banManager = new BanManager(this);
            playerData = new PlayerData(this);
        }
    }

    private void registerCommand(CommandManager commandManager, String label, BrigadierCommand brigadierCommand, SimpleCommand simpleCommand, String... aliases) {
        CommandMeta commandMeta = commandManager.metaBuilder(label).aliases(aliases).plugin(this).build();
        if (brigadierCommand != null) {
            commandManager.register(commandMeta, brigadierCommand);
        } else if (simpleCommand != null) {
            commandManager.register(commandMeta, simpleCommand);
        }
    }

    public void setCooldown(String cooldownType, int cooldownTime, UUID uuid) {
        Cache<UUID, Long> newCooldownMap = Caffeine.newBuilder()
                .expireAfterWrite(cooldownTime, TimeUnit.SECONDS)
                .build();
        newCooldownMap.put(uuid, System.currentTimeMillis());
        cooldowns.put(cooldownType, newCooldownMap);
    }

    public Long getCooldown(String cooldownType, UUID uuid) {
        Cache<UUID, Long> cooldownMap = cooldowns.getIfPresent(cooldownType);

        if (cooldownMap != null) {
            return cooldownMap.getIfPresent(uuid);
        }
        return null;
    }

    public void resetCooldown(String cooldownType, UUID uuid) {
        Cache<UUID, Long> cooldownMap = cooldowns.getIfPresent(cooldownType);

        if (cooldownMap != null) {
            cooldownMap.invalidate(uuid);
        }
    }

    public String getPrefix() {
        return "§f[§eVeloCore§f] ";
    }

    public Map<UUID, Long> getPlayerSession() {
        return playerOnlineSession;
    }


    public SqlManager getSqlConnection() {
        return dataBase;
    }

    public RedisManager getRedisConnection() {
        return redis;
    }

    public LuckPerms getLuckPerms() {
        return LuckPermsProvider.get();
    }

    public void broadcast(String message) {
        getProxy().getAllPlayers().stream().forEach(player -> player.sendMessage(Component.text(message)));
    }

    public void broadcastStaff(String message) {
        getProxy().getAllPlayers().stream().filter(player -> player.hasPermission("velocity.staff")).forEach(player -> player.sendMessage(Component.text(message)));
    }
}
