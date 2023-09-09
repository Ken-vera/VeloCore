package me.kenvera.velocore.managers;

import com.velocitypowered.api.proxy.ProxyServer;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DataManager {
    private ProxyServer proxy;
    private ConfigurationLoader<?> loader;
    private ConfigurationNode rootNode;
    public DataManager(ProxyServer proxy) {
        this.proxy = proxy;
    }

    public void load() {
        // Assuming you have a 'config.yml' file in your plugin's data folder
        Path configPath = Paths.get("plugins/velocore/config.yml");
        File configFile = configPath.toFile();

        // Check if the config file doesn't exist, and create it from a resource
        if (!configFile.exists()) {
            System.out.println("Config.yml is not found!");
            System.out.println("Generating one...");
            createConfigFromResource(configPath, "/config.yml");
        }

        // Create the loader
        loader = YAMLConfigurationLoader.builder().setPath(configPath).build();

        try {
            // Load the configuration file
            rootNode = loader.load();
            System.out.println("Configuration Loaded!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public String getString(String key, String defaultValue) {
        return rootNode.getNode(key).getString(defaultValue);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return rootNode.getNode(key).getBoolean(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        return rootNode.getNode(key).getInt(defaultValue);
    }

    public ConfigurationNode getKey(String key) {
        return rootNode.getNode(key);
    }

    private void createConfigFromResource(Path configPath, String resourcePath) {
        try (InputStream resourceStream = DataManager.class.getResourceAsStream(resourcePath)) {
            if (resourceStream != null) {
                File configFile = configPath.toFile();
                configFile.getParentFile().mkdirs();

                // Copy the resource to the config file
                java.nio.file.Files.copy(resourceStream, configPath);
                System.out.println("Config created!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
