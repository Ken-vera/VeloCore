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
import me.kenvera.velocore.managers.Ban;
import net.kyori.adventure.text.Component;

public final class UnBanCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("unban")
                .requires(src -> src.getPermissionValue("velocity.staff.unban") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("player", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            java.util.List<String> suggestions = plugin.getBanManager().getBanned();

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
                            String playerArg = StringArgumentType.getString(ctx, "player");
                            String uuid = plugin.getBanManager().getUUID(playerArg);

                            if (source instanceof Player playerSource) {
                                if (uuid != null) {
                                    Ban ban = plugin.getBanManager().getBan(uuid);
                                    if (ban != null) {
                                        String playerName = ban.getUsername(plugin);
                                        plugin.getBanManager().unBan(uuid, playerSource.getUsername(), ban.getId());
                                        plugin.getProxy().getAllPlayers().stream().filter(player1 -> player1.hasPermission("velocity.staff")).forEach(playerOnline -> {
                                            playerOnline.sendMessage(Component.text(""));
                                            playerOnline.sendMessage(Component.text("§7" + playerName + " §7successfully unbanned by " + playerSource.getUsername()));
                                            playerOnline.sendMessage(Component.text(""));
                                        });
                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + playerName + " §7successfully unbanned by " + playerSource.getUsername()));
                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                    } else {
                                        source.sendMessage(Component.text("§cYou can't unban player if they don't have an active ban!"));
                                    }
                                } else {
                                    source.sendMessage(Component.text("§7" + playerArg + " §cplayer data can't be found within database!"));
                                }

                            } else if (source instanceof ConsoleCommandSource) {
                                if (uuid != null) {
                                    Ban ban = plugin.getBanManager().getBan(uuid);
                                    if (ban != null) {
                                        String playerName = ban.getUsername(plugin);
                                        plugin.getBanManager().unBan(uuid, "Console", ban.getId());
                                        plugin.getProxy().getAllPlayers().stream().filter(player1 -> player1.hasPermission("velocity.staff")).forEach(playerOnline -> {
                                            playerOnline.sendMessage(Component.text(""));
                                            playerOnline.sendMessage(Component.text("§7" + playerName + " §7successfully unbanned by Console"));
                                            playerOnline.sendMessage(Component.text(""));
                                        });
                                        source.sendMessage(Component.text(""));
                                        source.sendMessage(Component.text("§7" + playerName + " §7successfully unbanned by Console"));
                                        source.sendMessage(Component.text(""));
                                    } else {
                                        source.sendMessage(Component.text("§cYou can't unban player if they don't have an active ban!"));
                                    }
                                } else {
                                    source.sendMessage(Component.text("§7" + playerArg + " §cplayer data can't be found within database!"));
                                }
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
