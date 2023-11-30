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

public final class DonatorChatCommand {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("donatorchat")
                .requires(src -> src.getPermissionValue("velocity.donatorchat.use") == Tristate.TRUE)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            String message = StringArgumentType.getString(ctx, "message");
                            CommandSource source = ctx.getSource();
                            Player playerSource = (Player) source;

                            if (!message.isEmpty()) {
                                if (plugin.getCooldown("donatorchat", playerSource.getUniqueId()) == null || playerSource.hasPermission("velocity.donatorchat.bypass")) {
                                    String server = plugin.getProxy().getPlayer(playerSource.getUsername()).flatMap(Player::getCurrentServer).get().getServerInfo().getName();
                                    String formattedMessage = plugin.getConfigManager().getString("donator-chat.prefix", null)
                                            .replaceAll("%server%", server)
                                            .replaceAll("%player%", playerSource.getUsername())
                                            .replaceAll("%message%", message.replaceAll("&", "§"));

                                    plugin.getProxy().getAllPlayers().stream().filter(player -> player.hasPermission("velocity.donatorchat.see")).forEach(player -> player.sendMessage(Component.text(formattedMessage)));
                                    plugin.setCooldown("donatorchat", 10, playerSource.getUniqueId());
                                } else {
                                    playerSource.sendMessage(Component.text("§cYou can't use donator chat that frequent!"));
                                }
                            } else {
                                playerSource.sendMessage(Component.text("§6Usage : /donatorchat <message>"));
                            }

                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }
}

//        connection.getJedis();
//        connection.publish("globalmessage:" + player.getCurrentServer().get().getServerInfo().getName().toUpperCase() + ":" + player.getGameProfile().getName() + ":" + message);