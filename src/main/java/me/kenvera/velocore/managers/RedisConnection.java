package me.kenvera.velocore.managers;

import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisConnection {
    private final VeloCore plugin;
    private JedisPool jedispool;
    private JedisPubSub jedisPubSub;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public RedisConnection(VeloCore plugin, String host, int port, String password) {
        this.plugin = plugin;
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(100);
        jedisPoolConfig.setMaxIdle(2);
        jedisPoolConfig.setMinIdle(1);
        jedisPoolConfig.setTestOnBorrow(true);
        jedisPoolConfig.setTestOnReturn(true);
        jedisPoolConfig.setTestWhileIdle(true);
        jedisPoolConfig.setNumTestsPerEvictionRun(-1);
        jedisPoolConfig.setBlockWhenExhausted(false);
        this.jedispool = new JedisPool(jedisPoolConfig, host, port, 5000, password, 0);

        subscribe();
    }

    public void subscribe() {
        executorService.execute(() -> {

            //subscribe to jedisPool
            try (Jedis jedis = jedispool.getResource()) {
                jedisPubSub = new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        shakeIncomingMessage(message);
                    }
                };
                jedis.subscribe(jedisPubSub, "globalMessage");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void unsubscribe() {
        if (jedisPubSub != null) {
            jedisPubSub.unsubscribe();
            jedisPubSub = null;
        }
    }

    public void publish(String message) {
        executorService.execute(() -> {
            try (Jedis jedis = jedispool.getResource()) {
                jedis.publish("globalMessage", message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void publish(String channel, String message) {
        executorService.execute(() -> {
            try (Jedis jedis = jedispool.getResource()) {
                jedis.publish(channel, message);

            }
        });
    }

    public JedisPool getJedis() {
        return jedispool;
    }

    public void shakeIncomingMessage(String message) {
        executorService.execute(() -> {
            try {
                if (message.startsWith("globalmessage:")) {
                    receiveGlobalChat(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void receiveGlobalChat(String message) {
        String prefix = "§7[§cGLOBAL§7] ";
        String[] rawMessage = message.split(":");
        String server = "§7[§b" + rawMessage[1] + "§7] ";
        String playerName = "§7" + rawMessage[2] + " §7: ";
        message = rawMessage[3];
        String[] split = message.split(">");

        for (Player player : plugin.getProxy().getAllPlayers()) {
            player.sendMessage(Component.text(prefix + server + playerName + message));
//            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(("§7[§cGLOBAL§7] [§b" + server.toUpperCase() + "§7] " + message)));
        }
        System.out.println(prefix + server + playerName + message);
        message = "[GLOBAL] " + server.replaceAll("§.", "") + playerName.replaceAll("§.", "") + rawMessage[3];
        plugin.getDiscordChannel().sendDiscordChat("1154435789657215078", message);
    }

    public void close() {
        unsubscribe();
//        jedispool.close();
        executorService.shutdownNow(); // Shut down the ExecutorService
    }


    public int getNumActiveConnections() {
        return jedispool.getNumActive();
    }

    public int getNumIdleConnections() {
        return jedispool.getNumIdle();
    }

    public int getMaxTotalConnections() {
        return jedispool.getMaxTotal();
    }
}
