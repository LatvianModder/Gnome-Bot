package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.discord.MessageHandler;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.AppTaskCancelledException;
import dev.gnomebot.app.util.Pair;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class ImportMessagesCommand {
	@LegacyDiscordCommand(name = "import_messages", help = "Imports messages from all channels into DB", arguments = "[channel|channel:message]", permissionLevel = AuthLevel.OWNER)
	public static final CommandCallback COMMAND = (context, reader) -> {
		List<Pair<ChannelInfo, Snowflake>> messageChannels = new ArrayList<>();

		Optional<Pair<ChannelInfo, Snowflake>> c;

		while ((c = reader.readChannelAndMessage()).isPresent()) {
			messageChannels.add(c.get());
		}

		if (messageChannels.isEmpty()) {
			for (ChannelInfo channel : context.gc.getChannelList()) {
				Snowflake lm = channel.getLastMessageId();

				if (lm != null) {
					messageChannels.add(Pair.of(channel, lm));
				}
			}
		}

		context.handler.app.queueBlockingTask(task -> {
			String channelNames = messageChannels.stream().map(mc -> mc.a.getMention()).collect(Collectors.joining(" "));
			App.info("Importing messages from " + messageChannels.stream().map(mc -> "#" + mc.a.getName() + ":" + mc.b.asString()).collect(Collectors.joining(" ")));
			context.reply("Importing messages from " + channelNames);

			int mId = 0;
			long now = System.currentTimeMillis();
			MemberCache memberCache = context.gc.createMemberCache();

			List<Pair<ChannelInfo, Snowflake>> channelsLeft = new ArrayList<>(messageChannels);

			for (Pair<ChannelInfo, Snowflake> pair : messageChannels) {
				ChannelInfo ch = pair.a;
				Snowflake lastId = pair.b;

				long nowChannel = System.currentTimeMillis();

				try {
					for (Message message : ch.getMessagesBefore(lastId)
							.delayElements(Duration.ofMillis(10))
							.onErrorContinue((throwable, o) -> App.info("Error! " + o + ": " + throwable))
							.filter(m -> m.getType() == Message.Type.DEFAULT && m.getAuthor().isPresent())
							.toIterable()) {
						mId++;
						lastId = message.getId();
						User user = message.getAuthor().get();
						MessageHandler.messageCreated(context.handler, ch, message, user, memberCache.getAndUpdate(user).orElse(null), true);

						if (task.cancelled) {
							throw new AppTaskCancelledException();
						}
					}

					context.reply("Imported " + ch.getMention() + " in " + Utils.prettyTimeString((System.currentTimeMillis() - nowChannel) / 1000L));
					channelsLeft.remove(pair);
				} catch (AppTaskCancelledException ex) {
					context.reply("Imported " + ch.getMention() + " in " + Utils.prettyTimeString((System.currentTimeMillis() - nowChannel) / 1000L));
					channelsLeft.remove(pair);
					channelsLeft.add(0, Pair.of(ch, lastId));
					break;
				} catch (Exception ex) {
					App.error("Error! " + ch.getName() + ": " + ex);
					context.reply("Can't read " + ch.getMention() + ", skipping");
				}
			}

			int count = mId;
			long totalTime = (System.currentTimeMillis() - now) / 1000L;
			context.reply("Imported " + count + " messages from " + channelNames + " from " + memberCache.getCacheSize() + " members in " + Utils.prettyTimeString(totalTime) + " @ " + (int) (count / (double) totalTime) + " m/s");

			if (task.cancelled) {
				context.reply("Importing was cancelled! To continue run\n```" + context.gc.prefix + "import_messages " + channelsLeft.stream().map(ch -> ch.a.id.asString() + ":" + ch.b.asString()).collect(Collectors.joining(" ")) + "```");
			}
		});
	};
}
