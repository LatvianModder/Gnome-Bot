package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.App;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.WebHook;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PingHandler {
	public final Databases db;
	private UserPingInstance[] userPingInstances;

	public PingHandler(Databases d) {
		db = d;
	}

	public synchronized void update() {
		userPingInstances = null;
	}

	private PingDestination createDestination(String id) {
		try {
			String[] s = id.split("/", 2);
			Snowflake userId = Snowflake.of(s[0]);

			if (s[1].equals("dm")) {
				try {
					return new DMDestination(userId, DM.openId(App.instance.discordHandler, userId));
				} catch (Exception ex) {
					return PingDestination.NONE;
				}
			} else {
				UserWebhook userWebhook = db.userWebhooks.query().eq("user", userId.asLong()).eq("name", s[1]).first();

				if (userWebhook != null) {
					return userWebhook.createWebhook();
				}
			}
		} catch (Exception e) {
		}

		return PingDestination.NONE;
	}

	public synchronized UserPingInstance[] getPings() {
		if (userPingInstances == null) {
			Map<String, PingDestination> destinationMap = new HashMap<>();
			List<PingDestination> destinations = new ArrayList<>();
			ArrayList<UserPingInstance> list = new ArrayList<>();

			WebHook gnomePingsWebHook = Config.get().gnome_mention_webhook;

			if (gnomePingsWebHook.id.asLong() != 0L) {
				list.add(new UserPingInstance(new Ping[]{new Ping(Pattern.compile("gnom|" + db.app.discordHandler.selfId.asString(), Pattern.CASE_INSENSITIVE), true)}, db.app.discordHandler.selfId, gnomePingsWebHook, UserPingConfig.DEFAULT));
			}

			for (UserPings pings : db.userPings.query()) {
				Snowflake userId = pings.getUIDSnowflake();

				try {
					for (UserPings.PingBuilder builder : pings.createBuilders(db, userId)) {
						for (String s : builder.name.split(",")) {
							PingDestination destination = destinationMap.computeIfAbsent(userId.asString() + "/" + s.trim(), this::createDestination);

							if (destination != PingDestination.NONE) {
								destinations.add(destination);
							}
						}

						if (!destinations.isEmpty()) {
							if (destinations.size() == 1) {
								list.add(builder.buildInstance(userId, new UserPingDestination(userId, destinations.get(0))));
							} else {
								list.add(builder.buildInstance(userId, new UserPingDestination(userId, new PingDestinationBundle(destinations.toArray(PingDestinationBundle.EMPTY_ARRAY)))));
							}

							destinations.clear();
						}
					}
				} catch (Exception ex) {
				}
			}

			userPingInstances = list.toArray(new UserPingInstance[0]);
			// App.info("Loaded " + list.size() + " user ping instances:");
			// list.forEach(App::info);
			App.LOGGER.refreshedPings();
		}

		return userPingInstances;
	}

	public void handle(GuildCollections gc, ChannelInfo channel, User user, String match, String content, String url) {
		Snowflake userId = user.getId();
		String username = user.getUsername();
		String avatar = user.getAvatarUrl();
		boolean bot = user.isBot();
		PingData pingData = new PingData(gc, channel, user, userId, username, avatar, bot, match, content, url);

		for (UserPingInstance instance : getPings()) {
			instance.handle(pingData);
		}
	}
}
