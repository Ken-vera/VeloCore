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

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public final class Debug {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("debug")
                .requires(src -> src.getPermissionValue("velocity.staff.ban") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("string", StringArgumentType.word())
                        .executes(ctx -> {
                            CommandSource source = ctx.getSource();
                            Player playerSource = (Player) source;
                            String subCommand = StringArgumentType.getString(ctx, "string");
                            if (subCommand.equalsIgnoreCase("db")) {
                                try {
                                    PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement("SELECT expire, banned_time FROM CNS1_cnplayerdata_1.ban WHERE player_name = ? AND purged = 0 LIMIT 1");
                                    statement.setString(1, "MnaZ");
                                    ResultSet result = statement.executeQuery();
                                    if (result.next()) {
                                        long millis = result.getTimestamp("expire").toInstant().minusMillis(25200000).toEpochMilli();
                                        long bannedTime = result.getTimestamp("banned_time").toInstant().minusMillis(25200000).toEpochMilli();
                                        playerSource.sendMessage(Component.text("Banned Time Milliseconds: " + bannedTime));
                                        playerSource.sendMessage(Component.text("Expire Milliseconds: " + millis));
                                        playerSource.sendMessage(Component.text("Banned Time (LocalTime): " + Utils.parseDateTime(bannedTime, true)));
                                        playerSource.sendMessage(Component.text("Expire Time (LocalTime): " + Utils.parseDateTime(millis, true)));
                                    } else {
                                        playerSource.sendMessage(Component.text("null"));
                                    }

                                } catch (SQLException e) {
                                    playerSource.sendMessage(Component.text("Error" + e.getMessage()));
                                    e.printStackTrace();
                                }

                            } else if (subCommand.equalsIgnoreCase("tes")) {
                                plugin.getRedisConnection().getJedis().close();


                            } else if (subCommand.equalsIgnoreCase("stat")) {
                                playerSource.sendMessage(Component.text("Sql Active Connection: " + plugin.getSqlConnection().getActiveConnections()));
                                playerSource.sendMessage(Component.text("Sql Idle Connection: " + plugin.getSqlConnection().getIdleConnections()));
                                playerSource.sendMessage(Component.text("Sql Total Connection: " + plugin.getSqlConnection().getTotalConnections()));

                                playerSource.sendMessage(Component.text("Redis Active Connection: " + plugin.getRedisConnection().getNumActiveConnections()));
                                playerSource.sendMessage(Component.text("Redis Idle Connection: " + plugin.getRedisConnection().getNumIdleConnections()));
                                playerSource.sendMessage(Component.text("Redis Total Connection: " + plugin.getRedisConnection().getMaxTotalConnections()));

                            } else if (subCommand.equalsIgnoreCase("millis")) {
                                long millis = 1695040590000L; // Replace this with your actual milliseconds value

                                playerSource.sendMessage(Component.text("ZonedDateTime in Jakarta: " + Utils.parseDateTime(millis, false)));

                            } else if (subCommand.equalsIgnoreCase("savedb")) {
                                long millis = System.currentTimeMillis();
                                long expire = millis + 100000L;
                                try {
                                    PreparedStatement statement = plugin.getSqlConnection().getConnection().prepareStatement("INSERT INTO CNS1_cnplayerdata_1.ban (uuid, player_name, issuer, reason, expire, banned_time) VALUES (?, ?, ?, ?, ?, ?)");
                                    statement.setString(1, "4bc0f1dc-ade2-3a8d-8a30-c77454c1c37c");
                                    statement.setString(2, "MnaZ");
                                    statement.setString(3, "Kenvera");
                                    statement.setString(4, "tes");

                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Jakarta"));

                                    // Convert the timestamps to formatted date-time strings
                                    String expireFormatted = dateFormat.format(new Date(expire));
                                    String bannedTimeFormatted = dateFormat.format(new Date(millis));


                                    statement.setString(5, null);
                                    statement.setString(6, Utils.parseDateTime(millis, true));
                                    statement.executeUpdate();

                                    playerSource.sendMessage(Component.text("sent"));


                                } catch (SQLException e) {
                                    playerSource.sendMessage(Component.text("Error" + e.getMessage()));
                                    e.printStackTrace();
                                }

                            } else if (subCommand.equalsIgnoreCase("checktable")) {
                                String query = "SELECT table_name FROM information_schema.tables WHERE table_schema = ?";

                                try (Connection connection = plugin.getSqlConnection().getConnection()) {
                                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                                    preparedStatement.setString(1, "CNS1_cnplayerdata_1");

                                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                        while (resultSet.next()) {
                                            playerSource.sendMessage(Component.text(resultSet.getString("table_name")));
                                        }
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }

                            } else if (subCommand.equalsIgnoreCase("getgroup")) {
                                String query = "SELECT `group` FROM CNS1_cnplayerdata_1.player_data WHERE uuid = ?";
                                String uuid = playerSource.getUniqueId().toString();

                                try (Connection connection = plugin.getSqlConnection().getConnection()) {
                                    PreparedStatement preparedStatement = connection.prepareStatement(query);
                                    preparedStatement.setString(1, uuid);

                                    try (ResultSet resultSet = preparedStatement.executeQuery()) {
                                        while (resultSet.next()) {
                                            playerSource.sendMessage(Component.text(resultSet.getString("group")));
                                        }
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                String uuid = plugin.getBanManager().getUUID(subCommand);
                                Ban ban = plugin.getBanManager().getBan(uuid);
                                if (ban != null) {
                                    playerSource.sendMessage(Component.text(String.valueOf(ban)));
                                    playerSource.sendMessage(Component.text(ban.getId()));
                                }
                            }

                            return Command.SINGLE_SUCCESS;
                        })
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("table", StringArgumentType.word())
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    Player playerSource = (Player) source;
                                    String subCommand = StringArgumentType.getString(ctx, "string");
                                    String tableName = StringArgumentType.getString(ctx, "table");

                                    if (subCommand.equalsIgnoreCase("purgetable")) {
                                        if (!tableName.isEmpty()) {
                                            try (Connection connection = plugin.getSqlConnection().getConnection()) {
                                                String dropTableSQL = "DROP TABLE IF EXISTS " + tableName;
                                                PreparedStatement preparedStatement = connection.prepareStatement(dropTableSQL);
                                                preparedStatement.executeUpdate();
                                                playerSource.sendMessage(Component.text("Dropped table: " + tableName));

                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                                playerSource.sendMessage(Component.text("An error occurred while dropping tables."));
                                            }
                                        }
                                    } else if (subCommand.equalsIgnoreCase("addgroup")) {
                                        String uuid = playerSource.getUniqueId().toString();
                                        String group = StringArgumentType.getString(ctx, "table");
                                        try {
                                            plugin.getPlayerData().addGroup(uuid, group);
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                    } else if (subCommand.equalsIgnoreCase("removegroup")) {
                                        String uuid = playerSource.getUniqueId().toString();
                                        String group = StringArgumentType.getString(ctx, "table");
                                        try {
                                            plugin.getPlayerData().removeGroup(uuid, group);
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                })
                        )

                ).build();
        return new BrigadierCommand(node);
    }
}


