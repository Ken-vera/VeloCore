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

                            suggestions.add("add");
                            suggestions.add("remove");
                            suggestions.add("set");
                            suggestions.add("reset");
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
                            String subCommand = StringArgumentType.getString(ctx, "subCommand");

                            if (subCommand.equalsIgnoreCase("list")) {
                                Set<String> groups = plugin.getLuckPerms().getGroupManager().getLoadedGroups()
                                        .stream()
                                        .map(Group::getName)
                                        .collect(Collectors.toSet());

                                source.sendMessage(Component.text("\n§6§lGroup List: \n"));
                                for (String group : groups) {
                                    source.sendMessage(Component.text("- " + group));
                                }
                                source.sendMessage(Component.text("\n"));
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
                                    String subCommand = StringArgumentType.getString(ctx, "subCommand");
                                    String player = StringArgumentType.getString(ctx, "player");
                                    String uuid = plugin.getPlayerData().getUUID(player);
                                    String group = Objects.requireNonNull(plugin.getLuckPerms().getUserManager().getUser(UUID.fromString(uuid))).getPrimaryGroup();

                                    if (source instanceof Player || source instanceof ConsoleCommandSource) {
                                        if (subCommand.equalsIgnoreCase("reset")) {
                                            if (source.getPermissionValue("velocity.group.reset") == Tristate.TRUE) {
                                                if (!group.equals("default")) {
                                                    try {
                                                        plugin.getPlayerData().setGroup(uuid, "default");
                                                        source.sendMessage(Component.text("§aSuccesfully reset " + player + "'s §agroup!"));
                                                    } catch (SQLException e) {
                                                        source.sendMessage(Component.text("§cDatabase error occured when trying to reset " + player + "'s §cgroup!"));
                                                        e.printStackTrace();
                                                    }
                                                }
                                            } else {
                                                source.sendMessage(Component.text("§cYou don't have permission to execute this command!"));
                                            }
                                        } else if (subCommand.equalsIgnoreCase("lookup")) {
                                            if (source.getPermissionValue("velocity.group.lookup") == Tristate.TRUE) {
                                                group = plugin.getPlayerData().getGroup(uuid);
                                                source.sendMessage(Component.text("§aPlayer §b" + player + " §a" + group));
                                            } else {
                                                source.sendMessage(Component.text("§cYou don't have permission to execute this command!"));
                                            }
                                        }
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
                                            String subCommand = StringArgumentType.getString(ctx, "subCommand");
                                            String player = StringArgumentType.getString(ctx, "player");
                                            String group = StringArgumentType.getString(ctx, "group");

                                            if (source instanceof Player || source instanceof ConsoleCommandSource) {
                                                CompletableFuture<String> uuidFuture = CompletableFuture.supplyAsync(() ->
                                                        plugin.getPlayerData().getUUID(player)
                                                );

                                                uuidFuture.thenAcceptAsync(uuid -> {
                                                    // Load user asynchronously
                                                    UserManager userManager = plugin.getLuckPerms().getUserManager();
                                                    CompletableFuture<User> userFuture = userManager.loadUser(UUID.fromString(uuid));

                                                    userFuture.thenAcceptAsync(user -> {
                                                        String playerGroup = user.getPrimaryGroup();
                                                        Set<String> groupList = plugin.getLuckPerms().getGroupManager().getLoadedGroups()
                                                                .stream()
                                                                .map(Group::getName)
                                                                .collect(Collectors.toSet());

                                                        if (subCommand.equalsIgnoreCase("set")) {
                                                            if (source.getPermissionValue("velocity.group.set") == Tristate.TRUE) {
                                                                if (groupList.contains(group)) {
                                                                    if (!playerGroup.equals(group)) {
                                                                        try {
                                                                            plugin.getPlayerData().setGroup(uuid, group);
                                                                            plugin.getRedis().publish("chronosync", "set_" + uuid + "_" + group);
                                                                            source.sendMessage(Component.text("§aSuccessfully set §b" + player + "'s §agroup to §f" + group));
                                                                        } catch (SQLException e) {
                                                                            source.sendMessage(Component.text("§cDatabase error occurred when trying to set §b" + player + "'s §cgroup to §f" + group));
                                                                            e.printStackTrace();
                                                                        }
                                                                    } else {
                                                                        source.sendMessage(Component.text("§b" + player + " §calready has §f" + group + " §cgroup!"));
                                                                    }
                                                                } else {
                                                                    source.sendMessage(Component.text(group + " §cgroup is not available within luckperms!"));
                                                                }
                                                            } else {
                                                                source.sendMessage(Component.text("§cYou don't have permission to execute this command!"));
                                                            }

                                                        } else if (subCommand.equalsIgnoreCase("add")) {
                                                            if (source.getPermissionValue("velocity.group.add") == Tristate.TRUE) {
                                                                if (groupList.contains(group)) {
                                                                    if (!playerGroup.equals(group)) {
                                                                        try {
                                                                            plugin.getPlayerData().addGroup(uuid, group);
                                                                            plugin.getRedis().publish("chronosync", "add_" + uuid + "_" + group);
                                                                            source.sendMessage(Component.text("§aSuccessfully added §f" + group + " §ato §b" + player + "'s §agroup"));
                                                                        } catch (SQLException e) {
                                                                            source.sendMessage(Component.text("§cDatabase error occurred when trying to adding §f" + group + " §cinto §b" + player + "'s §cgroup"));
                                                                            e.printStackTrace();
                                                                        }
                                                                    } else {
                                                                        source.sendMessage(Component.text("§b" + player + " §calready has §f" + group + " §cgroup!"));
                                                                    }
                                                                } else {
                                                                    source.sendMessage(Component.text(group + " §cgroup is not available within luckperms!"));
                                                                }
                                                            } else {
                                                                source.sendMessage(Component.text("§cYou don't have permission to execute this command!"));
                                                            }

                                                        } else if (subCommand.equalsIgnoreCase("remove")) {
                                                            if (source.getPermissionValue("velocity.group.remove") == Tristate.TRUE) {
                                                                if (groupList.contains(group)) {
                                                                    Collection<Group> inheritedGroups = user.getInheritedGroups(user.getQueryOptions());

                                                                    boolean hasgroup = false;
                                                                    for (Group group1 : inheritedGroups) {
                                                                        if (group1.getName().equalsIgnoreCase(group)) {
                                                                            hasgroup = true;
                                                                            break;
                                                                        }
                                                                    }

                                                                    if (hasgroup) {
                                                                        try {
                                                                            plugin.getPlayerData().removeGroup(uuid, group);
                                                                            plugin.getRedis().publish("chronosync", "remove_" + uuid + "_" + group);
                                                                            source.sendMessage(Component.text("§aSuccessfully remove §f" + group + " §afrom §b" + player + "'s §agroup"));
                                                                        } catch (SQLException e) {
                                                                            source.sendMessage(Component.text("§cDatabase error occurred when trying to remove §f" + group + " §cfrom §b" + player + "'s §cgroup"));
                                                                            e.printStackTrace();
                                                                        }
                                                                    } else {
                                                                        source.sendMessage(Component.text("§b" + player + " §cdon't have §f" + group + " §cgroup to be removed!"));
                                                                    }
                                                                } else {
                                                                    source.sendMessage(Component.text(group + " §cgroup is not available within luckperms!"));
                                                                }
                                                            } else {
                                                                source.sendMessage(Component.text("§cYou don't have permission to execute this command!"));
                                                            }
                                                        }


                                                    }).exceptionally(exception -> {
                                                        source.sendMessage(Component.text("§cError occurred while loading user data."));
                                                        exception.printStackTrace();
                                                        return null;
                                                    });

                                                }).exceptionally(exception -> {
                                                    source.sendMessage(Component.text("§cError occurred while getting UUID."));
                                                    exception.printStackTrace();
                                                    return null;
                                                });
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                )
                .build();
        return new BrigadierCommand(node);
    }
}
