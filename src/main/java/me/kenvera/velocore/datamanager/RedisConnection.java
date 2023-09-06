package me.kenvera.velocore.datamanager;

import com.velocitypowered.api.proxy.ProxyServer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisConnectionException;

public class RedisConnection {
    private final ProxyServer proxy;
    private final String redisHost = "100.126.17.140";
    private final int redisPort = 6379;
    private final String redisPassword = "51535968db86376b607c8a947149ce51376189ab09f63656f17b938a6db335f5";
    public RedisConnection(ProxyServer proxy) {
        this.proxy = proxy;

        try {
            Jedis jedis = new Jedis(redisHost, redisPort);
            System.out.println("Redis Host");
            jedis.auth("default", redisPassword);
            System.out.println("Redis Auth");
            jedis.ping();
            System.out.println("Redis Ping");
            jedis.close();
            System.out.println("Redis Close");
            System.out.println("Successfully connected to Redis server!");
        } catch (JedisConnectionException e) {
            e.printStackTrace();
        }
    }
}
