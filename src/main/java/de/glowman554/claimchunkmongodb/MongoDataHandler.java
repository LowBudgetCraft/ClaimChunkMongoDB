package de.glowman554.claimchunkmongodb;

import com.cjburkey.claimchunk.chunk.ChunkPlayerPermissions;
import com.cjburkey.claimchunk.chunk.ChunkPos;
import com.cjburkey.claimchunk.chunk.DataChunk;
import com.cjburkey.claimchunk.data.newdata.IClaimChunkDataHandler;
import com.cjburkey.claimchunk.player.FullPlayerData;
import com.cjburkey.claimchunk.player.SimplePlayerData;
import com.mongodb.client.*;
import org.bson.Document;

import java.util.*;
import java.util.logging.Level;

import static com.mongodb.client.model.Updates.*;

public class MongoDataHandler<T extends IClaimChunkDataHandler> implements IClaimChunkDataHandler {
    private final String connectionUri;
    private final String mongoDatabase;
    private MongoClient mongo;
    private MongoCollection<Document> claims;
    private MongoCollection<Document> players;

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
        claims = database.getCollection("claims");

        database.createCollection("players");
        players = database.getCollection("players");
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

    private Document createClaim(DataChunk dataChunk) {
        Document playerPermissions = new Document();
        for (UUID player : dataChunk.playerPermissions.keySet()) {
            playerPermissions.append(player.toString(), createPlayerPermission(dataChunk.playerPermissions.get(player)));
        }

        Document chunk = new Document("_id", createPos(dataChunk.chunk));
        chunk
                .append("player", dataChunk.player.toString())
                .append("tnt", dataChunk.tnt)
                .append("playerPermissions", playerPermissions);
        return chunk;
    }

    private Document createPos(ChunkPos pos) {
        return new Document()
                .append("x", pos.getX())
                .append("z", pos.getZ())
                .append("world", pos.getWorld());
    }

    private Document createPlayerPermission(ChunkPlayerPermissions permissions) {
        return new Document("permissionFlags", permissions.getPermissionFlags());
    }

    private DataChunk fromClaim(Document document) {
        Map<UUID, ChunkPlayerPermissions> permissionsMap = new HashMap<>();

        Document playerPermission = document.get("playerPermissions", Document.class);
        for (String key : playerPermission.keySet()) {
            permissionsMap.put(UUID.fromString(key), new ChunkPlayerPermissions(playerPermission.get(key, Document.class).getInteger("permissionFlags")));
        }

        return new DataChunk(fromPos(document.get("_id", Document.class)), UUID.fromString(document.getString("player")), permissionsMap, document.getBoolean("tnt"));
    }

    private ChunkPos fromPos(Document document) {
        return new ChunkPos(document.getString("world"), document.getInteger("x"), document.getInteger("z"));
    }

    private ChunkPlayerPermissions fromPlayerPermission(Document document) {
        return new ChunkPlayerPermissions(document.getInteger("permissionFlags"));
    }

    @Override
    public void addClaimedChunk(ChunkPos pos, UUID player) {
        claims.insertOne(createClaim(new DataChunk(pos, player, Map.of(), false)));
    }

    @Override
    public void addClaimedChunks(DataChunk[] chunks) {
        List<Document> insertions = new ArrayList<>();
        for (DataChunk chunk : chunks) {
            insertions.add(createClaim(chunk));
        }
        claims.insertMany(insertions);
    }

    @Override
    public void removeClaimedChunk(ChunkPos pos) {
        claims.deleteMany(new Document("_id", createPos(pos)));
    }

    private Document queryChunk(ChunkPos pos) {
        return claims.find(new Document("_id", createPos(pos))).first();
    }

    @Override
    public boolean isChunkClaimed(ChunkPos pos) {
        Document claim = queryChunk(pos);
        return claim != null;
    }

    @Override
    public UUID getChunkOwner(ChunkPos pos) {
        Document claim = queryChunk(pos);
        return UUID.fromString(claim.getString("player"));
    }

    @Override
    public DataChunk[] getClaimedChunks() {
        FindIterable<Document> iter = claims.find();

        List<DataChunk> results = new ArrayList<>();
        for (Document document : iter) {
            results.add(fromClaim(document));
        }

        return results.toArray(DataChunk[]::new);
    }

    @Override
    public boolean toggleTnt(ChunkPos pos) {
        Document claim = queryChunk(pos);

        boolean tnt = !claim.getBoolean("tnt");
        claims.updateOne(new Document("_id", claim.get("_id")), set("tnt", tnt));

        return tnt;
    }

    @Override
    public boolean isTntEnabled(ChunkPos pos) {
        Document claim = queryChunk(pos);
        return claim.getBoolean("tnt");
    }

