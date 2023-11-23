package me.kenvera.velocore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.managers.Ban;
import me.kenvera.velocore.managers.Utils;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;

public final class TempBanCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("tempban")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
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
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("duration", StringArgumentType.string())
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.string())
                                        .executes(ctx -> {
                                            CommandSource source = ctx.getSource();
                                            Player playerSource = (Player) source;
                                            String playerArg = StringArgumentType.getString(ctx, "player");
                                            String reason = StringArgumentType.getString(ctx, "reason");
                                            String duration = StringArgumentType.getString(ctx, "duration");

                                            long expire = System.currentTimeMillis() + getBanDuration(duration);
                                            Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);
                                            if (targetPlayer.isPresent()) {
                                                Player player = targetPlayer.get();
                                                if (player.getPermissionValue("velocity.ban.prevent") != Tristate.TRUE) {
                                                    plugin.getBanManager().addBanRedis(player.getUniqueId().toString(), player.getUsername(), playerSource.getUsername(), reason + " redis", expire);
                                                    plugin.getBanManager().addBanSql(player.getUniqueId().toString(), player.getUsername(), playerSource.getUsername(), reason, expire);
                                                    player.disconnect(Utils.formatBannedMessage(plugin, source instanceof ConsoleCommandSource ? "Console" : (((Player) source).getUsername()), reason, expire, plugin.getBanManager().getID(player.getUniqueId().toString())));
                                                    source.sendMessage(Utils.formatBanMessage(plugin));

                                                } else {
                                                    source.sendMessage(Component.text("§cThis player has immuned to ban!"));
                                                }
                                            } else {
                                                String uuid = plugin.getBanManager().getUUID(playerArg);
                                                String playerName = plugin.getBanManager().getUsername(uuid);
                                                if (uuid != null) {
                                                    Ban currentBan = plugin.getBanManager().getBan(uuid);
                                                    if (currentBan != null) {
                                                        source.sendMessage(Component.text("§c" + playerName + "§chas been banned until §7" + Utils.parseDateTime(currentBan.getExpire(), true)));
                                                    } else {
                                                        plugin.getBanManager().addBanRedis(uuid, playerName, playerSource.getUsername(), reason, expire);
                                                        plugin.getBanManager().addBanSql(uuid, playerName, playerSource.getUsername(), reason, expire);
                                                        source.sendMessage(Utils.formatBanMessage(plugin));
                                                    }
                                                } else {
                                                    source.sendMessage(Component.text("§c" + playerArg + "§cplayer data can't be found within database!"));
                                                }
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })))
                ).build();
        return new BrigadierCommand(node);
    }

    public static long getBanDuration(String durationString) {
        if (Utils.isInt(durationString)) {
            return 60L * 60 * 24 * Integer.parseInt(durationString) * 1000;
        } else if (durationString.endsWith("w")) {
            durationString = durationString.replace("w", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * 60 * 24 * 24 * Integer.parseInt(durationString) * 1000;
        } else if (durationString.endsWith("d")) {
            durationString = durationString.replace("d", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * 60 * 24 * Integer.parseInt(durationString) * 1000;
        } else if (durationString.endsWith("h")) {
            durationString = durationString.replace("h", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * 60 * Integer.parseInt(durationString) * 1000;
        } else if (durationString.endsWith("m")) {
            durationString = durationString.replace("m", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * Integer.parseInt(durationString) * 1000;
        } else if (durationString.endsWith("s")) {
            durationString = durationString.replace("s", "");
            if (!Utils.isInt(durationString)) return 0;
            return Integer.parseInt(durationString) * 1000;
        } else {
            return 0;
        }
    }
}
