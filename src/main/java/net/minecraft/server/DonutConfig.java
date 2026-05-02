package net.minecraft.server;

import java.io.*;
import java.util.Properties;

public class DonutConfig {

    private static final String CONFIG_PATH = "donut.properties";
    private static final Properties props = new Properties();

    static {
        File file = new File(CONFIG_PATH);

        if (!file.exists()) {
            writeDefaults(file);
        }

        try {
            props.load(new FileReader(file));
        } catch (IOException e) {
            System.out.println("[Donut] Failed to load donut.properties, using defaults.");
            e.printStackTrace();
        }
    }

    private static void writeDefaults(File file) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(file));
            writer.println("# Donut Config - Project Poseidon");
            writer.println("# Custom world generation and server settings");
            writer.println();
            writer.println("# --- Structures ---");
            writer.println("pyramids-enabled=false");
            writer.println();
            writer.println("# --- Chunk Loading ---");
            writer.println("# Cache recently loaded chunks in memory for faster access");
            writer.println("chunk-cache-enabled=true");
            writer.println("# Load chunks from disk on a separate thread");
            writer.println("async-chunk-load-enabled=true");
            writer.println("# Save chunks to disk on a separate thread");
            writer.println("async-chunk-save-enabled=true");
            writer.println();
            writer.close();
            System.out.println("[Donut] Created default donut.properties");
        } catch (IOException e) {
            System.out.println("[Donut] Failed to create donut.properties.");
            e.printStackTrace();
        }
    }

    public static void load() {
    System.out.println("[Donut] Config loaded from: " + new File(CONFIG_PATH).getAbsolutePath());
    System.out.println("[Donut] pyramids-enabled: " + getBoolean("pyramids-enabled", true));
    System.out.println("[Donut] async-chunk-load-enabled: " + getBoolean("async-chunk-load-enabled", true));
    System.out.println("[Donut] async-chunk-save-enabled: " + getBoolean("async-chunk-save-enabled", true));
    System.out.println("[Donut] chunk-cache-enabled: " + getBoolean("chunk-cache-enabled", true));
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(props.getProperty(key, String.valueOf(defaultValue)));
    }

    public static int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(props.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static String getString(String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
}
