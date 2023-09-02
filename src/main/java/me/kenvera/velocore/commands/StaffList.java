package me.kenvera.velocore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
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

public final class StaffList {
    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy, Map<UUID, Long> playerOnlineSession) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("staffchat")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                    .executes(ctx -> {
                        LuckPerms luckPerms = LuckPermsProvider.get();
                        CommandSource source = ctx.getSource();
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

                        return Command.SINGLE_SUCCESS;
                    })
                .build();
        return new BrigadierCommand(node);
    }
}
