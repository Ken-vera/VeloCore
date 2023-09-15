package me.kenvera.velocore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;

public final class DataBaseCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("database")
                .requires(src -> src.getPermissionValue("velocity.staff") != Tristate.UNDEFINED)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            java.util.List<String> suggestions = new ArrayList<>();

                            suggestions.add("load");
                            suggestions.add("save");

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
                            String subCommand = StringArgumentType.getString(ctx, "subcommand");

                            if (subCommand.equalsIgnoreCase("load")) {
                                plugin.getSqlConnection().loadStaffData();
                            }

                            if (subCommand.equalsIgnoreCase("save")) {
                                plugin.getSqlConnection().saveStaffData();
                            }

                            if (subCommand.equalsIgnoreCase("stats")) {
                                source.sendMessage(Component.text("Active Connections: " + plugin.getSqlConnection().getActiveConnections()));
                                source.sendMessage(Component.text("Idle Connections: " + plugin.getSqlConnection().getIdleConnections()));
                                source.sendMessage(Component.text("Total Connections: " + plugin.getSqlConnection().getTotalConnections()));
                            }

                            if (subCommand.equalsIgnoreCase("redis")) {
                                source.sendMessage(Component.text("Active Connections: " + plugin.getRedisConnection().getNumActiveConnections()));
                                source.sendMessage(Component.text("Idle Connections: " + plugin.getRedisConnection().getNumIdleConnections()));
                                source.sendMessage(Component.text("Total Connections: " + plugin.getRedisConnection().getMaxTotalConnections()));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
