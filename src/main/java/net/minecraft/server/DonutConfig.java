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
            writer.println("# Structures");
            writer.println("pyramids-enabled=false");
            writer.println();
            writer.close();
        } catch (IOException e) {
            System.out.println("[Donut] Failed to create donut.properties.");
            e.printStackTrace();
        }
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