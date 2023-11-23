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
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class ReloadCommand {
    private final VeloCore plugin;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final CountDownLatch redisShutdownLatch = new CountDownLatch(1);
    public ReloadCommand(VeloCore plugin) {
        this.plugin = plugin;
    }
    public BrigadierCommand createBrigadierCommand() {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
                .<CommandSource>literal("velocore")
                .requires(src -> src.getPermissionValue("velocity.staff.reload") != Tristate.UNDEFINED)
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("subcommand", StringArgumentType.word())
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
        executorService.shutdown();
        plugin.getRedisConnection().close();

        // Unload resources, save data, stop tasks, etc.
        // Perform the plugin reload logic here

//        // Example: Reloading the plugin after a 5-second delay
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        try {
            executorService.awaitTermination(30, TimeUnit.SECONDS);
            String consoleCommand = "vsu reloadplugin velocore"; // Replace with the actual command
            plugin.getProxy().getCommandManager().executeAsync(plugin.getProxy().getConsoleCommandSource(), consoleCommand);
        } catch (InterruptedException e) {
            e.printStackTrace();
            // Handle the exception if necessary
        }
//        try {
//            // Wait for the Redis connection to be fully closed (up to a specified timeout)
//            if (!redisShutdownLatch.await(10, TimeUnit.SECONDS)) {
//                playerSource.sendMessage(Component.text("Redis connection did not close in time."));
//                return;
//            }
//
//            // Now, you can proceed with the actual reload mechanism
//            // ...
//
//            playerSource.sendMessage(Component.text("Reload complete."));
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            playerSource.sendMessage(Component.text("Reload interrupted."));
//        }

        // Execute a proxy console command to reload the plugin
    }
}

