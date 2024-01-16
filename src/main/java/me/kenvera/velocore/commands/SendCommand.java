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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;
import xyz.kyngs.librelogin.api.provider.LibreLoginProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SendCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("send")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");

                            List<String> suggestions = new ArrayList<>();

                            if (ctx.getSource().hasPermission("velocity.send.all")) {
                                suggestions.add("all");
                            }

                            if (ctx.getSource().hasPermission("velocity.send.current")) {
                                suggestions.add("current");
                            }

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
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("server", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    String inputPart = ctx.getInput().toLowerCase();
                                    String[] inputParts = inputPart.split(" ");

                                    List<String> suggestions = new ArrayList<>();

                                    plugin.getProxy().getAllServers().forEach(server -> suggestions.add(server.getServerInfo().getName()));

                                    for (String suggestion : suggestions) {
                                        if (inputParts.length == 3) {
                                            String input = inputParts[2];
                                            if (suggestion.toLowerCase().startsWith(input)) {
                                                builder.suggest(suggestion);
                                            }
                                        } else if (inputParts.length <= 3) {
                                            builder.suggest(suggestion);
                                        }
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    Player playerSource = (Player) source;
                                    String target = StringArgumentType.getString(ctx, "player");
                                    String server = StringArgumentType.getString(ctx, "server");

                                    if (target.equalsIgnoreCase("all")) {
                                        if (source.hasPermission("velocity.send.all")) {
                                            Optional<RegisteredServer> targetServer = plugin.getProxy().getServer(server);

                                            if (targetServer.isPresent()) {
                                                 plugin.getProxy().getAllPlayers().stream()
                                                        .filter(player -> !player.equals(playerSource))
                                                        .filter(player -> !Objects.equals(player.getCurrentServer().get().getServerInfo().getName(), server))
                                                        .forEach(player -> player.createConnectionRequest(targetServer.get()).fireAndForget());
                                                source.sendMessage(Component.text("§aSuccessfully sent " +
                                                        plugin.getProxy().getAllPlayers().stream()
                                                                .filter(player -> !player.equals(playerSource))
                                                                .filter(player -> !Objects.equals(player.getCurrentServer().get().getServerInfo().getName(), server)).toList().size() +
                                                        " Players to " + targetServer.get().getServerInfo().getName()));
                                            } else {
                                                source.sendMessage(Component.text("§cServer " + server + "  seems offline."));
                                            }
                                        } else {
                                            source.sendMessage(Component.text("§cYou don't have permission to run this command!"));
                                        }
                                    } else if (target.equalsIgnoreCase("current")) {
                                        if (source.hasPermission("velocity.send.current")) {
                                            Optional<RegisteredServer> targetServer = plugin.getProxy().getServer(server);

                                            if (targetServer.isPresent()) {
                                                plugin.getProxy().getAllPlayers().stream()
                                                        .filter(player -> !player.equals(playerSource))
                                                        .filter(player -> Objects.equals(player.getCurrentServer().get().getServerInfo().getName(), playerSource.getCurrentServer().get().getServerInfo().getName()))
                                                        .forEach(player -> player.createConnectionRequest(targetServer.get()).fireAndForget());
                                                source.sendMessage(Component.text("§aSuccessfully sent " +
                                                        plugin.getProxy().getAllPlayers().stream()
                                                                .filter(player -> !player.equals(playerSource))
                                                                .filter(player -> Objects.equals(player.getCurrentServer().get().getServerInfo().getName(), playerSource.getCurrentServer().get().getServerInfo().getName())).toList().size() +
                                                        " Players to " + targetServer.get().getServerInfo().getName()));
                                            } else {
                                                source.sendMessage(Component.text("§cServer " + server + " seems to be offline."));
                                            }
                                        } else {
                                            source.sendMessage(Component.text("§cYou don't have permission to run this command!"));
                                        }
                                    } else {
                                        Optional<Player> targetPlayerOptional = plugin.getProxy().getPlayer(target);
                                        if (targetPlayerOptional.isPresent()) {
                                            var api = ((LibreLoginProvider<Player, RegisteredServer>) plugin.getProxy().getPluginManager().getPlugin("librelogin").orElseThrow().getInstance().orElseThrow()).getLibreLogin();
                                            Player targetPlayer = targetPlayerOptional.get();
                                            if (api.getAuthorizationProvider().isAuthorized(targetPlayer)) {
                                                Optional<RegisteredServer> targetServer = plugin.getProxy().getServer(server);
                                                if (targetServer.isPresent()) {
                                                    targetPlayer.createConnectionRequest(targetServer.get()).fireAndForget();
                                                    source.sendMessage(Component.text("§aSuccesfully sent " + targetPlayer.getUsername() + " to " + targetServer.get().getServerInfo().getName()));
                                                } else {
                                                    source.sendMessage(Component.text("§cServer " + server + " seems to be offline."));
                                                }
                                            } else {
                                                source.sendMessage(Component.text("§c" + targetPlayer.getUsername() + " §cis not authenticated"));
                                            }
                                        } else {
                                            source.sendMessage(Component.text("§cPlayer " + target + " seems not available within the server."));
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                        )
                .build();
        return new BrigadierCommand(node);
    }
}
