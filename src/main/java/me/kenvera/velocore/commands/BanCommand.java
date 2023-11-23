package me.kenvera.velocore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.managers.Ban;
import me.kenvera.velocore.managers.Utils;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

public final class BanCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("ban")
                .requires(src -> src.getPermissionValue("velocity.staff.ban") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            java.util.List<String> suggestions = new ArrayList<>();

                            for (Player player : plugin.getProxy().getAllPlayers()) {
                                if (!player.equals(ctx.getSource()) || !player.hasPermission("velocity.staff")) {
                                    String playerName = player.getUsername();
                                    suggestions.add(playerName);
                                }
                            }

                            for (String suggestion : suggestions) {
                                if (inputParts.length == 2) {
                                    String input = inputParts[1];
                                    if (suggestion.toLowerCase().startsWith(input)) {
                                        builder.suggest(suggestion);
                                    }
                                } else if (inputParts.length <= 2) {
                                    builder.suggest(suggestion);
                                }
                            }
                            return builder.buildFuture();
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.string())
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    Player playerSource = (Player) source;
                                    String playerArg = StringArgumentType.getString(ctx, "player");
                                    String reason = StringArgumentType.getString(ctx, "reason");

                                    Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);
                                    if (targetPlayer.isPresent()) {

                                        Player player = targetPlayer.get();

                                        if (player.getPermissionValue("velocity.ban.prevent") != Tristate.TRUE) {

                                            String UUID = plugin.getBanManager().getUUID(playerArg);
                                            Ban ban = plugin.getBanManager().getBan(UUID);
                                            if (ban != null) {
                                                source.sendMessage(Component.text("§c" + playerArg + "§calready have an active punishment!"));
                                            } else {
                                                player.disconnect(Component.text("§cYou have been banned by " + ((Player) source).getUsername()));
                                                plugin.getBanManager().addBanSql(player.getUniqueId().toString(), player.getUsername(), playerSource.getUsername(), reason);
                                            }
                                        }
                                    } else {
                                        String uuid = plugin.getBanManager().getUUID(playerArg);
                                        String playerName = plugin.getBanManager().getUsername(uuid);
                                        if (uuid != null) {

                                            Ban currentBan = plugin.getBanManager().getBan(uuid);
                                            if (currentBan != null) {

                                                long bannedTime = plugin.getBanManager().getBan(uuid).getBannedTime();
                                                source.sendMessage(Component.text("§c" + playerName + "§chas been permanently banned on §7" + Utils.parseDateTime(bannedTime, true)));

                                            } else {
                                                plugin.getBanManager().addBanRedis(uuid, playerName, playerSource.getUsername(), reason, -1);
                                                plugin.getBanManager().addBanSql(uuid, playerName, playerSource.getUsername(), reason, -1);
                                                source.sendMessage(Utils.formatBanMessage(plugin));
                                            }
                                        } else {
                                            source.sendMessage(Component.text("§c" + playerArg + "§cplayer data can't be found within database!"));
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                ).build();
        return new BrigadierCommand(node);
    }
}
