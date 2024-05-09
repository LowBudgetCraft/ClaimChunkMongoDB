package de.glowman554.claimchunkmongodb;

import com.cjburkey.claimchunk.ClaimChunk;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ClaimChunkMongoDB extends JavaPlugin {
    private static ClaimChunkMongoDB instance;
    private Configuration configuration;

    public ClaimChunkMongoDB() {
        instance = this;
    }

    public static ClaimChunkMongoDB getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        getDataFolder().mkdirs();
        configuration = new Configuration(new File(getDataFolder(), "config.json"));
        configuration.load();

        try {
            ClaimChunk.getInstance().overrideDataHandler(new MongoDataHandler<>(configuration.uri, configuration.database));
        } catch (ClaimChunk.DataHandlerAlreadySetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    public Configuration getConfiguration() {
        return configuration;
    }
}
