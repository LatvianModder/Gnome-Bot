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
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.data.HexId32;
import dev.latvian.apps.webutils.math.MathUtils;
import discord4j.common.util.Snowflake;
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
	public final Map<Snowflake, GuildCollections> guildCollections;
	public final Map<Integer, Macro> allMacros;

	public final WrappedCollection<WebLogEntry> webLog;
	private final WrappedCollection<WebToken> webTokens;
	public final WrappedCollection<BasicDocument> mmShowcase;
	public final WrappedCollection<UserWebhook> userWebhooks;
	public final WrappedCollection<Paste> pastes;
	public final WrappedCollection<InteractionDocument> interactions;
	public final WrappedCollection<ScheduledTask> scheduledTasks;
	public final WrappedCollection<DiscordPoll> polls;
	public final WrappedCollection<ChannelSettings> channelSettings;

	public WebToken selfToken;

	public Databases(App m) {
		app = m;

		mongoClient = MongoClients.create(Config.get().db_uri);
		database = mongoClient.getDatabase("gnomebot");

		collections = new LinkedHashMap<>();
		guildCollections = new HashMap<>();
		allMacros = new HashMap<>();

		webLog = create(database, "web_log", WebLogEntry::new).expiresAfterMonth("timestamp_expire", "timestamp", null);
		webTokens = create(database, "web_tokens", WebToken::new);
		mmShowcase = create(database, "mm_showcase", BasicDocument::new);
		userWebhooks = create(database, "user_webhooks", UserWebhook::new);
		pastes = create(database, "pastes", Paste::new);
		interactions = create(database, "interactions", InteractionDocument::new);
		scheduledTasks = create(database, "scheduled_tasks", ScheduledTask::new);
		polls = create(database, "polls", DiscordPoll::new);
		channelSettings = create(database, "channel_settings", ChannelSettings::new);

		for (var task : scheduledTasks.query()) {
			app.scheduledTasks.add(task);
		}

		try {
			for (var path : Files.list(AppPaths.GUILD_DATA).filter(Files::isDirectory).toList()) {
				var dirName = path.getFileName().toString();
				var id = GuildPaths.INVERTED_CUSTOM_NAMES.get().getOrDefault(dirName, Utils.snowflake(dirName));

				if (id.asLong() != 0L) {
					guild(id);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
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

	public Collection<GuildCollections> allGuilds() {
		return guildCollections.values();
	}

	public <T extends WrappedDocument<T>> WrappedCollection<T> create(MongoDatabase database, String ci, BiFunction<WrappedCollection<T>, MapWrapper, T> w) {
		WrappedCollection<T> collection = new WrappedCollection<>(this, database, ci, w);
		collections.put(ci, collection);
		return collection;
	}

	public void createSelfToken() {
		Document tokenDoc = new Document();
		tokenDoc.put("_id", Utils.createToken());
		tokenDoc.put("user", app.discordHandler.selfId.asLong());
		tokenDoc.put("name", "GnomeBot");
		tokenDoc.put("created", new Date());
		selfToken = new WebToken(webTokens, MapWrapper.wrap(tokenDoc));
	}

	public String getEncodedToken(long user, String name) {
		WebToken token = webTokens.query().eq("user", user).first();

		if (token == null) {
			String tokenString = Utils.createToken();
			Document document = new Document();
			document.put("_id", tokenString);
			document.put("user", user);
			document.put("name", name);
			document.put("created", new Date());
			webTokens.insert(document);
			return Base64.getUrlEncoder().encodeToString(tokenString.getBytes(StandardCharsets.UTF_8));
		} else {
			token.update(Updates.unset("guilds"));
			return Base64.getUrlEncoder().encodeToString(token.token.getBytes(StandardCharsets.UTF_8));
		}
	}

	@Nullable
	public WebToken getToken(Context ctx) {
		String a = ctx.header("Authorization");

		if (a != null && a.startsWith("Bearer ")) {
			String token = a.substring(7);

			if (token.equals(selfToken.token)) {
				return selfToken;
			}

			return getToken(token);
		}

		String c = ctx.cookie("gnometoken");

		if (c != null && !c.isEmpty()) {
			if (c.equals(selfToken.token)) {
				return selfToken;
			}

			return getToken(c);
		}

		String q = ctx.queryParam("logintoken");

		if (q != null && !q.isEmpty()) {
			String token = new String(Base64.getUrlDecoder().decode(q), StandardCharsets.UTF_8);
			WebToken webToken = getToken(token);

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
		String a = ctx.header("Authorization");

		if (a != null && a.startsWith("Bearer ")) {
			String token = a.substring(7);

			if (token.equals(selfToken.token)) {
				return selfToken;
			}

			return getToken(token);
		}

		String c = ctx.cookie("gnometoken");

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
		return webTokens.findFirst(token);
	}

	public void invalidateToken(long user) {
		webTokens.query().eq("user", user).many().delete();
	}

	public void invalidateAllTokens() {
		webTokens.drop();
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
}