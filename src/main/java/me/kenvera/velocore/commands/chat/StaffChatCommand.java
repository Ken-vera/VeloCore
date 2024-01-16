package me.kenvera.velocore.commands.chat;

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
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class StaffChatCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("staffchat")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            List<String> suggestions = new ArrayList<>();

                            suggestions.add("toggle");
                            suggestions.add("mute");

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

                            if (source instanceof Player playerSource) {
                                UUID uuid = playerSource.getUniqueId();
                                if (subCommand.equalsIgnoreCase("toggle")) {
                                    boolean currentStatus = plugin.getPlayerStaffChat().getOrDefault(uuid, false);

                                    if (!currentStatus) {
                                        plugin.getPlayerStaffChat().put(uuid, true);
                                        playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §aStaff Chat is Enabled!"));
                                    } else {
                                        plugin.getPlayerStaffChat().put(uuid, false);
                                        playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §cStaff Chat is Disabled!"));
                                    }
                                } else if (subCommand.equalsIgnoreCase("mute")) {
                                    boolean currenStatus = plugin.getPlayerStaffChatMute().getOrDefault(uuid, false);

                                    if (!currenStatus) {
                                        plugin.getPlayerStaffChatMute().put(uuid, true);
                                        playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §aStaff Chat is Muted!"));
                                    } else {
                                        plugin.getPlayerStaffChatMute().put(uuid, false);
                                        playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §aStaff Chat is Unmuted!"));
                                    }
                                } else {
                                    String message = ctx.getInput().substring(ctx.getInput().indexOf(" ") + 1);
                                    String server = playerSource.getCurrentServer().get().getServerInfo().getName();
                                    User user = plugin.getLuckPerms().getUserManager().getUser(playerSource.getUniqueId());
                                    assert user != null;
                                    CachedMetaData metaData = user.getCachedData().getMetaData();
                                    String prefix = Objects.requireNonNull(metaData.getPrefix()).replaceAll("&", "§");
                                    plugin.broadcastStaff("§7[§cStaffChat§7] [§6" + playerSource.getCurrentServer().get().getServerInfo().getName().toUpperCase() + "§7] " + prefix + " " + playerSource.getUsername() + " : §6" + message);
                                    plugin.getStaffChannel().sendDiscordChat(playerSource.getUsername(), plugin.getConfigManager().getString("discord.staff-channel-prefix", null)
                                            .replaceAll("%server%", server.toUpperCase())
                                            .replaceAll("%player%", playerSource.getUsername())
                                            .replaceAll("%prefix%", prefix.replaceAll("§.", "")) +
                                            message.replaceFirst("!", " : ")
                                                    .replaceAll("§.", ""));
                                }

                            } else if (source instanceof ConsoleCommandSource) {
                                plugin.broadcastStaff("§7[§cStaffChat§7] " + "§7[Console§7]" + " : §6" + subCommand);
                                System.out.println("§7[§cStaffChat§7] " + "§7[Console§7]" + " : §6" + subCommand);
                                plugin.getStaffChannel().sendDiscordChat("Console", subCommand.replaceAll("§.", ""));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
