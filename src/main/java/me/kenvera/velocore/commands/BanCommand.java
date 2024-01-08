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
import dev.simplix.protocolize.api.Protocolize;
import dev.simplix.protocolize.api.SoundCategory;
import dev.simplix.protocolize.api.player.ProtocolizePlayer;
import dev.simplix.protocolize.data.Sound;
import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.managers.Ban;
import me.kenvera.velocore.managers.Utils;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.Optional;

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
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    CommandSource source = ctx.getSource();
                                    String playerArg = StringArgumentType.getString(ctx, "player");
                                    String reason = StringArgumentType.getString(ctx, "reason");

                                    Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);
                                    if (targetPlayer.isPresent()) {
                                        Player player = targetPlayer.get();

                                        if (source instanceof Player playerSource) {
                                            if (player.getPermissionValue("velocity.ban.prevent") != Tristate.TRUE) {
                                                String UUID = plugin.getBanManager().getUUID(playerArg);
                                                Ban ban = plugin.getBanManager().getBan(UUID);
                                                if (ban != null) {
                                                    playerSource.sendMessage(Component.text("§7" + playerArg + " §calready have an active punishment!"));
                                                } else {
                                                    plugin.getBanManager().addBan(player.getUniqueId().toString(), player.getUsername(), playerSource.getUsername(), reason, -1);
                                                    player.disconnect(Utils.formatBannedMessage(plugin, playerSource.getUsername(), reason, -1, plugin.getBanManager().getID(player.getUniqueId().toString())));
                                                    plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                        playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                        ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                        if (playerProtocol != null) {
                                                            playerProtocol.playSound(Sound.BLOCK_ANVIL_PLACE, SoundCategory.AMBIENT, 1f, 1f);
                                                        }
                                                    });
                                                    plugin.getProxy().getAllPlayers().stream().filter(player1 -> player1.hasPermission("velocity.staff")).forEach(playerOnline -> {
                                                        playerOnline.sendMessage(Component.text("§7" + player.getUsername() + " §chas been permanently banned by §7" + playerSource.getUsername()));
                                                        playerOnline.sendMessage(Component.text("§cReason: §7" + reason));
                                                        playerOnline.sendMessage(Component.text(""));
                                                    });
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + player.getUsername() + " §chas been permanently banned by §7" + playerSource.getUsername()));
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cReason: §7" + reason));
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                }

                                            }

                                        } else if (source instanceof ConsoleCommandSource) {
                                            String UUID = plugin.getBanManager().getUUID(playerArg);
                                            Ban ban = plugin.getBanManager().getBan(UUID);
                                            if (ban != null) {
                                                source.sendMessage(Component.text("§7" + playerArg + " §calready have an active punishment!"));
                                            } else {
                                                plugin.getBanManager().addBan(player.getUniqueId().toString(), player.getUsername(), "Console", reason, -1);
                                                player.disconnect(Utils.formatBannedMessage(plugin, "Console", reason, -1, plugin.getBanManager().getID(player.getUniqueId().toString())));
                                                plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                    playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                    ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                    if (playerProtocol != null) {
                                                        playerProtocol.playSound(Sound.BLOCK_ANVIL_PLACE, SoundCategory.AMBIENT, 1f, 1f);
                                                    }
                                                });
                                                plugin.getProxy().getAllPlayers().stream().filter(player1 -> player1.hasPermission("velocity.staff")).forEach(playerOnline -> {
                                                    playerOnline.sendMessage(Component.text("§7" + player.getUsername() + " §chas been permanently banned by §7Console"));
                                                    playerOnline.sendMessage(Component.text("§cReason: §7" + reason));
                                                    playerOnline.sendMessage(Component.text(""));
                                                });
                                                source.sendMessage(Component.text(""));
                                                source.sendMessage(Component.text("§7" + player.getUsername() + " §chas been permanently banned by §7Console"));
                                                source.sendMessage(Component.text("§cReason: §7" + reason));
                                                source.sendMessage(Component.text(""));
                                            }
                                        }
                                    } else {
                                        if (source instanceof Player playerSource) {
                                            String uuid = plugin.getBanManager().getUUID(playerArg);
                                            String playerName = plugin.getBanManager().getUsername(uuid);
                                            if (uuid != null) {

                                                Ban currentBan = plugin.getBanManager().getBan(uuid);
                                                if (currentBan != null) {

                                                    long bannedTime = plugin.getBanManager().getBan(uuid).getBannedTime();
                                                    playerSource.sendMessage(Component.text("§7" + playerName + " §chas been permanently banned on §7" + Utils.parseDateTime(bannedTime, true)));

                                                } else {
                                                    plugin.getBanManager().addBan(uuid, playerName, playerSource.getUsername(), reason, -1);
                                                    plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                        playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                        ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                        if (playerProtocol != null) {
                                                            playerProtocol.playSound(Sound.BLOCK_ANVIL_PLACE, SoundCategory.AMBIENT, 1f, 1f);
                                                        }
                                                    });
                                                    plugin.getProxy().getAllPlayers().stream().filter(player1 -> player1.hasPermission("velocity.staff")).forEach(playerOnline -> {
                                                        playerOnline.sendMessage(Component.text("§7" + playerName + " §chas been permanently banned by §7" + playerSource.getUsername()));
                                                        playerOnline.sendMessage(Component.text("§cReason: §7" + reason));
                                                        playerOnline.sendMessage(Component.text(""));
                                                    });
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + playerName + " §chas been permanently banned by §7" + playerSource.getUsername()));
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cReason: §7" + reason));
                                                    plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                }
                                            } else {
                                                playerSource.sendMessage(Component.text("§7" + playerArg + " §cplayer data can't be found within database!"));
                                            }

                                        } else if (source instanceof ConsoleCommandSource) {
                                            String uuid = plugin.getBanManager().getUUID(playerArg);
                                            String playerName = plugin.getBanManager().getUsername(uuid);
                                            if (uuid != null) {

                                                Ban currentBan = plugin.getBanManager().getBan(uuid);
                                                if (currentBan != null) {
                                                    long bannedTime = plugin.getBanManager().getBan(uuid).getBannedTime();
                                                    source.sendMessage(Component.text("§7" + playerName + " §chas been permanently banned on §7" + Utils.parseDateTime(bannedTime, true)));
                                                } else {
                                                    plugin.getBanManager().addBan(uuid, playerName, "Console", reason, -1);
                                                    plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                        playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                        ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                        if (playerProtocol != null) {
                                                            playerProtocol.playSound(Sound.BLOCK_ANVIL_PLACE, SoundCategory.AMBIENT, 1f, 1f);
                                                        }
                                                    });
                                                    plugin.getProxy().getAllPlayers().stream().filter(player1 -> player1.hasPermission("velocity.staff")).forEach(playerOnline -> {
                                                        playerOnline.sendMessage(Component.text("§7" + playerName + " §chas been permanently banned by §7Console"));
                                                        playerOnline.sendMessage(Component.text("§cReason: §7" + reason));
                                                        playerOnline.sendMessage(Component.text(""));
                                                    });
                                                    source.sendMessage(Component.text(""));
                                                    source.sendMessage(Component.text("§7" + playerName + " §chas been permanently banned by §7Console"));
                                                    source.sendMessage(Component.text("§cReason: §7" + reason));
                                                    source.sendMessage(Component.text(""));
                                                }
                                            } else {
                                                source.sendMessage(Component.text("§7" + playerArg + " §cplayer data can't be found within database!"));
                                            }
                                        }
                                    }
                                    return Command.SINGLE_SUCCESS;
                                }))
                ).build();
        return new BrigadierCommand(node);
    }
}
