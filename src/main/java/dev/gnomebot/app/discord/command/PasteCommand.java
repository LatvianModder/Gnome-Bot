package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.Button;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PasteCommand extends ApplicationCommands {
	public static final Pattern URL_REGEX = Pattern.compile("https://(?:media|cdn)\\.(?:discordapp|discord)\\.(?:com|net)/attachments/(\\d+)/(\\d+)/(\\S+)");

	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("paste")
			.description("Paste a file")
			.add(string("message_or_url").required())
			.run(PasteCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		String mu = event.get("message_or_url").asString();

		Matcher urlm = URL_REGEX.matcher(mu);

		if (urlm.find()) {
			event.acknowledge();

			Snowflake channelId = Snowflake.of(urlm.group(1));
			Snowflake attachmentId = Snowflake.of(urlm.group(2));
			String filename = urlm.group(3);

			Paste.createPaste(event.context.gc.db, channelId.asLong(), attachmentId.asLong(), filename, "");
			event.edit().respond(MessageBuilder.create("Paste version of `" + filename + "`").addComponentRow(Button.link(Paste.getUrl(attachmentId.asString()), "View " + filename)));
			return;
		}

		event.acknowledgeEphemeral();
		throw wip();
	}
}
