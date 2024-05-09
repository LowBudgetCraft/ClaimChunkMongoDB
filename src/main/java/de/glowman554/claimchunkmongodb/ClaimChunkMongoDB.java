package de.glowman554.claimchunkmongodb;

import com.cjburkey.claimchunk.ClaimChunk;
import de.glowman554.claimchunkmongodb.commands.CacheCommand;
import de.glowman554.claimchunkmongodb.database.MongoDataHandler;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class ClaimChunkMongoDB extends JavaPlugin {
    private static ClaimChunkMongoDB instance;
    private Configuration configuration;

    private MongoDataHandler dataHandler;

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

        dataHandler = new MongoDataHandler(configuration.uri, configuration.database);

        try {
            ClaimChunk.getInstance().overrideDataHandler(dataHandler);
        } catch (ClaimChunk.DataHandlerAlreadySetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onEnable() {
        dataHandler.getClaimHandler().schedulePeriodicCacheClear();

        CacheCommand cache = new CacheCommand();
        Objects.requireNonNull(getCommand("cache")).setExecutor(cache);
        Objects.requireNonNull(getCommand("cache")).setTabCompleter(cache);
    }

    @Override
    public void onDisable() {
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public MongoDataHandler getDataHandler() {
        return dataHandler;
    }
}
