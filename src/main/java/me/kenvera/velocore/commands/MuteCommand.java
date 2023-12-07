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
import me.kenvera.velocore.managers.Utils;
import net.kyori.adventure.text.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class MuteCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("mute")
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
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("duration", StringArgumentType.string())
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    Player playerSource = (Player) source;
                                    String playerArg = StringArgumentType.getString(ctx, "player");
                                    String duration = StringArgumentType.getString(ctx, "duration");

                                    long expire = System.currentTimeMillis() + getDuration(duration);

                                    Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);
                                    if (targetPlayer.isPresent()) {
                                        try {
                                            plugin.getPlayerData().setMuted(plugin.getProxy().getPlayer(playerArg).get().getUniqueId().toString(), expire);
                                            plugin.broadcast("");
                                            plugin.broadcast("§7Oops! Seems like someone needs a little break from chatting.");
                                            targetPlayer.get().sendMessage(Component.text("§7You've been muted for " + duration + ". Enjoy the silence!"));
                                            plugin.broadcastStaff("§7" + targetPlayer.get().getUsername() + "§7 has been muted for " + duration + "§7 by " + playerSource.getUsername());
                                            plugin.broadcast("");
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                )
                .build();
        return new BrigadierCommand(node);
    }

    public static long getDuration(String durationString) {
        if (Utils.isInt(durationString)) {
            return Integer.parseInt(durationString) * 1000L;
        } else if (durationString.endsWith("w")) {
            durationString = durationString.replace("w", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * 60L * 24L * 7L * Integer.parseInt(durationString) * 1000L;
        } else if (durationString.endsWith("d")) {
            durationString = durationString.replace("d", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * 60L * 24L * Integer.parseInt(durationString) * 1000L;
        } else if (durationString.endsWith("h")) {
            durationString = durationString.replace("h", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * 60L * Integer.parseInt(durationString) * 1000L;
        } else if (durationString.endsWith("m")) {
            durationString = durationString.replace("m", "");
            if (!Utils.isInt(durationString)) return 0;
            return 60L * Integer.parseInt(durationString) * 1000L;
        } else if (durationString.endsWith("s")) {
            durationString = durationString.replace("s", "");
            if (!Utils.isInt(durationString)) return 0;
            return Integer.parseInt(durationString) * 1000L;
        } else {
            return 0;
        }
    }
}
