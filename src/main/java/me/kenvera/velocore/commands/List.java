package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class List implements SimpleCommand {
    private final ProxyServer proxy;
    private final Map<UUID, Long> playerOnlineSession;
    private final LuckPerms luckPerms;

    public List(ProxyServer proxy, Map<UUID, Long> playerOnlineSession) {
        this.proxy = proxy;
        this.playerOnlineSession = playerOnlineSession;
        this.luckPerms = LuckPermsProvider.get();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (source.getPermissionValue("velocity.staff") != Tristate.TRUE) {
            source.sendMessage(Component.text("§cYou don't have permission to run this command!"));
            return;
        }

        java.util.List<Player> onlineStaff = proxy.getAllPlayers().stream()
                .filter(player -> player.hasPermission("velocity.staff"))
                .toList();

        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§aThere are ")
                .append(Component.text(onlineStaff.size()))
                .append(Component.text(" §aStaff[s] online:")));

        for (Player staffMember : onlineStaff) {
            UUID uuid = staffMember.getUniqueId();
            User user = luckPerms.getUserManager().getUser(uuid);
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

            assert user != null;
            CachedMetaData metaData = user.getCachedData().getMetaData();
            String prefix = Objects.requireNonNull(metaData.getPrefix()).replaceAll("&", "§");

            source.sendMessage(Component.text(prefix + " ")
                    .append(Component.text(staffMember.getUsername()))
                    .append(Component.text(" §7- "))
                    .append(Component.text("§7" + staffMember.getCurrentServer().get().getServerInfo().getName()))
                    .append(Component.text("§7 " + formattedTime)));
        }
        source.sendMessage(Component.text(""));
    }
}
