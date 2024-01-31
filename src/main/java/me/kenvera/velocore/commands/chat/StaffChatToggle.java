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

public final class StaffChatToggle {
    public static BrigadierCommand createBrigadierCommand(final VeloCore plugin) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("sct")
                .requires(src -> src.getPermissionValue("velocity.staff") == Tristate.TRUE)
                .executes(ctx -> {
                    if (ctx.getSource() instanceof Player playerSource) {
                        UUID uuid = playerSource.getUniqueId();
                        boolean currentStatus = plugin.getPlayerStaffChat().getOrDefault(uuid, false);

                        if (!currentStatus) {
                            plugin.getPlayerStaffChat().put(uuid, true);
                            playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §aStaff Chat is Enabled!"));
                        } else {
                            plugin.getPlayerStaffChat().put(uuid, false);
                            playerSource.sendMessage(Component.text("§7[§cStaffChat§7] §cStaff Chat is Disabled!"));
                        }
                    }
                    return Command.SINGLE_SUCCESS;
                })

                .build();
        return new BrigadierCommand(node);
    }
}
