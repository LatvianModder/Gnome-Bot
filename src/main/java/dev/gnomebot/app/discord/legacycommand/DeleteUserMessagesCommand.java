package dev.gnomebot.app.discord.legacycommand;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.common.util.Snowflake;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DeleteUserMessagesCommand {
	@LegacyDiscordCommand(name = "delete_user_messages", help = "Deletes user messages", arguments = "<user> [time of how far back]", permissionLevel = AuthLevel.ADMIN)
	public static final CommandCallback COMMAND = (context, reader) -> {
		var user = reader.readUser().get();
		var time = reader.readSeconds().orElse(300L);

		if (time > 7.884e+6) {
			throw new GnomeException("Can't delete messages older than 3 months!");
		}

		context.handler.app.queueBlockingTask(task -> {
			context.message.addReaction(Emojis.VOTENONE).block();
			List<Bson> filters = new ArrayList<>();
			filters.add(Filters.eq("user", user.getId().asLong()));
			filters.add(Filters.gte("timestamp", new Date(System.currentTimeMillis() - time * 1000L)));
			var count = 0;

			for (var m : context.gc.messages.query().filters(filters)) {
				if (task.cancelled) {
					break;
				} else if (m.getUID() == context.message.getId().asLong()) {
					continue;
				}

				count++;

				try {
					App.info("Deleting " + Snowflake.of(m.getUID()).asString());
					var channel = context.gc.getChannelInfo(Snowflake.of(m.getChannelID()));
					channel.getMessage(Snowflake.of(m.getUID())).delete().block();
				} catch (Exception ex) {
					App.info("Message not found!");
				}
			}

			context.upvote();
			context.message.removeSelfReaction(Emojis.VOTENONE).block();
			App.info("Deleted " + count + " messages from " + user.getUsername());
		});
	};
}
