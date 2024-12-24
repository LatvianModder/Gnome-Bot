package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.channel.ChannelInfo;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.core.object.entity.User;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.regex.Pattern;

public class PingHandler implements Function<PingHandler.TargetDestinationKey, PingDestination> {
	public record TargetDestinationKey(long targetId, String id) {
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
			var userWebhook = db.userWebhooksDB.query().eq("user", key.targetId).eq("name", key.id).first();

			if (userWebhook != null) {
				return userWebhook.createWebhook();
			}
		} catch (Exception ignored) {
		}

		return PingDestination.NONE;
	}

	public synchronized UserPingInstance[] getPings() {
		if (userPingInstances == null) {
			var start = System.currentTimeMillis();
			var destinationMap = new HashMap<TargetDestinationKey, PingDestination>();
			var destinations = new ArrayList<PingDestination>();
			var list = new ArrayList<UserPingInstance>();

			var gnomePingsWebHook = db.app.config.discord.gnome_mention_webhook;

			if (gnomePingsWebHook.id != 0L) {
				list.add(new UserPingInstance(new Ping[]{new Ping(Pattern.compile("gnom|" + db.app.discordHandler.selfId, Pattern.CASE_INSENSITIVE), true)}, 0L, gnomePingsWebHook, UserPingConfig.DEFAULT));
			}

			try {
				var pattern = Pattern.compile("^\\d+\\.txt$");
				var pingFiles = Files.list(AppPaths.PINGS).filter(Files::isRegularFile).filter(f -> pattern.matcher(f.getFileName().toString()).find()).toList();

				var futures = new ArrayList<CompletableFuture<Pair<Long, List<PingBuilder>>>>(pingFiles.size());

				for (var path : pingFiles) {
					futures.add(CompletableFuture.supplyAsync(() -> {
						var userId = SnowFlake.num(path.getFileName().toString().replace(".txt", ""));

						try {
							return Pair.of(userId, PingBuilder.compile(db, userId, Files.readString(path).trim(), false));
						} catch (Exception ex) {
							if (ex.getMessage().startsWith("You must message ")) {
								Log.warn(db.app.discordHandler.getUserName(userId) + " / " + userId + " needs to DM Gnome");
							} else {
								Log.warn(userId + " pings were misconfigured: " + ex);
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
								list.add(builder.buildInstance(userId, destinations.getFirst()));
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
			Log.info("Loaded " + userPingInstances.length + " ping instances in " + (System.currentTimeMillis() - start) + " ms");
		}

		return userPingInstances;
	}

	public void handle(GuildCollections gc, ChannelInfo channel, User user, String match, String content, String url) {
		var userId = user.getId().asLong();
		var username = user.getUsername();
		var avatar = user.getAvatarUrl();
		var bot = user.isBot();
		var pingData = new PingData(gc, channel, user, userId, username, avatar, bot, match, content, url);

		Thread.startVirtualThread(() -> {
			for (var instance : getPings()) {
				instance.handle(pingData);
			}
		});
	}
}
