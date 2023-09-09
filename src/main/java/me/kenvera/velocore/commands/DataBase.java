package me.kenvera.velocore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.managers.SqlConnection;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;

public final class DataBase {
    public static BrigadierCommand createBrigadierCommand(final ProxyServer proxy, SqlConnection dataBase) {
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
                                dataBase.loadStaffData();
                            }

                            if (subCommand.equalsIgnoreCase("save")) {
                                dataBase.saveStaffData();
                            }

                            if (subCommand.equalsIgnoreCase("test")) {
                                try {
                                    dataBase.testDb();
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }

                            if (subCommand.equalsIgnoreCase("stats")) {
                                source.sendMessage(Component.text("Active Connections: " + dataBase.getActiveConnections()));
                                source.sendMessage(Component.text("Idle Connections: " + dataBase.getIdleConnections()));
                                source.sendMessage(Component.text("Total Connections: " + dataBase.getTotalConnections()));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
