package me.kenvera.velocore.managers;

import com.velocitypowered.api.proxy.Player;
import me.kenvera.velocore.VeloCore;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisConnection {
    private final VeloCore plugin;
    private JedisPool jedispool;
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
//        this.jedispool = new JedisPool(jedisPoolConfig, host, port, 5000, password, 0);

        try {
            JedisPool newPool = new JedisPool(jedisPoolConfig, new URI("redis://:51535968db86376b607c8a947149ce51376189ab09f63656f17b938a6db335f5@100.126.17.140:6379"));
            JedisPool oldPool = jedispool;
            jedispool = newPool;
            if (oldPool != null && !oldPool.isClosed())
                oldPool.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        subscribe();
    }

    public void subscribe()
    {
        executorService.execute(() -> {

            //subscribe to jedisPool
            try (Jedis jedis = jedispool.getResource()) {
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        shakeIncomingMessage(message);
                    }
                }, "globalMessage");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void publish(String message)
    {
        executorService.execute(() -> {
            try (Jedis jedis = jedispool.getResource()) {
                jedis.publish("globalMessage", message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public void publish(String channel, String message)
    {
        executorService.execute(() -> {
            try (Jedis jedis = jedispool.getResource()) {
                jedis.publish(channel, message);
            }
        });
    }

    public JedisPool getJedis()
    {
        return jedispool;
    }

    public void shakeIncomingMessage(String message){
        executorService.execute(() -> {
            if (message.startsWith("globalmessage:")){
                receiveGlobalChat(message);
                System.out.println("received incoming message");
            }
        });
    }

    public void receiveGlobalChat(String message) {
        message = message.replace("globalmessage:", "");
        String[] split = message.split(">");

        for (Player player : plugin.getProxy().getAllPlayers()) {
            String server = player.getCurrentServer().get().getServerInfo().getName();
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(("§7[§cGLOBAL§7] [§b" + server.toUpperCase() + "§7] " + message)));
            System.out.println("§7[§cGLOBAL§7] [§b" + server.toUpperCase() + "§7] " + message);
        }
    }

    public void close() {
        executorService.shutdownNow(); // Shut down the ExecutorService
        jedispool.close();
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
