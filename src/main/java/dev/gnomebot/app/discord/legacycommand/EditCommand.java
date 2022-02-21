package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.server.AuthLevel;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;

/**
 * @author LatvianModder
 */
public class EditCommand {
	@LegacyDiscordCommand(name = "edit", help = "Edits a message", arguments = "<id> [message]", permissionLevel = AuthLevel.OWNER)
	public static final CommandCallback COMMAND = (context, reader) -> {
		Message m = context.findMessage(Snowflake.of(reader.readLong().orElse(0L))).orElse(null);

		if (m == null) {
			throw new GnomeException("Message not found!");
		}

		String t = reader.readRemainingString().orElse("");

		if (t.isEmpty()) {
			context.reply(context.gc.prefix + "edit " + m.getId().asString() + "\n\\`\\`\\`\n```\n" + m.getContent()
					.replaceAll("<@&(\\d+)>", "role:$1")
					.replaceAll("<@(\\d+)>", "user:$1")
					.replaceAll("<#(\\d+)>", "channel:$1")
					+ "\n```\\`\\`\\`");
		} else if (t.length() > 1 && t.startsWith("```\n") && t.endsWith("\n```")) {
			String c = t.substring(4, t.length() - 4)
					.replaceAll("role:(\\d+)", "<@&$1>")
					.replaceAll("user:(\\d+)", "<@$1>")
					.replaceAll("channel:(\\d+)", "<#$1>");
			m.edit(MessageEditSpec.builder()
					.contentOrNull(c)
					.allowedMentionsOrNull(DiscordMessage.noMentions())
					.build()
			).block();

			context.upvote();
		} else {
			throw new GnomeException("Message must be written in a code block!");
		}
	};
}
