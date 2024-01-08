package me.kenvera.velocore.listeners;

import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.database.RedisManager;
import redis.clients.jedis.JedisPubSub;

import java.sql.SQLException;

public class RedisListener extends JedisPubSub {
    private final VeloCore plugin;
    private final String channelName;
    public RedisListener(VeloCore plugin, RedisManager redisManager, String channelName) {
        this.plugin = plugin;
        this.channelName = channelName;
    }

    @Override
    public void onMessage(String channel, String message) {
        if (channel.equals(channelName)) {
            String[] messageParts = message.split("_");
            if (messageParts.length == 4) {
                String messageType = messageParts[0];
                String uuid = messageParts[1];
                String group = messageParts[2];
                String target = messageParts[3];
                if (target.equalsIgnoreCase("proxy")) {
                    plugin.getLogger().info("§7Received message on channel " + channel + ":" + message);

                    switch (messageType) {
                        case "set" -> {
                            plugin.getLogger().info("[Group] Received group command (set) for " + uuid + " from backend!");
                            try {
                                plugin.getPlayerData().setGroup(uuid, group);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        case "reset" -> {
                            plugin.getLogger().info("[Group] Received group command (reset) for " + uuid + " from backend!");
                            try {
                                plugin.getPlayerData().setGroup(uuid, "default");
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        case "add" -> {
                            plugin.getLogger().info("[Group] Received group command (add) for " + uuid + " from backend!");
                            try {
                                plugin.getPlayerData().addGroup(uuid, group);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                        case "remove" -> {
                            plugin.getLogger().info("[Group] Received group command (remove) for " + uuid + " from backend!");
                            try {
                                plugin.getPlayerData().removeGroup(uuid, group);
                            } catch (SQLException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
    }


//    public void receiveShopCommand(String message) {
//        String[] messageParts = message.split("_");
//        String uuid = messageParts[1];
//        String group = messageParts[2];
//
//        if (group.equalsIgnoreCase("elite") ||
//                group.equalsIgnoreCase("master") ||
//                group.equalsIgnoreCase("mythic") ||
//                group.equalsIgnoreCase("conqueror") ||
//                group.equalsIgnoreCase("revenant")) {
//
//            plugin.getRedis().publish("chronosync", "set_" + uuid + "_" + group);
//        }
//    }
}
