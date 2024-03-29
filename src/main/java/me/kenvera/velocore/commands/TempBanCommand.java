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

public final class TempBanCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("tempban")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
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
                        .then(RequiredArgumentBuilder.<CommandSource, String>argument("duration", StringArgumentType.string())
                                .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            CommandSource source = ctx.getSource();
                                            String playerArg = StringArgumentType.getString(ctx, "player");
                                            String reason = StringArgumentType.getString(ctx, "reason");
                                            String duration = StringArgumentType.getString(ctx, "duration");
                                            long royalty = 1196793581751521390L;
                                            long to = 919806897631158292L;

                                            long expire = System.currentTimeMillis() + getBanDuration(duration);
                                            Optional<Player> targetPlayer = plugin.getProxy().getPlayer(playerArg);
                                            if (targetPlayer.isPresent()) {
                                                Player player = targetPlayer.get();

                                                if (source instanceof Player playerSource) {
                                                    if (player.getPermissionValue("velocity.ban.prevent") != Tristate.TRUE) {
                                                        plugin.getBanManager().addBan(player.getUniqueId().toString(), player.getUsername(), playerSource.getUsername(), reason, expire);
                                                        player.disconnect(Utils.formatBannedMessage(plugin, playerSource.getUsername(), reason, expire, plugin.getBanManager().getID(player.getUniqueId().toString())));
                                                        plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                            playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                            ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                            if (playerProtocol != null) {
                                                                playerProtocol.playSound(Sound.ITEM_GOAT_HORN_SOUND_7, SoundCategory.AMBIENT, 1f, 1f);
                                                            }
                                                        });

                                                        plugin.broadcastStaff("§7" + player.getUsername() + " §chas been temporarily banned by §7" + playerSource.getUsername());
                                                        plugin.broadcastStaff("§cDuration: §7" + duration);
                                                        plugin.broadcastStaff("§cReason: §7" + reason);
                                                        plugin.broadcastStaff("");

                                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + player.getUsername() + " §chas been temporarily banned by §7" + playerSource.getUsername()));
                                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cDuration: §7" + duration));
                                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cReason: §7" + reason));
                                                        plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                        plugin.getDiscordLogger().logEmbedBan(String.valueOf(plugin.getBanManager().getID(player.getUniqueId().toString())), player.getUsername(), player.getUniqueId().toString(), player.getRemoteAddress().getHostName(), player.getClientBrand(), "Temporary", duration, Utils.parseDateTime(expire, true), ((Player) source).getUsername(), reason, royalty, to);
                                                    } else {
                                                        playerSource.sendMessage(Component.text("§cThis player is immuned to ban!"));
                                                    }

                                                } else if (source instanceof ConsoleCommandSource) {
                                                    plugin.getBanManager().addBan(player.getUniqueId().toString(), player.getUsername(), "Console", reason, expire);
                                                    player.disconnect(Utils.formatBannedMessage(plugin, "Console", reason, expire, plugin.getBanManager().getID(player.getUniqueId().toString())));
                                                    plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                        playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                        ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                        if (playerProtocol != null) {
                                                            playerProtocol.playSound(Sound.ITEM_GOAT_HORN_SOUND_7, SoundCategory.AMBIENT, 1f, 1f);
                                                        }
                                                    });

                                                    plugin.broadcastStaff("§7" + player.getUsername() + " §chas been temporarily banned by §7Console");
                                                    plugin.broadcastStaff("§cDuration: §7" + duration);
                                                    plugin.broadcastStaff("§cReason: §7" + reason);
                                                    plugin.broadcastStaff("");

                                                    source.sendMessage(Component.text(""));
                                                    source.sendMessage(Component.text("§7" + player.getUsername() + " §chas been temporarily banned by §7Console"));
                                                    source.sendMessage(Component.text("§cDuration: §7" + duration));
                                                    source.sendMessage(Component.text("§cReason: §7" + reason));
                                                    source.sendMessage(Component.text(""));
                                                    plugin.getDiscordLogger().logEmbedBan(String.valueOf(plugin.getBanManager().getID(player.getUniqueId().toString())), player.getUsername(), player.getUniqueId().toString(), player.getRemoteAddress().getHostName(), player.getClientBrand(), "Temporary", duration, Utils.parseDateTime(expire, true), "Console", reason, royalty, to);
                                                }

                                            } else {
                                                String uuid = plugin.getBanManager().getUUID(playerArg);
                                                String playerName = plugin.getBanManager().getUsername(uuid);

                                                if (source instanceof Player playerSource) {
                                                    if (uuid != null) {
                                                        Ban currentBan = plugin.getBanManager().getBan(uuid);
                                                        if (currentBan != null) {
                                                            if (currentBan.getExpire() != -1) {
                                                                playerSource.sendMessage(Component.text(""));
                                                                playerSource.sendMessage(Component.text("§7" + playerName + " §chas been temporarily banned until §7" + Utils.parseDateTime(currentBan.getExpire(), true) + " §cby §7" + currentBan.getIssuer()));
                                                                playerSource.sendMessage(Component.text("§cReason: " + currentBan.getReason()));
                                                                playerSource.sendMessage(Component.text(""));
                                                            } else {
                                                                playerSource.sendMessage(Component.text(""));
                                                                playerSource.sendMessage(Component.text("§7" + playerName + " §chas been permanently banned on §7" + Utils.parseDateTime(currentBan.getBannedTime(), true) + " §cby §7" + currentBan.getIssuer()));
                                                                playerSource.sendMessage(Component.text("§cReason: " + currentBan.getReason()));
                                                                playerSource.sendMessage(Component.text(""));
                                                            }
                                                        } else {
                                                            plugin.getBanManager().addBan(uuid, playerName, playerSource.getUsername(), reason, expire);
                                                            plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                                playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                                ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                                if (playerProtocol != null) {
                                                                    playerProtocol.playSound(Sound.ITEM_GOAT_HORN_SOUND_7, SoundCategory.AMBIENT, 1f, 1f);
                                                                }
                                                            });

                                                            plugin.broadcastStaff("§7" + playerName + " §chas been temporarily banned by §7" + playerSource.getUsername());
                                                            plugin.broadcastStaff("§cDuration: §7" + duration);
                                                            plugin.broadcastStaff("§cReason: §7" + reason);
                                                            plugin.broadcastStaff("");

                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + playerName + " §chas been temporarily banned by §7" + playerSource.getUsername()));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cDuration: §7" + duration));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cReason: §7" + reason));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                            plugin.getDiscordLogger().logEmbedBan(String.valueOf(plugin.getBanManager().getID(uuid)), playerName, uuid, "Offline", "n/a", "Temporary", duration, Utils.parseDateTime(expire, true), ((Player) source).getUsername(), reason, royalty, to);
                                                        }
                                                    } else {
                                                        source.sendMessage(Component.text("§7" + playerArg + " §cplayer data can't be found within database!"));
                                                    }

                                                } else if (source instanceof ConsoleCommandSource) {
                                                    if (uuid != null) {
                                                        Ban currentBan = plugin.getBanManager().getBan(uuid);
                                                        if (currentBan != null) {
                                                            if (currentBan.getExpire() != -1) {
                                                                source.sendMessage(Component.text(""));
                                                                source.sendMessage(Component.text("§7" + playerName + " §chas been temporarily banned until §7" + Utils.parseDateTime(currentBan.getExpire(), true) + " §cby §7" + currentBan.getIssuer()));
                                                                source.sendMessage(Component.text("§cReason: " + currentBan.getReason()));
                                                                source.sendMessage(Component.text(""));
                                                            } else {
                                                                source.sendMessage(Component.text(""));
                                                                source.sendMessage(Component.text("§7" + playerName + " §chas been permanently banned on §7" + Utils.parseDateTime(currentBan.getBannedTime(), true) + " §cby §7" + currentBan.getIssuer()));
                                                                source.sendMessage(Component.text("§cReason: " + currentBan.getReason()));
                                                                source.sendMessage(Component.text(""));
                                                            }
                                                        } else {
                                                            plugin.getBanManager().addBan(uuid, playerName, "Console", reason, expire);
                                                            plugin.getProxy().getAllPlayers().forEach(playerOnline -> {
                                                                playerOnline.sendMessage(Utils.formatBanMessage(plugin));
                                                                ProtocolizePlayer playerProtocol = Protocolize.playerProvider().player(playerOnline.getUniqueId());
                                                                if (playerProtocol != null) {
                                                                    playerProtocol.playSound(Sound.ITEM_GOAT_HORN_SOUND_7, SoundCategory.AMBIENT, 1f, 1f);
                                                                }
                                                            });

                                                            plugin.broadcastStaff("§7" + playerName + " §chas been temporarily banned by §7Console");
                                                            plugin.broadcastStaff("§cDuration: §7" + duration);
                                                            plugin.broadcastStaff("§cReason: §7" + reason);
                                                            plugin.broadcastStaff("");

                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§7" + playerName + " §chas been temporarily banned by §7Console"));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cDuration: §7" + duration));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text("§cReason: §7" + reason));
                                                            plugin.getProxy().getConsoleCommandSource().sendMessage(Component.text(""));
                                                            plugin.getDiscordLogger().logEmbedBan(String.valueOf(plugin.getBanManager().getID(uuid)), playerName, uuid, "Offline", "n/a", "Temporary", duration, Utils.parseDateTime(expire, true), "Console", reason, royalty, to);
                                                        }
                                                    } else {
                                                        source.sendMessage(Component.text("§7" + playerArg + " §cplayer data can't be found within database!"));
                                                    }
                                                }
                                            }
                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                        )
                ).build();
        return new BrigadierCommand(node);
    }

    public static long getBanDuration(String durationString) {
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
