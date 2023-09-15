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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

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
                                    UUID uuid = playerSource.getUniqueId();
                                    String playerArg = StringArgumentType.getString(ctx, "player");
                                    String reason = StringArgumentType.getString(ctx, "reason");

                                    Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);
                                    if (targetPlayer.isPresent()) {
                                        Player player = targetPlayer.get();
                                        if (player.getPermissionValue("velocity.ban.prevent") != Tristate.TRUE) {
                                            try {
                                                player.disconnect(Component.text("§cYou have been banned by " + ((Player) source).getUsername()));
                                                plugin.getBanManager().addBan(player.getUniqueId().toString(), player.getUsername(), playerSource.getUsername(), reason);
                                            } catch (SQLException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }

                                    return Command.SINGLE_SUCCESS;
                                }))
                ).build();
        return new BrigadierCommand(node);
    }
}


