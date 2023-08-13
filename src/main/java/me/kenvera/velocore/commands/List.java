package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;

public class List implements SimpleCommand {
    private final ProxyServer proxy;

    public List(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        if (!source.hasPermission("velocity.list")) {
            source.sendMessage(Component.text("§cYou don't have permission to run this command!"));
            return;
        }

        java.util.List<Player> onlineStaff = proxy.getAllPlayers().stream()
                .filter(player -> player.hasPermission("velocity.list"))
                .toList();

        source.sendMessage(Component.text(""));
        source.sendMessage(Component.text("§aThere are ")
                .append(Component.text(onlineStaff.size()))
                .append(Component.text(" §aStaff[s] online:")));
        for (Player staffMember : onlineStaff) {
            source.sendMessage(Component.text(staffMember.getUsername() )
                    .append(Component.text(" §7- "))
                    .append(Component.text("§7" + staffMember.getCurrentServer().get().getServerInfo().getName() ))
                    .append(Component.text(" ")));
        }
        source.sendMessage(Component.text(""));
    }
}
