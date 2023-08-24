package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Find implements SimpleCommand {
    private final ProxyServer proxy;
    private final Map<UUID, Long> playerOnlineSession;

    public Find (ProxyServer proxy, Map<UUID, Long> playerOnlineSession){
        this.proxy = proxy;
        this.playerOnlineSession = playerOnlineSession;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("velocity.find")) {
            source.sendMessage(Component.text("§cYou don't have permission to run this command."));
            return;
        }

        if (args.length < 1) {
            source.sendMessage(Component.text("§cUsage: /find <username>"));
            return;
        }

        String targetPlayerName = args[0];
        Player targetPlayer = proxy.getPlayer(targetPlayerName).orElse(null);

        if (targetPlayer == null) {
            source.sendMessage(Component.text("§cPlayer " + targetPlayerName + " seems to be offline"));
        } else {
            String targetPlayerServer = targetPlayer.getCurrentServer().get().getServerInfo().getName();
            UUID uuid = targetPlayer.getUniqueId();
            String targetPlayerAddress = "/" + targetPlayer.getRemoteAddress().getHostName();
            boolean targetPlayerMode = targetPlayer.isOnlineMode();
            ProtocolVersion targetPlayerVersion = targetPlayer.getProtocolVersion();
            String targetPlayerClient = targetPlayer.getClientBrand();
            long targetPlayerPing = targetPlayer.getPing();
            long onlineTime = playerOnlineSession.getOrDefault(uuid, 0L);
            long currentTime = System.currentTimeMillis();

            long onlineSession = currentTime - onlineTime;
            long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(onlineSession);
            long hours = totalMinutes / 60;
            long minutes = totalMinutes % 60;
            String formattedTime = "";

            if (hours > 0) {
                formattedTime += hours + "h ";
            }
            formattedTime += minutes + "m";

            source.sendMessage(Component.text(""));
            source.sendMessage(Component.text("§ePlayer §a" + targetPlayerName + " §eis online in §a" + targetPlayerServer + "§7 " + formattedTime));
            source.sendMessage(Component.text("§eUUID: §7" + uuid));
            source.sendMessage(Component.text("§eIP: §7" + targetPlayerAddress + "§7 " + targetPlayerPing + "ms"));
            source.sendMessage(Component.text("§eOnline Mode: §7" + targetPlayerMode));
            source.sendMessage(Component.text("§eClient Version: §7" + targetPlayerVersion));
            source.sendMessage(Component.text("§eClient Brand: §7" + targetPlayerClient));
            source.sendMessage(Component.text(""));
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
        return proxy.getAllPlayers().stream()
                .map(Player::getUsername)
                .toList();
    }
}
