package de.glowman554.claimchunkmongodb.database;

import com.cjburkey.claimchunk.chunk.ChunkPlayerPermissions;
import com.cjburkey.claimchunk.chunk.ChunkPos;
import com.cjburkey.claimchunk.chunk.DataChunk;
import com.cjburkey.claimchunk.data.newdata.IClaimChunkDataHandler;
import com.cjburkey.claimchunk.player.FullPlayerData;
import com.cjburkey.claimchunk.player.SimplePlayerData;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import de.glowman554.claimchunkmongodb.ClaimChunkMongoDB;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class MongoDataHandler implements IClaimChunkDataHandler {
    private final String connectionUri;
    private final String mongoDatabase;
    private MongoClient mongo;

    private CachedClaimHandler claimHandler;
    private CachedPlayerHandler playerHandler;

    public MongoDataHandler(String connectionUri, String mongoDatabase) {
        this.connectionUri = connectionUri;
        this.mongoDatabase = mongoDatabase;
    }


    @Override
    public void init() throws Exception {
        ClaimChunkMongoDB.getInstance().getLogger().log(Level.INFO, "Connecting to database...");
        mongo = MongoClients.create(connectionUri);
        MongoDatabase database = mongo.getDatabase(mongoDatabase);

        database.createCollection("claims");
        claimHandler = new CachedClaimHandler(database.getCollection("claims"));

        database.createCollection("players");
        playerHandler = new CachedPlayerHandler(database.getCollection("players"));
    }

    @Override
    public boolean getHasInit() {
        return mongo != null;
    }

    @Override
    public void exit() throws Exception {
        ClaimChunkMongoDB.getInstance().getLogger().log(Level.INFO, "Disconnecting from database");
        mongo.close();
    }

    @Override
    public void save() throws Exception {
    }

    @Override
    public void load() throws Exception {
    }


    @Override
    public void addClaimedChunk(ChunkPos pos, UUID player) {
        claimHandler.insert(new DataChunk[]{new DataChunk(pos, player, Map.of(), false)});
    }

    @Override
    public void addClaimedChunks(DataChunk[] chunks) {
        claimHandler.insert(chunks);
    }

    @Override
    public void removeClaimedChunk(ChunkPos pos) {
        claimHandler.delete(pos);
    }


    @Override
    public boolean isChunkClaimed(ChunkPos pos) {
        DataChunk dataChunk = claimHandler.get(pos);
        return dataChunk != null;
    }

    @Override
    public UUID getChunkOwner(ChunkPos pos) {
        DataChunk dataChunk = claimHandler.get(pos);
        if (dataChunk == null) {
            return null;
        }
        return dataChunk.player;
    }

    @Override
    public DataChunk[] getClaimedChunks() {
        return claimHandler.all().toArray(DataChunk[]::new);
    }

    @Override
    public boolean toggleTnt(ChunkPos pos) {
        return claimHandler.toggleTNT(pos);
    }

    @Override
    public boolean isTntEnabled(ChunkPos pos) {
        DataChunk dataChunk = claimHandler.get(pos);
        if (dataChunk == null) {
            return false;
        }
        return dataChunk.tnt;
    }


    @Override
    public void addPlayer(UUID player, String lastIgn, String chunkName, long lastOnlineTime, boolean alerts, int maxClaims) {
        playerHandler.insert(new FullPlayerData[]{new FullPlayerData(player, lastIgn, chunkName, lastOnlineTime, alerts, maxClaims)});
    }

    @Override
    public void addPlayers(FullPlayerData[] fullPlayers) {
        playerHandler.insert(fullPlayers);
    }


    @Override
    public String getPlayerUsername(UUID player) {
        FullPlayerData playerData = playerHandler.get(player);
        if (playerData == null) {
            return null;
        }
        return playerData.lastIgn;
    }

    @Override
    public UUID getPlayerUUID(String username) {
        return playerHandler.fromUsername(username);
    }

    @Override
    public void setPlayerLastOnline(UUID player, long time) {
        playerHandler.updateLastOnline(player, time);
    }

    @Override
    public void setPlayerChunkName(UUID player, String name) {
        playerHandler.updateChunkName(player, name);
    }

    @Override
    public String getPlayerChunkName(UUID player) {
        FullPlayerData playerData = playerHandler.get(player);
        if (playerData == null) {
            return null;
        }
        return playerData.chunkName;
    }

    @Override
    public void setPlayerReceiveAlerts(UUID player, boolean alerts) {
        playerHandler.updateAlerts(player, alerts);
    }

    @Override
    public boolean getPlayerReceiveAlerts(UUID player) {
        FullPlayerData playerData = playerHandler.get(player);
        if (playerData == null) {
            return false;
        }
        return playerData.alert;
    }

    @Override
    public void setPlayerExtraMaxClaims(UUID player, int maxClaims) {
        playerHandler.updateExtraMaxClaims(player, maxClaims);
    }

    @Override
    public void addPlayerExtraMaxClaims(UUID player, int numToAdd) {
        FullPlayerData playerData = playerHandler.get(player);
        if (playerData != null) {
            playerHandler.updateExtraMaxClaims(player, playerData.extraMaxClaims + numToAdd);
        }
    }

    @Override
    public void takePlayerExtraMaxClaims(UUID player, int numToTake) {
        addPlayerExtraMaxClaims(player, -numToTake);
    }

    @Override
    public int getPlayerExtraMaxClaims(UUID player) {
        FullPlayerData playerData = playerHandler.get(player);
        if (playerData == null) {
            return 0;
        }
        return playerData.extraMaxClaims;
    }

    @Override
    public boolean hasPlayer(UUID player) {
        FullPlayerData playerData = playerHandler.get(player);
        return playerData != null;
    }

    @Override
    public Collection<SimplePlayerData> getPlayers() {
        return playerHandler.all().stream()
                .map(fullPlayerData -> new SimplePlayerData(fullPlayerData.player, fullPlayerData.lastIgn, fullPlayerData.lastOnlineTime))
                .toList();
    }

    @Override
    public FullPlayerData[] getFullPlayerData() {
        return playerHandler.all().toArray(FullPlayerData[]::new);
    }

    @Override
    public void givePlayerAccess(ChunkPos chunk, UUID accessor, ChunkPlayerPermissions permissions) {
        claimHandler.givePlayerAccess(chunk, accessor, permissions);
    }

    @Override
    public void takePlayerAccess(ChunkPos chunk, UUID accessor) {
        claimHandler.takePlayerAccess(chunk, accessor);
    }

    @Override
    public Map<UUID, ChunkPlayerPermissions> getPlayersWithAccess(ChunkPos chunk) {
        DataChunk dataChunk = claimHandler.get(chunk);
        if (dataChunk == null) {
            return null;
        }
        return dataChunk.playerPermissions;
    }

    public CachedClaimHandler getClaimHandler() {
        return claimHandler;
    }

    public CachedPlayerHandler getPlayerHandler() {
        return playerHandler;
    }
}
