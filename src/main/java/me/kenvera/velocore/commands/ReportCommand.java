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
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;

public final class ReportCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("report")
                .requires(src -> src.getPermissionValue("velocity.report") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            java.util.List<String> suggestions = new ArrayList<>();

                            for (Player player : plugin.getProxy().getAllPlayers()) {
                                if (!player.equals(ctx.getSource())) {
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

                                    List<Player> notifyPlayers = plugin.getProxy().getAllPlayers().stream()
                                            .filter(player -> player.hasPermission("velocity.report.notify") && player != playerSource)
                                            .toList();

                                    TextComponent message = Component.text()
                                            .append(Component.text("§c[Report] "))
                                            .append(Component.text(playerSource.getUsername() + " reported " + playerArg + " for: ")
                                                    .color(NamedTextColor.GRAY))
                                            .append(Component.text(reason)
                                                    .color(NamedTextColor.WHITE))
                                            .build();

                                    for (Player notifyPlayer : notifyPlayers) {
                                        notifyPlayer.sendMessage(message);
                                    }

                                    playerSource.sendMessage(Component.text("§aReport submitted successfully."));

                                    return Command.SINGLE_SUCCESS;
                                }))
                ).build();
        return new BrigadierCommand(node);
    }
}


