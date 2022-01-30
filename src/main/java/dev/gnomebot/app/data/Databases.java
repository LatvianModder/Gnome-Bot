package dev.gnomebot.app.data;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import dev.gnomebot.app.App;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.util.MapWrapper;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * @author LatvianModder
 */
public class Databases {
	public static final UpdateOptions UPSERT = new UpdateOptions().upsert(true);

	public final App app;
	public final MongoDatabase database;
	public final Map<String, WrappedCollection<?>> collections;

	private final Object guildLock = new Object();
	public final WrappedCollection<WebLogEntry> webLog;
	public final WrappedCollection<WebToken> webTokens;
	public final WrappedCollection<BasicDocument> guildData;
	public final Map<Snowflake, GuildCollections> guildCollections;
	public final WrappedCollection<BasicDocument> mmShowcase;
	public final WrappedCollection<UserWebhook> userWebhooks;
	public final WrappedCollection<Paste> pastes;

	public Databases(App m) {
		app = m;

		MongoClient mongoClient = MongoClients.create(Config.get().db_uri);
		database = mongoClient.getDatabase("gnomebot");

		collections = new LinkedHashMap<>();

		webLog = create("web_log", WebLogEntry::new).expiresAfterMonth("timestamp_expire", "timestamp");
		webTokens = create("web_tokens", WebToken::new);
		guildData = create("guild_data", BasicDocument::new);
		guildCollections = new HashMap<>();
		mmShowcase = create("mm_showcase", BasicDocument::new);
		userWebhooks = create("user_webhooks", UserWebhook::new);
		pastes = create("pastes", Paste::new);
	}

	// hardcoded
	public GuildCollections guildModdedMC() {
		return guild(Snowflake.of(166630061217153024L));
	}

	@Nullable
	public GuildCollections guildOrNull(@Nullable Snowflake id) {
		synchronized (guildLock) {
			return id == null ? null : guildCollections.get(id);
		}
	}

	public GuildCollections guild(Snowflake id) {
		synchronized (guildLock) {
			return guildCollections.computeIfAbsent(id, g -> new GuildCollections(this, g));
		}
	}

	public GuildCollections guild(GuildMessageChannel channel) {
		return guild(channel.getGuildId());
	}

	public <T extends WrappedDocument<T>> WrappedCollection<T> create(String ci, BiFunction<WrappedCollection<T>, MapWrapper, T> w) {
		WrappedCollection<T> collection = new WrappedCollection<>(this, ci, w);
		collections.put(ci, collection);
		return collection;
	}
}