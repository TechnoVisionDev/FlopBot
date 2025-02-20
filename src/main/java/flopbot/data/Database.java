package flopbot.data;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;
import flopbot.data.cache.Stake;
import flopbot.data.cache.Wallet;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.jetbrains.annotations.NotNull;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Manages data between the bot and the MongoDB database.
 *
 * @author TechnoVision
 */
public class Database {

    /** Collections */
    public @NotNull MongoCollection<Wallet> wallets;
    public @NotNull MongoCollection<Stake> stakes;

    /**
     * Connect to database using MongoDB URI and
     * initialize any collections that don't exist.
     *
     * @param uri MongoDB uri string.
     */
    public Database(String uri) {
        // Setup MongoDB database with URI.
        CodecRegistry pojoCodecRegistry = fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry = fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .codecRegistry(codecRegistry)
                .build();
        MongoClient mongoClient = MongoClients.create(clientSettings);
        MongoDatabase database = mongoClient.getDatabase("TechnoBot");

        // Initialize collections if they don't exist.
        wallets = database.getCollection("wallets", Wallet.class);
        stakes = database.getCollection("stakes", Stake.class);

        // Add custom indexing to collections
        wallets.createIndex(Indexes.descending("user"));
        stakes.createIndex(Indexes.ascending("txid"));
    }
}
