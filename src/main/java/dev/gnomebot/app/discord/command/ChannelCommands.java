package dev.gnomebot.app.discord.command;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.MutableLong;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.service.ChannelService;
import discord4j.rest.util.Permission;
import org.bson.Document;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class ChannelCommands extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("channel")
			.description("Channel stuff")
			.add(subGroup("xp")
					.description("Check and update channel XP")
					.add(sub("list")
							.description("Lists all channel XP")
							.run(ChannelCommands::listXp)
					)
					.add(sub("get")
							.description("Prints channel XP")
							.add(channel("channel"))
							.run(ChannelCommands::getXp)
					)
					.add(sub("set")
							.description("Updates channel XP")
							.add(integer("xp").required())
							.add(channel("channel"))
							.run(ChannelCommands::setXp)
					)
					.add(sub("set_all")
							.description("Updates all channel XP")
							.add(integer("xp").required())
							.run(ChannelCommands::setAllXp)
					)
					.add(sub("refresh")
							.description("This will adjust everyone's XP. Run this after you've tweaked xp in channels")
							.run(ChannelCommands::refreshXp)
					)
			)
			.add(subGroup("auto")
					.add(sub("upvote")
							.description("Check or set auto-upvoting messages in a channel")
							.add(bool("enabled"))
							.add(channel("channel"))
							.run(ChannelCommands::autoUpvote)
					)
					.add(sub("threads")
							.description("Check or set auto-threading messages in a channel")
							.add(bool("enabled"))
							.add(channel("channel"))
							.run(ChannelCommands::autoThreads)
					)
			)
			.add(sub("thread_title")
					.description("Lets original message poster change title of thread")
					.add(string("title").required())
					.run(ChannelCommands::threadTitle)
			)
			.add(sub("download_all_images")
					.description("Downloads all images in current channel as .zip")
					.add(channel("channel"))
					.run(ChannelCommands::downloadAllImages)
			)
			.add(sub("delete_messages_after")
					.description("Deletes messages after a message ID")
					.add(string("message_id").required())
					.add(channel("channel"))
					.run(ChannelCommands::deleteMessagesAfter)
			)
			.add(sub("delete_messages_before")
					.description("Deletes messages before a message ID")
					.add(string("message_id").required())
					.add(integer("count").required())
					.add(channel("channel"))
					.run(ChannelCommands::deleteMessagesBefore)
			)
			.add(sub("delete_messages_between")
					.description("Deletes messages between two message IDs")
					.add(string("message_id_1").required())
					.add(string("message_id_2").required())
					.add(channel("channel"))
					.run(ChannelCommands::deleteMessagesBetween)
			);

	private static void listXp(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();

		List<ChannelInfo> channelsWithXp = event.context.gc.getChannelList().stream().filter(s -> s.xp > 0L).collect(Collectors.toList());

		StringBuilder sb = new StringBuilder();
		sb.append("Channel XP:");

		for (ChannelInfo ch : channelsWithXp) {
			if (ch.getPermissions(event.context.sender.getId()).contains(Permission.VIEW_CHANNEL)) {
				sb.append("\n<#").append(Snowflake.asString(ch.getUID())).append(">: ").append(ch.xp);
			}
		}

		event.respond(sb.toString());
	}

	private static void getXp(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		ChannelInfo info = event.get("channel").asChannelInfoOrCurrent();

		if (info == null) {
			throw new GnomeException("Invalid channel!");
		}

		event.respond(info.getMention() + " XP: " + info.xp);
	}

	private static void setXp(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		ChannelInfo info = event.get("channel").asChannelInfoOrCurrent();

		if (info == null) {
			throw new GnomeException("Invalid channel!");
		}

		long xp = Math.max(0L, event.get("xp").asLong());

		info.xp = xp;
		info.update("xp", xp);

		event.respond(info.getMention() + " XP set to " + info.xp);
	}

	private static void setAllXp(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		long xp = Math.max(0L, event.get("xp").asLong());

		for (ChannelInfo info : event.context.gc.getChannelList()) {
			info.xp = xp;
			info.update("xp", xp);
		}

		event.respond("All channel XP set to " + xp);
	}

	@SuppressWarnings("deprecation")
	private static void refreshXp(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		final class StatKey {
			public Date date;
			public long channel;
			public long user;

			@Override
			public boolean equals(Object o) {
				if (this == o) {
					return true;
				}
				if (o == null || getClass() != o.getClass()) {
					return false;
				}

				StatKey statKey = (StatKey) o;
				return channel == statKey.channel && user == statKey.user && date.getTime() == statKey.date.getTime();
			}

			@Override
			public int hashCode() {
				return Objects.hash(date, channel, user);
			}
		}

		long start = System.currentTimeMillis();
		long total = 0L;
		Map<Long, MutableLong> totalCount = new HashMap<>();
		Map<Long, MutableLong> totalXp = new HashMap<>();
		Map<StatKey, MutableLong> dailyCount = new HashMap<>();
		Map<StatKey, MutableLong> dailyXp = new HashMap<>();

		for (ChannelInfo channelInfo : event.context.gc.getChannelList()) {
			event.edit().respond("1/4 Counting messages in " + channelInfo.getMention() + "...");
			long channelId = channelInfo.id.asLong();

			for (DiscordMessage m : event.context.gc.messages.query().eq("channel", channelId).projectionFields("timestamp", "user")) {
				total++;
				StatKey key = new StatKey();
				Date date = m.getDate();
				key.date = new Date(date.getYear(), date.getMonth(), date.getDate());
				key.channel = channelId;
				key.user = m.getUserID();

				totalCount.computeIfAbsent(key.user, MutableLong::new).add(1L);
				dailyCount.computeIfAbsent(key, MutableLong::new).add(1L);

				if (channelInfo.xp > 0L) {
					totalXp.computeIfAbsent(key.user, MutableLong::new).add(channelInfo.xp);
					dailyXp.computeIfAbsent(key, MutableLong::new).add(channelInfo.xp);
				}
			}
		}

		event.context.gc.messageCount.drop();
		event.context.gc.messageXp.drop();

		event.edit().respond("2/4 Updating message counters...");

		for (Map.Entry<StatKey, MutableLong> entry : dailyCount.entrySet()) {
			Document doc = new Document();
			doc.put("date", entry.getKey().date);
			doc.put("channel", entry.getKey().channel);
			doc.put("user", entry.getKey().user);
			doc.put("count", entry.getValue().value);
			event.context.gc.messageCount.insert(doc);
		}

		event.edit().respond("3/4 Updating message xp...");

		for (Map.Entry<StatKey, MutableLong> entry : dailyXp.entrySet()) {
			Document doc = new Document();
			doc.put("date", entry.getKey().date);
			doc.put("channel", entry.getKey().channel);
			doc.put("user", entry.getKey().user);
			doc.put("xp", entry.getValue().value);
			event.context.gc.messageXp.insert(doc);
		}

		event.edit().respond("4/4 Updating user data...");

		for (DiscordMember member : event.context.gc.members.query()) {
			long c = MutableLong.valueOf(totalCount.get(member.getUID()));
			long xp = MutableLong.valueOf(totalXp.get(member.getUID()));

			if (c != member.getTotalMessages() && xp != member.getTotalXp()) {
				member.update(Updates.combine(Updates.set("total_messages", c), Updates.set("total_xp", xp)));
			} else if (c != member.getTotalMessages()) {
				member.update("total_messages", c);
			} else if (xp != member.getTotalXp()) {
				member.update("total_xp", xp);
			}
		}

		long time = (System.currentTimeMillis() - start) / 1000L;
		event.edit().respond("XP refreshed from " + total + " messages in " + Utils.prettyTimeString(time) + "!");
	}

	private static void autoThreads(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		ChannelInfo settings = event.get("channel").asChannelInfoOrCurrent();
		Boolean enabled = event.get("enabled").asBoolean().orElse(null);

		if (enabled == null) {
			event.respond("Auto-threading in " + settings.getMention() + " is " + (settings.autoThread ? "enabled" : "disabled"));
		} else {
			settings.autoThread = enabled;
			settings.update("auto_thread", settings.autoThread);
			event.respond("Auto-threading in " + settings.getMention() + " has been " + (settings.autoThread ? "enabled" : "disabled"));
		}
	}

	private static void autoUpvote(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		ChannelInfo settings = event.get("channel").asChannelInfoOrCurrent();
		Boolean enabled = event.get("enabled").asBoolean().orElse(null);

		if (enabled == null) {
			event.respond("Auto-upvoting in " + settings.getMention() + " is " + (settings.autoUpvote ? "enabled" : "disabled"));
		} else {
			settings.autoUpvote = enabled;
			settings.update("auto_upvote", settings.autoUpvote);
			event.respond("Auto-upvoting in " + settings.getMention() + " has been " + (settings.autoUpvote ? "enabled" : "disabled"));
		}
	}

	private static void threadTitle(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledge();
		throw wip();
	}

	private static void downloadAllImages(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		class ImageFile {
			public String originalFilename;
			public String ext;
			public String filename;
			public String url;
			public String author;
			public Instant timestamp;
		}

		throw wip();
	}

	private static void deleteMessagesAfter(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		throw wip();
	}

	private static void deleteMessagesBefore(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		throw wip();
	}

	private static void deleteMessagesBetween(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		ChannelInfo channel = event.get("channel").asChannelInfoOrCurrent();
		Snowflake m1 = event.get("message_id_1").asSnowflake();
		Snowflake m2 = event.get("message_id_2").asSnowflake();

		long oldest = Utils.oldest(m1, m2).asLong();
		long newest = Utils.newest(m1, m2).asLong();

		event.context.handler.app.queueBlockingTask(task -> {
			try {
				ChannelService service = event.context.handler.client.getRestClient().getChannelService();
				// event.context.message.addReaction(Emojis.VOTENONE).block();

				// FIXME: Change to bulk deletion
				Iterable<MessageData> iterable = channel.getRest().getMessagesBefore(Snowflake.of(newest)).take(1000L, true).toIterable();

				for (MessageData message : iterable) {
					long id = message.id().asLong();

					if (task.cancelled || id == oldest) {
						break;
					}

					App.info("Deleting " + id);
					service.deleteMessage(channel.id.asLong(), id, null).block();
				}

				event.edit().respond("Done!");
			} catch (Exception ex) {
				event.edit().respond("Error! " + ex);
			}
		});
	}
}