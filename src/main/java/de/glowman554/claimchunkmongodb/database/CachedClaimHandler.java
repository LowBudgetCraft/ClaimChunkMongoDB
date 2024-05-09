package de.glowman554.claimchunkmongodb.database;

import com.cjburkey.claimchunk.chunk.ChunkPlayerPermissions;
import com.cjburkey.claimchunk.chunk.ChunkPos;
import com.cjburkey.claimchunk.chunk.DataChunk;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import de.glowman554.claimchunkmongodb.ClaimChunkMongoDB;
import org.bson.Document;
import org.bukkit.Bukkit;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.mongodb.client.model.Updates.set;
import static com.mongodb.client.model.Updates.unset;

public class CachedClaimHandler extends Cache<ChunkPos, DataChunk> {
    private final MongoCollection<Document> claims;

    public CachedClaimHandler(MongoCollection<Document> claims) {
        this.claims = claims;
    }


    private Document createPos(ChunkPos pos) {
        return new Document().append("x", pos.getX()).append("z", pos.getZ()).append("world", pos.getWorld());
    }

    private Document createPlayerPermission(ChunkPlayerPermissions permissions) {
        return new Document("permissionFlags", permissions.getPermissionFlags());
    }

    private Document createClaim(DataChunk dataChunk) {
        Document playerPermissions = new Document();
        for (UUID player : dataChunk.playerPermissions.keySet()) {
            playerPermissions.append(player.toString(), createPlayerPermission(dataChunk.playerPermissions.get(player)));
        }

        Document chunk = new Document("_id", createPos(dataChunk.chunk));
        chunk.append("player", dataChunk.player.toString()).append("tnt", dataChunk.tnt).append("playerPermissions", playerPermissions);
        return chunk;
    }

    private ChunkPos fromPos(Document document) {
        return new ChunkPos(document.getString("world"), document.getInteger("x"), document.getInteger("z"));
    }

    private ChunkPlayerPermissions fromPlayerPermission(Document document) {
        return new ChunkPlayerPermissions(document.getInteger("permissionFlags"));
    }

    private DataChunk fromClaim(Document document) {
        Map<UUID, ChunkPlayerPermissions> permissionsMap = new HashMap<>();

        Document playerPermission = document.get("playerPermissions", Document.class);
        for (String key : playerPermission.keySet()) {
            permissionsMap.put(UUID.fromString(key), new ChunkPlayerPermissions(playerPermission.get(key, Document.class).getInteger("permissionFlags")));
        }

        return new DataChunk(fromPos(document.get("_id", Document.class)), UUID.fromString(document.getString("player")), permissionsMap, document.getBoolean("tnt"));
    }


    @Override
    protected DataChunk queryDB(ChunkPos key) {
        Document document = claims.find(new Document("_id", createPos(key))).first();
        if (document == null) {
            return null;
        }
        return fromClaim(document);
    }

    @Override
    protected void deleteDB(ChunkPos key) {
        claims.deleteMany(new Document("_id", createPos(key)));
    }

    @Override
    protected List<DataChunk> queryDBAll() {
        FindIterable<Document> iter = claims.find();

        List<DataChunk> results = new ArrayList<>();
        for (Document document : iter) {
            results.add(fromClaim(document));
        }

        return results;
    }

    @Override
    protected ChunkPos extractKey(DataChunk value) {
        return value.chunk;
    }

    @Override
    protected void insertDB(DataChunk[] dataChunks) {
        List<Document> insertions = new ArrayList<>();
        for (DataChunk dataChunk : dataChunks) {
            insertions.add(createClaim(dataChunk));
        }
        claims.insertMany(insertions);
    }


    public boolean toggleTNT(ChunkPos pos) {
        DataChunk chunk = get(pos);
        chunk.tnt = !chunk.tnt;

        claims.updateOne(new Document("_id", createPos(chunk.chunk)), set("tnt", chunk.tnt));

        return chunk.tnt;
    }

    public void givePlayerAccess(ChunkPos chunk, UUID accessor, ChunkPlayerPermissions permissions) {
        claims.updateOne(new Document("_id", createPos(chunk)), new Document("playerPermissions", set(accessor.toString(), createPlayerPermission(permissions))));
        uncache(chunk);
    }

    public void takePlayerAccess(ChunkPos chunk, UUID accessor) {
        claims.updateOne(new Document("_id", createPos(chunk)), new Document("playerPermissions", unset(accessor.toString())));
        uncache(chunk);
    }

    public void schedulePeriodicCacheClear() {
        int ticks = 20 * 60 * 60;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(ClaimChunkMongoDB.getInstance(), this::periodicClear, ticks, ticks);
    }

    private void periodicClear() {
        Logger logger = ClaimChunkMongoDB.getInstance().getLogger();

        logger.log(Level.INFO, "Clearing claimed chunk cache now.");
        logger.log(Level.INFO, "Size was " + getCache().size());
        clear();
    }
}
