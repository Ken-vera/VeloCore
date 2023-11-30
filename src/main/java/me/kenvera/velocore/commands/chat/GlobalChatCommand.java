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

public final class GlobalChatCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("globalchat")
                .requires(src -> src.getPermissionValue("velocity.globalchat.use") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String message = StringArgumentType.getString(ctx, "message");
                            CommandSource source = ctx.getSource();
                            Player playerSource = (Player) source;

                            if (!message.isEmpty()) {
                                if (plugin.getCooldown("globalchat", playerSource.getUniqueId()) == null || playerSource.hasPermission("velocity.globalchat.bypass")) {
                                    String server = plugin.getProxy().getPlayer(playerSource.getUsername()).flatMap(Player::getCurrentServer).get().getServerInfo().getName();
                                    String formattedMessage = plugin.getConfigManager().getString("global-chat.prefix", null)
                                            .replaceAll("%server%", server)
                                            .replaceAll("%player%", playerSource.getUsername())
                                            .replaceAll("%message%", message.replaceAll("&", "§"));

                                    plugin.getProxy().getAllPlayers().forEach(player -> player.sendMessage(Component.text(formattedMessage)));
                                    plugin.setCooldown("globalchat", 10, playerSource.getUniqueId());
                                } else {
                                    playerSource.sendMessage(Component.text("§cYou can't use global chat that frequent!"));
                                }
                            } else {
                                playerSource.sendMessage(Component.text("§6Usage : /globalchat <message>"));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}

//        connection.getJedis();
//        connection.publish("globalmessage:" + player.getCurrentServer().get().getServerInfo().getName().toUpperCase() + ":" + player.getGameProfile().getName() + ":" + message);