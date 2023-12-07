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
import net.kyori.adventure.text.Component;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.model.user.UserManager;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class GroupCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("group")
                .requires(src -> src.getPermissionValue("velocity.group") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subCommand", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            List<String> suggestions = new ArrayList<>();

                            suggestions.add("reset");
                            suggestions.add("set");
                            suggestions.add("list");
                            suggestions.add("lookup");

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
                            Player playerSource = (Player) source;
                            String subCommand = StringArgumentType.getString(ctx, "subCommand");

                            if (subCommand.equalsIgnoreCase("list")) {
                                Set<String> groups = plugin.getLuckPerms().getGroupManager().getLoadedGroups()
                                        .stream()
                                        .map(Group::getName)
                                        .collect(Collectors.toSet());

                                playerSource.sendMessage(Component.text("\n§6§lGroup List: \n"));
                                for (String group : groups) {
                                    playerSource.sendMessage(Component.text("- " + group));
                                }
                                playerSource.sendMessage(Component.text("\n"));
                            }

                            return Command.SINGLE_SUCCESS;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String inputPart = ctx.getInput().toLowerCase();
                                    String[] inputParts = inputPart.split(" ");
                                    List<String> suggestions;

                                    if (inputParts.length == 3) {
                                        String filter = inputParts[2].toLowerCase();
                                        suggestions = plugin.getPlayerData().getUsernames(filter);
                                    } else {
                                        suggestions = plugin.getPlayerData().getUsernames();
                                    }

                                    for (String suggestion : suggestions) {
                                        builder.suggest(suggestion);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    Player playerSource = (Player) source;
                                    String subCommand = StringArgumentType.getString(ctx, "subCommand");
                                    String player = StringArgumentType.getString(ctx, "player");
                                    String uuid = plugin.getPlayerData().getUUID(player);
                                    String group = Objects.requireNonNull(plugin.getLuckPerms().getUserManager().getUser(UUID.fromString(uuid))).getPrimaryGroup();

                                    if (subCommand.equalsIgnoreCase("reset")) {
                                        if (!group.equals("default")) {
                                            try {
                                                plugin.getPlayerData().setGroup(uuid, "default");
                                                playerSource.sendMessage(Component.text("§aSuccesfully reset " + player + "'s §agroup!"));
                                            } catch (SQLException e) {
                                                playerSource.sendMessage(Component.text("§cDatabase error occured when trying to reset " + player + "'s §cgroup!"));
                                                e.printStackTrace();
                                            }
                                        }
                                    } else if (subCommand.equalsIgnoreCase("lookup")) {
                                        group = plugin.getPlayerData().getGroup(uuid);
                                        playerSource.sendMessage(Component.text("§aPlayer §b" + player + " §a" + group));
                                    }

                                    return Command.SINGLE_SUCCESS;
                                })
                        )
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    String inputPart = ctx.getInput().toLowerCase();
                                    String[] inputParts = inputPart.split(" ");
                                    List<String> suggestions;

                                    if (inputParts.length == 3) {
                                        String filter = inputParts[2].toLowerCase();
                                        suggestions = plugin.getPlayerData().getUsernames(filter);
                                    } else {
                                        suggestions = plugin.getPlayerData().getUsernames();
                                    }

                                    for (String suggestion : suggestions) {
                                        builder.suggest(suggestion);
                                    }
                                    return builder.buildFuture();
                                })
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("group", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            String inputPart = ctx.getInput().toLowerCase();
                                            String[] inputParts = inputPart.split(" ");

                                            List<String> suggestions = plugin.getLuckPerms().getGroupManager().getLoadedGroups()
                                                    .stream()
                                                    .map(Group::getName).distinct().toList();

                                            for (String suggestion : suggestions) {
                                                if (inputParts.length == 4) {
                                                    String input = inputParts[3];
                                                    if (suggestion.toLowerCase().startsWith(input)) {
                                                        builder.suggest(suggestion);
                                                    }
                                                } else if (inputParts.length <= 4) {
                                                    builder.suggest(suggestion);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            CommandSource source = ctx.getSource();
                                            Player playerSource = (Player) source;
                                            String subCommand = StringArgumentType.getString(ctx, "subCommand");
                                            String player = StringArgumentType.getString(ctx, "player");
                                            String group = StringArgumentType.getString(ctx, "group");

                                            CompletableFuture<String> uuidFuture = CompletableFuture.supplyAsync(() ->
                                                    plugin.getPlayerData().getUUID(player)
                                            );

                                            uuidFuture.thenAcceptAsync(uuid -> {
                                                // Load user asynchronously
                                                UserManager userManager = plugin.getLuckPerms().getUserManager();
                                                CompletableFuture<User> userFuture = userManager.loadUser(UUID.fromString(uuid));

                                                userFuture.thenAcceptAsync(user -> {
                                                    String playerGroup = user.getPrimaryGroup();

                                                    if (subCommand.equalsIgnoreCase("set")) {
                                                        if (!playerGroup.equals(group)) {
                                                            try {
                                                                plugin.getPlayerData().setGroup(uuid, group);
                                                                plugin.getRedis().publish("chronosync", "set_" + uuid + "_" + group);
                                                                playerSource.sendMessage(Component.text("§aSuccessfully set " + player + "'s §agroup to " + group));
                                                            } catch (SQLException e) {
                                                                playerSource.sendMessage(Component.text("§cDatabase error occurred when trying to set " + player + "'s §cgroup to " + group));
                                                                e.printStackTrace();
                                                            }
                                                        } else {
                                                            playerSource.sendMessage(Component.text(player + "§calready has " + group + " §cgroup!"));
                                                        }
                                                    }
                                                }).exceptionally(exception -> {
                                                    // Handle exceptions if loading the user fails
                                                    playerSource.sendMessage(Component.text("§cError occurred while loading user data."));
                                                    exception.printStackTrace();
                                                    return null;
                                                });

                                            }).exceptionally(exception -> {
                                                // Handle exceptions if getting UUID fails
                                                playerSource.sendMessage(Component.text("§cError occurred while getting UUID."));
                                                exception.printStackTrace();
                                                return null;
                                            });

//                                            String uuid = plugin.getPlayerData().getUUID(player);
//                                            User user = plugin.getLuckPerms().getUserManager().getUser(player);
//                                            String playerGroup = user.getPrimaryGroup();
//
//                                            if (subCommand.equalsIgnoreCase("set")) {
//                                                if (!playerGroup.equals(group)) {
//                                                    try {
//                                                        plugin.getPlayerData().setGroup(uuid, group);
//                                                        playerSource.sendMessage(Component.text("§aSuccesfully set " + player + "'s §agroup to " + group));
//                                                    } catch (SQLException e) {
//                                                        playerSource.sendMessage(Component.text("§cDatabase error occured when trying to set " + player + "'s §cgroup to " + group));
//                                                        e.printStackTrace();
//                                                    }
//                                                } else {
//                                                    playerSource.sendMessage(Component.text(player + "§calready has " + group + " §cgroup!"));
//                                                }
//                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }
}
