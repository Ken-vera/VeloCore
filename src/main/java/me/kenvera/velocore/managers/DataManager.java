package me.kenvera.velocore.managers;

import me.kenvera.velocore.VeloCore;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataManager {
    private ConfigurationLoader<?> loader;
    private ConfigurationNode rootNode;
    private VeloCore plugin;
    public DataManager(VeloCore plugin) {
        this.plugin = plugin;
        this.load();
    }

    public void load() {
        Path configPath = Paths.get("plugins/velocore/config.yml");
        File configFile = configPath.toFile();

        if (!configFile.exists()) {
            plugin.getLogger().error("Config.yml is not found!");
            plugin.getLogger().warn("Generating one...");
            createConfigFromResource(configPath, "/config.yml");
        }

        loader = YAMLConfigurationLoader.builder().setPath(configPath).build();

        try {
            rootNode = loader.load();
            plugin.getLogger().info("Configuration Loaded!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ConfigurationNode getKey(String key) {
        return rootNode.getNode(key);
    }

    public String getString(String key, String defaultValue) {
        Object value = rootNode.getNode((Object[]) key.split("\\.")).getValue();
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        Object value = rootNode.getNode((Object[]) key.split("\\.")).getValue();
        if (value instanceof Integer) {
            return (int) value;
        }
        return defaultValue;
    }

    public Long getLong(String key) {
        Object value = rootNode.getNode((Object[]) key.split("\\.")).getValue();
        if (value instanceof Long) {
            return (long) value;
        }
        return null;
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        Object value = rootNode.getNode((Object[]) key.split("\\.")).getValue();
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        return defaultValue;
    }

    private void createConfigFromResource(Path configPath, String resourcePath) {
        try (InputStream resourceStream = DataManager.class.getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                File configFile = configPath.toFile();
                configFile.getParentFile().mkdirs();

                java.nio.file.Files.copy(resourceStream, configPath);
                plugin.getLogger().info("Generated new config file!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
