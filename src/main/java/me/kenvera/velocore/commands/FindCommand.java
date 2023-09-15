package me.kenvera.velocore.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class FindCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("find")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
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
                        .executes(ctx -> {
                            CommandSource source = ctx.getSource();
                            Player playerSource = (Player) source;
                            String playerArg = StringArgumentType.getString(ctx, "player");
                            Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);

                            if (targetPlayer.isPresent()) {
                                String targetPlayerServer = targetPlayer.get().getCurrentServer().get().getServerInfo().getName();
                                UUID uuid = targetPlayer.get().getUniqueId();
                                String targetPlayerAddress = "/" + targetPlayer.get().getRemoteAddress().getHostName();
                                boolean targetPlayerMode = targetPlayer.get().isOnlineMode();
                                ProtocolVersion targetPlayerVersion = targetPlayer.get().getProtocolVersion();
                                String targetPlayerClient = targetPlayer.get().getClientBrand();
                                long targetPlayerPing = targetPlayer.get().getPing();
                                long onlineTime = plugin.getPlayerSession().getOrDefault(uuid, 0L);
                                long currentTime = System.currentTimeMillis();

                                long onlineSession = currentTime - onlineTime;
                                long totalMinutes = TimeUnit.MILLISECONDS.toMinutes(onlineSession);
                                long hours = totalMinutes / 60;
                                long minutes = totalMinutes % 60;
                                String formattedTime = "";

                                if (hours > 0) {
                                    formattedTime += hours + "h ";
                                }
                                formattedTime += minutes + "m";

                                source.sendMessage(Component.text(""));
                                source.sendMessage(Component.text("§ePlayer §a" + targetPlayer.get().getUsername() + " §eis online in §a" + targetPlayerServer + "§7 " + formattedTime));
                                source.sendMessage(Component.text("§eUUID: §7" + uuid));
                                source.sendMessage(Component.text("§eIP: §7" + targetPlayerAddress + "§7 " + targetPlayerPing + "ms"));
                                source.sendMessage(Component.text("§eOnline Mode: §7" + targetPlayerMode));
                                source.sendMessage(Component.text("§eClient Version: §7" + targetPlayerVersion));
                                source.sendMessage(Component.text("§eClient Brand: §7" + targetPlayerClient));
                                source.sendMessage(Component.text(""));
                            } else {
                                source.sendMessage(Component.text("§cPlayer " + playerArg + " seems to be offline"));
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}


