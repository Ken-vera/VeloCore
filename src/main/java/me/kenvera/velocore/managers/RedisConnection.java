package me.kenvera.velocore.managers;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisConnection {
    private ProxyServer proxy;
    private final JedisPool jedispool;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public RedisConnection(ProxyServer proxy, String host, int port, String password) {
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
        this.proxy = proxy;
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
            }
        });
    }

    public void publish(String message)
    {
        executorService.execute(() -> {
            try (Jedis jedis = jedispool.getResource()) {
                jedis.publish("globalMessage", message);
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
            }
        });
    }

    public void receiveGlobalChat(String message) {
        message = message.replace("globalmessage:", "");
        String[] split = message.split(">");

        for (Player player : proxy.getAllPlayers()) {
            String server = player.getCurrentServer().get().getServerInfo().getName();
            player.sendMessage(LegacyComponentSerializer.legacySection().deserialize(("§7[§cGLOBAL§7] [§b" + server.toUpperCase() + "§7] " + message)));
            System.out.println("§7[§cGLOBAL§7] [§b" + server.toUpperCase() + "§7] " + message);
        }
    }
}
