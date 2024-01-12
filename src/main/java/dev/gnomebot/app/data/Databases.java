package dev.gnomebot.app.data;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.GuildPaths;
import dev.gnomebot.app.data.ping.InteractionDocument;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.data.HexId32;
import dev.latvian.apps.webutils.math.MathUtils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.channel.GuildMessageChannel;
import io.javalin.http.Context;
import io.javalin.websocket.WsConnectContext;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Databases {
	public static final UpdateOptions UPSERT = new UpdateOptions().upsert(true);

	public final App app;
	public final MongoClient mongoClient;
	public final MongoDatabase database;
	private final Object guildLock = new Object();

	public final Map<String, WrappedCollection<?>> collections;
	public final Map<Long, GuildCollections> guildCollections;
	public final Map<Integer, Macro> allMacros;
	public final Map<Long, ChannelSettings> channelSettings;

	public final WrappedCollection<WebLogEntry> webLogDB;
	private final WrappedCollection<WebToken> webTokensDB;
	public final WrappedCollection<BasicDocument> mmShowcaseDB;
	public final WrappedCollection<UserWebhook> userWebhooksDB;
	public final WrappedCollection<Paste> pastesDB;
	public final WrappedCollection<InteractionDocument> interactionsDB;
	public final WrappedCollection<ScheduledTask> scheduledTasksDB;
	public final WrappedCollection<DiscordPoll> pollsDB;
	public final WrappedCollection<ChannelSettings> channelSettingsDB;

	public WebToken selfToken;

	public Databases(App m) {
		app = m;

		mongoClient = MongoClients.create(Config.get().db_uri);
		database = mongoClient.getDatabase("gnomebot");

		collections = new LinkedHashMap<>();
		guildCollections = new HashMap<>();
		allMacros = new HashMap<>();
		channelSettings = new HashMap<>();

		webLogDB = create(database, "web_log", WebLogEntry::new).expiresAfterMonth("timestamp_expire", "timestamp", null);
		webTokensDB = create(database, "web_tokens", WebToken::new);
		mmShowcaseDB = create(database, "mm_showcase", BasicDocument::new);
		userWebhooksDB = create(database, "user_webhooks", UserWebhook::new);
		pastesDB = create(database, "pastes", Paste::new);
		interactionsDB = create(database, "interactions", InteractionDocument::new);
		scheduledTasksDB = create(database, "scheduled_tasks", ScheduledTask::new);
		pollsDB = create(database, "polls", DiscordPoll::new);
		channelSettingsDB = create(database, "channel_settings", ChannelSettings::new);

		for (var task : scheduledTasksDB.query()) {
			app.scheduledTasks.add(task);
		}

		for (var cs : channelSettingsDB.query()) {
			channelSettings.put(cs.channelId, cs);
		}

		try {
			for (var path : Files.list(AppPaths.GUILD_DATA).filter(Files::isDirectory).toList()) {
				var dirName = path.getFileName().toString();
				var id = GuildPaths.INVERTED_CUSTOM_NAMES.get().getOrDefault(dirName, SnowFlake.num(dirName));

				if (id != 0L) {
					guild(id);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@Nullable
	public GuildCollections guildOrNull(long id) {
		if (id == 0L) {
			return null;
		}

		synchronized (guildLock) {
			return guildCollections.get(id);
		}
	}

	public GuildCollections guild(long id) {
		synchronized (guildLock) {
			return guildCollections.computeIfAbsent(id, g -> new GuildCollections(this, g));
		}
	}

	public GuildCollections guild(Snowflake id) {
		return guild(id.asLong());
	}

	public GuildCollections guild(Guild guild) {
		return guild(guild.getId().asLong());
	}

	public GuildCollections guild(GuildMessageChannel channel) {
		return guild(channel.getGuildId());
	}

	public Collection<GuildCollections> allGuilds() {
		return guildCollections.values();
	}

	public <T extends WrappedDocument<T>> WrappedCollection<T> create(MongoDatabase database, String ci, BiFunction<WrappedCollection<T>, MapWrapper, T> w) {
		var collection = new WrappedCollection<T>(this, database, ci, w);
		collections.put(ci, collection);
		return collection;
	}

	public void createSelfToken() {
		var tokenDoc = new Document();
		tokenDoc.put("_id", Utils.createToken());
		tokenDoc.put("user", app.discordHandler.selfId);
		tokenDoc.put("name", "GnomeBot");
		tokenDoc.put("created", new Date());
		selfToken = new WebToken(webTokensDB, MapWrapper.wrap(tokenDoc));
	}

	public String getEncodedToken(long user, String name) {
		var token = webTokensDB.query().eq("user", user).first();

		if (token == null) {
			var tokenString = Utils.createToken();
			var document = new Document();
			document.put("_id", tokenString);
			document.put("user", user);
			document.put("name", name);
			document.put("created", new Date());
			webTokensDB.insert(document);
			return Base64.getUrlEncoder().encodeToString(tokenString.getBytes(StandardCharsets.UTF_8));
		} else {
			token.update(Updates.unset("guilds"));
			return Base64.getUrlEncoder().encodeToString(token.token.getBytes(StandardCharsets.UTF_8));
		}
	}

	@Nullable
	public WebToken getToken(Context ctx) {
		var a = ctx.header("Authorization");

		if (a != null && a.startsWith("Bearer ")) {
			var token = a.substring(7);

			if (token.equals(selfToken.token)) {
				return selfToken;
			}

			return getToken(token);
		}

		var c = ctx.cookie("gnometoken");

		if (c != null && !c.isEmpty()) {
			if (c.equals(selfToken.token)) {
				return selfToken;
			}

			return getToken(c);
		}

		var q = ctx.queryParam("logintoken");

		if (q != null && !q.isEmpty()) {
			var token = new String(Base64.getUrlDecoder().decode(q), StandardCharsets.UTF_8);
			var webToken = getToken(token);

			if (webToken != null) {
				webToken.justLoggedIn = true;
				ctx.cookie("gnometoken", webToken.token, 31536000);
				return webToken;
			}
		}

		return null;
	}

	@Nullable
	public WebToken getToken(WsConnectContext ctx) {
		var a = ctx.header("Authorization");

		if (a != null && a.startsWith("Bearer ")) {
			var token = a.substring(7);

			if (token.equals(selfToken.token)) {
				return selfToken;
			}

			return getToken(token);
		}

		var c = ctx.cookie("gnometoken");

		if (c != null && !c.isEmpty()) {
			if (c.equals(selfToken.token)) {
				return selfToken;
			}

			return getToken(c);
		}

		// WS don't need to handle query param login
		return null;
	}

	@Nullable
	public WebToken getToken(String token) {
		return webTokensDB.findFirst(token);
	}

	public void invalidateToken(long user) {
		webTokensDB.query().eq("user", user).many().delete();
	}

	public void invalidateAllTokens() {
		webTokensDB.drop();
	}

	public synchronized void loadMacro(Macro macro) {
		allMacros.put(macro.id.getAsInt(), macro);
	}

	public synchronized void findMacroId(Macro macro) {
		var key = macro.id.getAsInt();

		while (key == 0 || allMacros.containsKey(key)) {
			key = MathUtils.RANDOM.nextInt();
		}

		macro.id = HexId32.of(key);
		allMacros.put(key, macro);
	}

	public synchronized ChannelSettings channelSettings(long channelId) {
		return channelSettings.computeIfAbsent(channelId, id -> new ChannelSettings(channelSettingsDB, MapWrapper.wrap(new Document("_id", id))));
	}
}