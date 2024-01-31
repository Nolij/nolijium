package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.data.fingerprint.FingerprintMeasure;
import me.jellysquid.mods.sodium.client.data.fingerprint.HashedFingerprint;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SodiumClientMod implements ClientModInitializer {

    public static final String MODID = "nolijium";
    public static final String MODNAME = "Nolijium";

    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    private static SodiumGameOptions CONFIG = loadConfig();

    private static String MOD_VERSION;

    public static boolean oculusLoaded = false;

    @Override
    public void onInitializeClient() {
        oculusLoaded = FabricLoader.getInstance().isModLoaded("iris");

        MOD_VERSION = FabricLoader.getInstance().getModContainer(MODID).get().getMetadata().getVersion().toString();

        try {
            updateFingerprint();
        } catch (Throwable t) {
            LOGGER.error("Failed to update fingerprint", t);
        }
    }

    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    private static SodiumGameOptions loadConfig() {
        try {
            return SodiumGameOptions.load();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            var config = new SodiumGameOptions();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        CONFIG = SodiumGameOptions.defaults();

        try {
            CONFIG.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }

    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    private static void updateFingerprint() {
        var current = FingerprintMeasure.create();

        if (current == null) {
            return;
        }

        HashedFingerprint saved = null;

        try {
            saved = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            LOGGER.error("Failed to load existing fingerprint",  t);
        }

        if (saved == null || !current.looselyMatches(saved)) {
            HashedFingerprint.writeToDisk(current.hashed());

            CONFIG.notifications.hasSeenDonationPrompt = false;
            CONFIG.notifications.hasClearedDonationButton = false;

            try {
                CONFIG.writeChanges();
            } catch (IOException e) {
                LOGGER.error("Failed to update config file", e);
            }
        }
    }

    public static boolean canUseVanillaVertices() {
        return !SodiumClientMod.options().performance.useCompactVertexFormat && !oculusLoaded;
    }
}