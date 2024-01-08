package dev.gnomebot.app.discord.command;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.DiscordMember;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.TimeUtils;
import dev.latvian.apps.webutils.data.MutableLong;
import discord4j.common.util.Snowflake;
import discord4j.discordjson.json.MessageData;
import discord4j.rest.service.ChannelService;
import org.bson.Document;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ChannelCommands extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("channel")
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

	private static void listXp(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();

		var channelsWithXp = event.context.gc.getChannelList().stream().filter(ChannelInfo::isXpSet).toList();

		StringBuilder sb = new StringBuilder();
		sb.append("Channel XP:");

		for (ChannelInfo ch : channelsWithXp) {
			if (ch.canViewChannel(event.context.sender.getId())) {
				sb.append("\n<#").append(ch.id.asString()).append(">: ").append(ch.getXp());
			}
		}

		event.respond(sb.toString());
	}

	private static void getXp(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		ChannelInfo info = event.get("channel").asChannelInfoOrCurrent();

		if (info == null) {
			throw new GnomeException("Invalid channel!");
		}

		event.respond(info.getMention() + " XP: " + info.settings.xp + " (Actual: " + info.getXp() + ")");
	}

	private static void setXp(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		ChannelInfo info = event.get("channel").asChannelInfoOrCurrent();

		if (info == null) {
			throw new GnomeException("Invalid channel!");
		}

		int xp = Math.max(-1, event.get("xp").asInt());

		info.settings.xp = xp;
		info.settings.update("xp", xp);

		event.respond(info.getMention() + " XP set to " + info.settings.xp + " (Actual: " + info.getXp() + ")");
	}

	private static void setAllXp(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		int xp = Math.max(0, event.get("xp").asInt());
		event.context.gc.globalXp.set(xp);
		event.respond("All channel XP set to " + xp);
	}

	@SuppressWarnings("deprecation")
	private static void refreshXp(ChatInputInteractionEventWrapper event) throws Exception {
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

				totalCount.computeIfAbsent(key.user, MutableLong.MAP_VALUE).add(1L);
				dailyCount.computeIfAbsent(key, MutableLong.MAP_VALUE).add(1L);

				var xp = channelInfo.getXp();

				if (xp > 0L) {
					totalXp.computeIfAbsent(key.user, MutableLong.MAP_VALUE).add(xp);
					dailyXp.computeIfAbsent(key, MutableLong.MAP_VALUE).add(xp);
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
			long c = MutableLong.valueOf(totalCount.get(member.getUID()), 0L);
			long xp = MutableLong.valueOf(totalXp.get(member.getUID()), 0L);

			if (c != member.getTotalMessages() && xp != member.getTotalXp()) {
				member.update(Updates.combine(Updates.set("total_messages", c), Updates.set("total_xp", xp)));
			} else if (c != member.getTotalMessages()) {
				member.update("total_messages", c);
			} else if (xp != member.getTotalXp()) {
				member.update("total_xp", xp);
			}
		}

		long time = (System.currentTimeMillis() - start) / 1000L;
		event.edit().respond("XP refreshed from " + total + " messages in " + TimeUtils.prettyTimeString(time) + "!");
	}

	private static void autoThreads(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var ci = event.get("channel").asChannelInfoOrCurrent();
		var enabled = event.get("enabled").asBoolean().orElse(null);

		if (enabled == null) {
			event.respond("Auto-threading in " + ci.getMention() + " is " + (ci.settings.autoThread ? "enabled" : "disabled"));
		} else {
			ci.settings.autoThread = enabled;
			ci.settings.update("auto_thread", ci.settings.autoThread);
			event.respond("Auto-threading in " + ci.getMention() + " has been " + (ci.settings.autoThread ? "enabled" : "disabled"));
		}
	}

	private static void autoUpvote(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();

		var ci = event.get("channel").asChannelInfoOrCurrent();
		var enabled = event.get("enabled").asBoolean().orElse(null);

		if (enabled == null) {
			event.respond("Auto-upvoting in " + ci.getMention() + " is " + (ci.settings.autoUpvote ? "enabled" : "disabled"));
		} else {
			ci.settings.autoUpvote = enabled;
			ci.settings.update("auto_upvote", ci.settings.autoUpvote);
			event.respond("Auto-upvoting in " + ci.getMention() + " has been " + (ci.settings.autoUpvote ? "enabled" : "disabled"));
		}
	}

	private static void threadTitle(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		throw wip();
	}

	private static void downloadAllImages(ChatInputInteractionEventWrapper event) throws Exception {
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

	private static void deleteMessagesAfter(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		throw wip();
	}

	private static void deleteMessagesBefore(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		event.context.checkSenderAdmin();
		throw wip();
	}

	private static void deleteMessagesBetween(ChatInputInteractionEventWrapper event) throws Exception {
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