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
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class UnMuteCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("unmute")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            List<String> suggestions = new ArrayList<>();

                            for (Player player : plugin.getProxy().getAllPlayers()) {
                                if (!player.equals(ctx.getSource()) && !player.hasPermission("velocity.staff")) {
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

                        .executes(ctx -> {
                            CommandSource source = ctx.getSource();
                            String playerArg = StringArgumentType.getString(ctx, "player");

                            Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);
                            if (targetPlayer.isPresent()) {
                                if (source instanceof Player playerSource) {
                                    try {
                                        plugin.getPlayerData().setMuted(plugin.getProxy().getPlayer(playerArg).get().getUniqueId().toString(), null);
                                        plugin.getRedis().removeKey("mute:" + targetPlayer.get().getUniqueId().toString());
                                        targetPlayer.get().sendMessage(Component.text(""));
                                        targetPlayer.get().sendMessage(Component.text("§7Your mute has been revoked. Enjoy the conversation!"));
                                        targetPlayer.get().sendMessage(Component.text(""));
                                        plugin.broadcastStaff("");
                                        plugin.broadcastStaff("§7" + targetPlayer.get().getUsername() + "§7 has been unmuted by " + playerSource.getUsername());
                                        plugin.broadcastStaff("");

                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + targetPlayer.get().getUsername() + "§7 has been unmuted by " + playerSource.getUsername()));
                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        playerSource.sendMessage(Component.text("§cThere was a SQL error when trying to unmute player!"));
                                    }
                                } else if (source instanceof ConsoleCommandSource) {
                                    try {
                                        plugin.getPlayerData().setMuted(plugin.getProxy().getPlayer(playerArg).get().getUniqueId().toString(), null);
                                        plugin.getRedis().removeKey("mute:" + targetPlayer.get().getUniqueId().toString());
                                        targetPlayer.get().sendMessage(Component.text(""));
                                        targetPlayer.get().sendMessage(Component.text("§7Your mute has been revoked. Enjoy the conversation!"));
                                        targetPlayer.get().sendMessage(Component.text(""));
                                        plugin.broadcastStaff("");
                                        plugin.broadcastStaff("§7" + targetPlayer.get().getUsername() + "§7 has been unmuted by Console");
                                        plugin.broadcastStaff("");

                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + targetPlayer.get().getUsername() + "§7 has been unmuted by Console"));
                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                    } catch (SQLException e) {
                                        e.printStackTrace();
                                        source.sendMessage(Component.text("§cThere was a SQL error when trying to unmute player!"));
                                    }
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
