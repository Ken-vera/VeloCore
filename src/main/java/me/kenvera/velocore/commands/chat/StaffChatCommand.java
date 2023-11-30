package me.kenvera.velocore.commands.chat;

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

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class StaffChatCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("staffchat")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
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
                            Player playerSource = (Player) source;
                            UUID uuid = playerSource.getUniqueId();
                            String subCommand = StringArgumentType.getString(ctx, "subcommand");

                            if (subCommand.equalsIgnoreCase("toggle")) {
                                boolean currentStatus = plugin.getPlayerStaffChat().getOrDefault(uuid, false);

                                if (!currentStatus) {
                                    plugin.getPlayerStaffChat().put(uuid, true);
                                    playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §aStaff Chat is Enabled!"));
                                } else {
                                    plugin.getPlayerStaffChat().put(uuid, false);
                                    playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §cStaff Chat is Disabled!"));
                                }
                            }

                            if (subCommand.equalsIgnoreCase("mute")) {
                                boolean currenStatus = plugin.getPlayerStaffChatMute().getOrDefault(uuid, false);

                                if (!currenStatus) {
                                    plugin.getPlayerStaffChatMute().put(uuid, true);
                                    playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §aStaff Chat is Muted!"));
                                } else {
                                    plugin.getPlayerStaffChatMute().put(uuid, false);
                                    playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §aStaff Chat is Unmuted!"));
                                }
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}
