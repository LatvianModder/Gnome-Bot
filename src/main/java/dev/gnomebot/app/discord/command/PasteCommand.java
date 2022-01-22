package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.Paste;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.discordjson.json.WebhookMessageEditRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class PasteCommand extends ApplicationCommands {
	public static final Pattern URL_REGEX = Pattern.compile("https://(?:media|cdn)\\.(?:discordapp|discord)\\.(?:com|net)/attachments/(\\d+)/(\\d+)/(\\S+)");

	@RootCommand
	public static final CommandBuilder COMMAND = root("paste")
			.description("Paste a file")
			.add(string("message_or_url").required())
			.run(PasteCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		String mu = event.get("message_or_url").asString();

		Matcher urlm = URL_REGEX.matcher(mu);

		if (urlm.find()) {
			event.acknowledge();

			Snowflake channelId = Snowflake.of(urlm.group(1));
			Snowflake attachmentId = Snowflake.of(urlm.group(2));
			String filename = urlm.group(3);

			Paste.createPaste(event.context.gc.db, channelId.asLong(), attachmentId.asLong(), filename);

			event.editInitial(WebhookMessageEditRequest.builder()
					.allowedMentionsOrNull(DiscordMessage.noMentions().toData())
					.contentOrNull("Paste version of `" + filename + "`")
					.addComponent(ActionRow.of(Button.link(Paste.getUrl(attachmentId.asString()), "View " + filename)).getData())
					.build()
			);

			return;
		}

		event.acknowledgeEphemeral();
		throw wip();
	}
}
