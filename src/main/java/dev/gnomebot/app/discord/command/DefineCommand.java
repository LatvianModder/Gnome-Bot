package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.CodingUtils;
import dev.latvian.apps.webutils.FormattingUtils;
import dev.latvian.apps.webutils.json.JSONObject;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class DefineCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("define")
			.description("Prints dictionary definition of a word")
			.add(string("word").required())
			.run(DefineCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();

		try {
			var o = Utils.readInternalJson("api/info/define/" + CodingUtils.encodeURL(event.get("word").asString()));

			if (o.bool("found")) {
				MessageBuilder builder = MessageBuilder.create();
				String word = o.string("word");

				EmbedBuilder embedBuilder = EmbedBuilder.create();
				embedBuilder.color(EmbedColor.GRAY);
				embedBuilder.title(word);

				var meanings = o.array("meanings");

				for (int i = 0; i < Math.min(25, meanings.size()); i++) {
					var o1 = meanings.object(i);

					StringBuilder b = new StringBuilder("*");
					FormattingUtils.titleCase(b, o1.string("definition"));
					b.append('*');

					if (!o1.string("example").isEmpty()) {
						b.append("\n\n\"");
						FormattingUtils.titleCase(b, o1.string("example"));
						b.append('"');
					}

					embedBuilder.field((i + 1) + ". " + o1.string("type"), FormattingUtils.trim(b.toString(), 1024));
				}

				builder.addEmbed(embedBuilder);

				List<ActionComponent> list = new ArrayList<>();

				for (var e : o.array("phonetics")) {
					var o1 = (JSONObject) e;

					if (!o1.string("audio_url").isEmpty()) {
						String url = o1.string("audio_url");

						if (url.endsWith(".mp3")) {
							if (url.startsWith("https:https://")) {
								url = url.substring(6);
							}

							list.add(Button.link(url, ReactionEmoji.unicode("\uD83C\uDFB5"), o1.string("text")));
						}
					}
				}

				if (!list.isEmpty()) {
					builder.addComponent(ActionRow.of(list));
				}

				event.respond(builder);
				return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		event.respond("No Definitions Found!");
	}
}
