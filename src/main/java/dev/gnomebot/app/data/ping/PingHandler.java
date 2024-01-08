package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.BrainEvents;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;

public class PingHandler implements Function<PingHandler.TargetDestinationKey, PingDestination> {
	public record TargetDestinationKey(Snowflake targetId, String id) {
	}

	public final Databases db;
	private UserPingInstance[] userPingInstances;

	public PingHandler(Databases d) {
		db = d;
	}

	public synchronized void update() {
		userPingInstances = null;
	}

	@Override
	public PingDestination apply(TargetDestinationKey key) {
		if (key.id.equals("dm")) {
			return DMDestination.INSTANCE;
		}

		try {
			var userWebhook = db.userWebhooks.query().eq("user", key.targetId.asLong()).eq("name", key.id).first();

			if (userWebhook != null) {
				return userWebhook.createWebhook();
			}
		} catch (Exception ignored) {
		}

		return PingDestination.NONE;
	}

	public synchronized UserPingInstance[] getPings() {
		if (userPingInstances == null) {
			long start = System.currentTimeMillis();
			var destinationMap = new HashMap<TargetDestinationKey, PingDestination>();
			var destinations = new ArrayList<PingDestination>();
			var list = new ArrayList<UserPingInstance>();

			var gnomePingsWebHook = Config.get().gnome_mention_webhook;

			if (gnomePingsWebHook.id.asLong() != 0L) {
				list.add(new UserPingInstance(new Ping[]{new Ping(Pattern.compile("gnom|" + db.app.discordHandler.selfId.asString(), Pattern.CASE_INSENSITIVE), true)}, Utils.NO_SNOWFLAKE, gnomePingsWebHook, UserPingConfig.DEFAULT));
			}

			try {
				var pattern = Pattern.compile("^\\d+\\.txt$");
				var pingFiles = Files.list(AppPaths.PINGS).filter(Files::isRegularFile).filter(f -> pattern.matcher(f.getFileName().toString()).find()).toList();

				var futures = new ArrayList<CompletableFuture<Pair<Snowflake, List<PingBuilder>>>>(pingFiles.size());

				for (var path : pingFiles) {
					futures.add(CompletableFuture.supplyAsync(() -> {
						var userId = Utils.snowflake(path.getFileName().toString().replace(".txt", ""));

						try {
							return Pair.of(userId, PingBuilder.compile(db, userId, Files.readString(path).trim(), false));
						} catch (Exception ex) {
							if (ex.getMessage().startsWith("You must message ")) {
								App.warn(db.app.discordHandler.getUserName(userId) + " / " + userId.asString() + " needs to DM Gnome");
							} else {
								App.warn(userId.asString() + " pings were misconfigured");
								ex.printStackTrace();
							}

							return Pair.of(userId, List.of());
						}
					}));
				}

				CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

				for (var future : futures) {
					var pair = future.get();
					var userId = pair.a();

					for (var builder : pair.b()) {
						for (var s : builder.name.split(",")) {
							var dst = destinationMap.computeIfAbsent(new TargetDestinationKey(userId, s.trim()), this);

							if (dst != PingDestination.NONE) {
								destinations.add(dst);
							}
						}

						if (!destinations.isEmpty()) {
							if (destinations.size() == 1) {
								list.add(builder.buildInstance(userId, destinations.get(0)));
							} else {
								list.add(builder.buildInstance(userId, new PingDestinationBundle(destinations.toArray(PingDestinationBundle.EMPTY_ARRAY))));
							}

							destinations.clear();
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			userPingInstances = list.toArray(new UserPingInstance[0]);
			App.LOGGER.event(BrainEvents.REFRESHED_PINGS);
			App.info("Loaded " + userPingInstances.length + " ping instances in " + (System.currentTimeMillis() - start) + " ms");
		}

		return userPingInstances;
	}

	public void handle(GuildCollections gc, ChannelInfo channel, User user, String match, String content, String url) {
		var userId = user.getId();
		var username = user.getUsername();
		var avatar = user.getAvatarUrl();
		var bot = user.isBot();
		var pingData = new PingData(gc, channel, user, userId, username, avatar, bot, match, content, url);

		CompletableFuture.runAsync(() -> {
			for (var instance : getPings()) {
				instance.handle(pingData);
			}
		});
	}
}
