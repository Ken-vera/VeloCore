package me.kenvera.velocore.donation;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;

public class DonationAnnouncement implements SimpleCommand {
    private ProxyServer proxy;
    private VeloCore veloCore;

    public DonationAnnouncement(ProxyServer proxy) {
        this.proxy = proxy;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof ConsoleCommandSource)) {
            source.sendMessage(Component.text("§cThis command is only available for the console."));
            return;
        }

        if (args.length < 3) {
            source.sendMessage(Component.text("§cUsage: /donationbroadcast <player> <amount> <item>"));
            return;
        }

        String playerName = args[0];
        String amount = args[1];
        StringBuilder itemBuilder = new StringBuilder();

        // Concatenate all arguments after index 2
        for (int i = 2; i < args.length; i++) {
            itemBuilder.append(args[i]);
            if (i < args.length - 1) {
                itemBuilder.append(" ");
            }
        }

        String item = itemBuilder.toString();
        item = item.replace("&", "§");
        amount = amount.replace("&", "§");

        for (Player player : proxy.getAllPlayers()) {
            if (player.isActive()) {
                player.sendMessage(Component.text("§e§lPhoenix Donation » §e" + playerName + " §fbaru saja membeli " + amount + " ").append(Component.text(item)));
            }
        }
    }
}
