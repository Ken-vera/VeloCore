package me.kenvera.velocore.database;

import me.kenvera.velocore.VeloCore;
import me.kenvera.velocore.listeners.RedisListener;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisManager {
    private final VeloCore plugin;
    private final JedisPool jedispool;
    private JedisPubSub jedisPubSub;
    private final ExecutorService executorService = Executors.newFixedThreadPool(2);

    public RedisManager(VeloCore plugin, String host, int port, String password) {
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
        String channelName = plugin.getConfigManager().getString("redis.channel", "chronosync");

        subscribe(channelName);
    }

    public void subscribe(String channelName) {
        executorService.execute(() -> {
            try (Jedis jedis = jedispool.getResource()) {
                jedisPubSub = new RedisListener(plugin, this, channelName);
                jedis.subscribe(jedisPubSub, channelName);
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

    public void setKey(String key, Long value) {
        try (Jedis jedis = jedispool.getResource()) {
            jedis.set(key, String.valueOf(value));
        }
    }

    public void setMute(String identifier, long expire, String reason, String issuer) {
        try (Jedis jedis = jedispool.getResource()) {
            jedis.hset(identifier, "mute-expire", String.valueOf(expire));
            jedis.hset(identifier, "mute-reason", reason);
            jedis.hset(identifier, "mute-issuer", issuer);

            jedis.pexpire(identifier, expire - System.currentTimeMillis());
        }
    }

    public void removeMute(String identifier) {
        try (Jedis jedis = jedispool.getResource()) {
            for (String key : jedis.keys(identifier)) {
                jedis.del(key);
            }
        }
    }

    public String getKey(String key) {
        try (Jedis jedis = jedispool.getResource()) {
            return jedis.get(key);
        }
    }

    public void removeKey(String key) {
        try (Jedis jedis = jedispool.getResource()) {
            jedis.del(key);
        }
    }
}
