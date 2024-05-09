package de.glowman554.claimchunkmongodb.database;

import com.cjburkey.claimchunk.player.FullPlayerData;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Updates.set;

public class CachedPlayerHandler extends Cache<UUID, FullPlayerData> {
    private final MongoCollection<Document> players;

    public CachedPlayerHandler(MongoCollection<Document> players) {
        this.players = players;
    }

    private Document createPlayer(FullPlayerData fullPlayerData) {
        return new Document("_id", fullPlayerData.player.toString())
                .append("lastIgn", fullPlayerData.lastIgn)
                .append("chunkName", fullPlayerData.chunkName)
                .append("lastOnlineTime", fullPlayerData.lastOnlineTime)
                .append("alert", fullPlayerData.alert)
                .append("extraMaxClaims", fullPlayerData.extraMaxClaims);
    }

    private FullPlayerData fromPlayer(Document document) {
        return new FullPlayerData(UUID.fromString(document.getString("_id")), document.getString("lastIgn"), document.getString("chunkName"), document.getLong("lastOnlineTime"), document.getBoolean("alert"), document.getInteger("extraMaxClaims"));
    }


    @Override
    protected FullPlayerData queryDB(UUID key) {
        Document document = players.find(new Document("_id", key.toString())).first();
        if (document == null) {
            return null;
        }
        return fromPlayer(document);
    }

    @Override
    protected void deleteDB(UUID key) {
        players.deleteMany(new Document("_id", key.toString()));
    }

    @Override
    protected List<FullPlayerData> queryDBAll() {
        FindIterable<Document> iter = players.find();

        List<FullPlayerData> results = new ArrayList<>();
        for (Document document : iter) {
            results.add(fromPlayer(document));
        }

        return results;
    }

    @Override
    protected UUID extractKey(FullPlayerData value) {
        return value.player;
    }

    @Override
    protected void insertDB(FullPlayerData[] values) {
        List<Document> insertions = new ArrayList<>();
        for (FullPlayerData playerData : values) {
            insertions.add(createPlayer(playerData));
        }
        players.insertMany(insertions);
    }


    public UUID fromUsername(String username) {
        for (FullPlayerData playerData : getCache().values()) {
            if (playerData.lastIgn.equals(username)) {
                return playerData.player;
            }
        }
        Document document = players.find(new Document("lastIgn", username)).first();
        if (document == null) {
            return null;
        }
        FullPlayerData playerData = fromPlayer(document);
        cache(playerData.player, playerData);
        return playerData.player;
    }

    public void updateLastOnline(UUID uuid, long time) {
        FullPlayerData playerData = get(uuid);
        playerData.lastOnlineTime = time;

        players.updateOne(new Document("_id", uuid.toString()), set("lastOnlineTime", time));
    }

    public void updateChunkName(UUID uuid, String chunkName) {
        FullPlayerData playerData = get(uuid);
        playerData.chunkName = chunkName;

        players.updateOne(new Document("_id", uuid.toString()), set("chunkName", chunkName));
    }

    public void updateAlerts(UUID uuid, boolean alert) {
        FullPlayerData playerData = get(uuid);
        playerData.alert = alert;

        players.updateOne(new Document("_id", uuid.toString()), set("alert", alert));
    }

    public void updateExtraMaxClaims(UUID uuid, int maxClaims) {
        FullPlayerData playerData = get(uuid);
        playerData.extraMaxClaims = maxClaims;

        players.updateOne(new Document("_id", uuid.toString()), set("extraMaxClaims", maxClaims));
    }
}