    private Document createPlayer(FullPlayerData fullPlayerData) {
        return new Document("_id", fullPlayerData.player.toString())
                .append("lastIgn", fullPlayerData.lastIgn)
                .append("chunkName", fullPlayerData.chunkName)
                .append("lastOnlineTime", fullPlayerData.lastOnlineTime)
                .append("alert", fullPlayerData.alert)
                .append("extraMaxClaims", fullPlayerData.extraMaxClaims);
    }

    private SimplePlayerData fromPlayerSimple(Document document) {
        return new SimplePlayerData(UUID.fromString(document.getString("_id")), document.getString("lastIgn"), document.getLong("lastOnlineTime"));
    }

    private FullPlayerData fromPlayer(Document document) {
        return new FullPlayerData(UUID.fromString(document.getString("_id")), document.getString("lastIgn"), document.getString("chunkName"), document.getInteger("lastOnlineTime"), document.getBoolean("alert"), document.getInteger("extraMaxClaims"));
    }

    @Override
    public void addPlayer(UUID player, String lastIgn, String chunkName, long lastOnlineTime, boolean alerts, int maxClaims) {
        players.insertOne(createPlayer(new FullPlayerData(player, lastIgn, chunkName, lastOnlineTime, alerts, maxClaims)));
    }

    @Override
    public void addPlayers(FullPlayerData[] fullPlayers) {
        List<Document> insertions = new ArrayList<>();
        for (FullPlayerData player : fullPlayers) {
            insertions.add(createPlayer(player));
        }
        players.insertMany(insertions);
    }

    private Document queryPlayer(UUID player) {
        return players.find(new Document("_id", player.toString())).first();
    }

    @Override
    public String getPlayerUsername(UUID player) {
        Document document = queryPlayer(player);
        if (document == null) {
            return null;
        }
        return document.getString("lastIgn");
    }

    @Override
    public UUID getPlayerUUID(String username) {
        Document document = players.find(new Document("lastIgn", username)).first();
        if (document == null) {
            return null;
        }
        return UUID.fromString(document.getString("_id"));
    }

    @Override
    public void setPlayerLastOnline(UUID player, long time) {
        players.updateOne(new Document("_id", player.toString()), set("lastOnlineTime", time));
    }

    @Override
    public void setPlayerChunkName(UUID player, String name) {
        players.updateOne(new Document("_id", player.toString()), set("chunkName", name));
    }

    @Override
    public String getPlayerChunkName(UUID player) {
        Document document = queryPlayer(player);
        if (document == null) {
            return null;
        }
        return document.getString("chunkName");
    }

    @Override
    public void setPlayerReceiveAlerts(UUID player, boolean alerts) {
        players.updateOne(new Document("_id", player.toString()), set("alert", alerts));
    }

    @Override
    public boolean getPlayerReceiveAlerts(UUID player) {
        Document document = queryPlayer(player);
        if (document == null) {
            return false;
        }
        return document.getBoolean("alert");
    }

    @Override
    public void setPlayerExtraMaxClaims(UUID player, int maxClaims) {
        players.updateOne(new Document("_id", player.toString()), set("extraMaxClaims", maxClaims));
    }

    @Override
    public void addPlayerExtraMaxClaims(UUID player, int numToAdd) {
        players.updateOne(new Document("_id", player.toString()), inc("extraMaxClaims", numToAdd));
    }

    @Override
    public void takePlayerExtraMaxClaims(UUID player, int numToTake) {
        addPlayerExtraMaxClaims(player, -numToTake);
    }

    @Override
    public int getPlayerExtraMaxClaims(UUID player) {
        Document document = queryPlayer(player);
        return document.getInteger("extraMaxClaims");
    }

    @Override
    public boolean hasPlayer(UUID player) {
        Document document = queryPlayer(player);
        return document != null;
    }

    @Override
    public Collection<SimplePlayerData> getPlayers() {
        FindIterable<Document> iter = players.find();

        Collection<SimplePlayerData> result = new ArrayList<>();
        for (Document player : iter) {
            result.add(fromPlayerSimple(player));
        }

        return result;
    }

    @Override
    public FullPlayerData[] getFullPlayerData() {
        FindIterable<Document> iter = players.find();

        List<FullPlayerData> result = new ArrayList<>();
        for (Document player : iter) {
            result.add(fromPlayer(player));
        }
        return result.toArray(FullPlayerData[]::new);
    }

    @Override
    public void givePlayerAccess(ChunkPos chunk, UUID accessor, ChunkPlayerPermissions permissions) {
        claims.updateOne(new Document("_id", createPos(chunk)), new Document("playerPermissions", set(accessor.toString(), createPlayerPermission(permissions))));
    }

    @Override
    public void takePlayerAccess(ChunkPos chunk, UUID accessor) {
        claims.updateOne(new Document("_id", createPos(chunk)), new Document("playerPermissions", unset(accessor.toString())));
    }

    @Override
    public Map<UUID, ChunkPlayerPermissions> getPlayersWithAccess(ChunkPos chunk) {
        Document document = queryChunk(chunk);
        if (document == null) {
            return null;
        }
        return fromClaim(document).playerPermissions;
    }
}
