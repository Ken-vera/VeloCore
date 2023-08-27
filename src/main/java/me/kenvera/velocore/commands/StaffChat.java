package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

import java.util.Map;
import java.util.UUID;

public class StaffChat implements SimpleCommand {
    private final ProxyServer proxy;
    private final Map<UUID, Boolean> playerStaffChat;
    public StaffChat(ProxyServer proxy, Map<UUID, Boolean> playerStaffChat) {
        this.proxy = proxy;
        this.playerStaffChat = playerStaffChat;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (source instanceof Player player) {
            UUID uuid = player.getUniqueId();

            if (args.length < 1) {
                source.sendMessage(Component.text("§cUsage: /staffchat toggle"));
                return;
            }

            if (args[0].equals("toggle")) {
                if (source.hasPermission("velocity.staff")) {
                    if (playerStaffChat.get(uuid) == null) {
                        playerStaffChat.put(uuid, true);
                        player.sendMessage(Component.text("Staff chat enabled!"));
                    } else {
                        playerStaffChat.clear();
                        player.sendMessage(Component.text("Staff chat disabled!"));
                    }
                }
            }
        }
    }
}
