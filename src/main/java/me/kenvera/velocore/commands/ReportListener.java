package me.kenvera.velocore.commands;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class ReportListener implements SimpleCommand {
    private final ProxyServer proxy;

    public ReportListener(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length < 2) {
            source.sendMessage(Component.text("§cUsage: /report <player> <reason>"));
            return;
        }

        String reportedPlayer = args[0];
        String reason = String.join(" ", args).substring(reportedPlayer.length() + 1);

        if (!(source instanceof Player reporter)) {
            source.sendMessage(Component.text("§cThis command can only be run by players."));
            return;
        }

        List<Player> notifyPlayers = proxy.getAllPlayers().stream()
                .filter(player -> player.hasPermission("velocity.report.notify") && player != reporter)
                .toList();

        if (notifyPlayers.isEmpty()) {
            source.sendMessage(Component.text("§cNo staff members available to notify."));
            return;
        }

        TextComponent message = Component.text()
                .append(Component.text("§c[Report] "))
                .append(Component.text(reporter.getUsername() + " reported " + reportedPlayer + " for: ")
                        .color(NamedTextColor.GRAY))
                .append(Component.text(reason)
                        .color(NamedTextColor.WHITE))
                .build();

        for (Player notifyPlayer : notifyPlayers) {
            notifyPlayer.sendMessage(message);
        }

        source.sendMessage(Component.text("§aReport submitted successfully."));
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] arguments = invocation.arguments();
        if (arguments.length == 1) {
            return proxy.getAllPlayers().stream()
                    .filter(player -> !player.equals(invocation.source()))
                    .filter(player -> !player.hasPermission("velocity.staff"))
                    .map(Player::getUsername)
                    .filter(name -> name.toLowerCase().startsWith(arguments[0].toLowerCase()))
                    .toList();
        }
        return proxy.getAllPlayers().stream()
                .filter(player -> !player.equals(invocation.source()))
                .filter(player -> !player.hasPermission("velocity.staff"))
                .map(Player::getUsername)
                .toList();
    }
}
