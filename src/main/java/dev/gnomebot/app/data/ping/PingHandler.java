package dev.gnomebot.app.data.ping;

import dev.gnomebot.app.App;
import dev.gnomebot.app.Config;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.discord.DM;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class PingHandler {
	private record UserPingDestination(Snowflake userId, PingDestination dst) implements PingDestination {
		@Override
		public void relayPing(PingData data) {
			// FIXME: Check permissions
			dst.relayPing(data);
		}
	}

	private record DMDestination(Snowflake userId) implements PingDestination {
		@Override
		public void relayPing(PingData data) {
			DM.send(data.gc().db.app.discordHandler, data.user(), MessageBuilder.create(data.content()), false);
		}
	}

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
				return new DMDestination(userId);
			} else {
				UserWebhook userWebhook = db.userWebhooks.query().eq("user", userId.asLong()).eq("name", s[1]).first();

				if (userWebhook != null) {
					return new UserPingDestination(userId, userWebhook.createWebhook());
				}
			}
		} catch (Exception e) {
		}

		return PingDestination.NONE;
	}

	public synchronized UserPingInstance[] getPings() {
		if (userPingInstances == null) {
			Map<String, PingDestination> destinations = new HashMap<>();
			ArrayList<UserPingInstance> list = new ArrayList<>();

			WebHook gnomePingsWebHook = Config.get().gnome_mention_webhook;

			if (gnomePingsWebHook.id.asLong() != 0L) {
				list.add(new UserPingInstance(new Ping[]{new Ping(Pattern.compile("gnom|" + db.app.discordHandler.selfId.asString(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE), true)}, db.app.discordHandler.selfId, gnomePingsWebHook, UserPingConfig.DEFAULT));
			}

			for (UserPings pings : db.userPings.query()) {
				Snowflake userId = pings.getUIDSnowflake();

				try {
					for (UserPings.PingBuilder builder : pings.createBuilders()) {
						for (String s : builder.name.split(",")) {
							PingDestination destination = destinations.computeIfAbsent(userId.asString() + "/" + s.trim(), this::createDestination);

							if (destination != PingDestination.NONE) {
								list.add(builder.buildInstance(userId, destination));
							}
						}
					}
				} catch (Exception ex) {
				}
			}

			userPingInstances = list.toArray(new UserPingInstance[0]);
			App.info("Loaded " + list.size() + " user ping instances:");
			list.forEach(App::info);
		}

		return userPingInstances;
	}

	public void handle(GuildCollections gc, ChannelInfo channel, User user, String content, String url) {
		Snowflake userId = user.getId();
		String username = user.getUsername();
		String avatar = user.getAvatarUrl();
		boolean bot = user.isBot();
		PingData pingData = new PingData(gc, channel, user, userId, username, avatar, bot, content, url);

		for (UserPingInstance instance : getPings()) {
			instance.handle(pingData);
		}
	}
}
