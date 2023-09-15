package me.kenvera.velocore.commands;

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

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ReloadCommand {
    private final VeloCore plugin;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    public ReloadCommand(VeloCore plugin) {
        this.plugin = plugin;
    }
    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("velocore")
                .requires(src -> src.getPermissionValue("velocity.staff.reload") != Tristate.UNDEFINED)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String inputPart = ctx.getInput().toLowerCase();
                            String[] inputParts = inputPart.split(" ");
                            java.util.List<String> suggestions = new ArrayList<>();

                            suggestions.add("reload");

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

                            if (subCommand.equalsIgnoreCase("reloadplugin")) {
                                executorService.submit(() -> {
                                    reloadPlugin(playerSource);
                                });
                            }
                            return Command.SINGLE_SUCCESS;
                        }))
                .build();
        return new BrigadierCommand(node);
    }

    private void reloadPlugin(Player playerSource) {
        // Unload resources, save data, stop tasks, etc.
        // Perform the plugin reload logic here

        // Example: Reloading the plugin after a 5-second delay
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Execute a proxy console command to reload the plugin
        String consoleCommand = "vsu reloadplugin VeloCore"; // Replace with the actual command
        plugin.getProxy().getCommandManager().executeAsync(plugin.getProxy().getConsoleCommandSource(), consoleCommand);
    }
}

