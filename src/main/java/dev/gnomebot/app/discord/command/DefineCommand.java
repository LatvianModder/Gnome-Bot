package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.webutils.CodingUtils;
import dev.latvian.apps.webutils.FormattingUtils;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;
import discord4j.core.object.reaction.ReactionEmoji;

import java.util.ArrayList;
import java.util.List;

public class DefineCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("define")
			.description("Prints dictionary definition of a word")
			.add(string("word").required())
			.run(DefineCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();

		try {
			var o = Utils.readInternalJson("api/info/define/" + CodingUtils.encodeURL(event.get("word").asString()));

			if (o.asBoolean("found")) {
				var builder = MessageBuilder.create();
				var word = o.asString("word");

				var embedBuilder = EmbedBuilder.create();
				embedBuilder.color(EmbedColor.GRAY);
				embedBuilder.title(word);

				var meanings = o.asArray("meanings");

				for (var i = 0; i < Math.min(25, meanings.size()); i++) {
					var o1 = meanings.asObject(i);

					var b = new StringBuilder("*");
					FormattingUtils.titleCase(b, o1.asString("definition"));
					b.append('*');

					if (!o1.asString("example").isEmpty()) {
						b.append("\n\n\"");
						FormattingUtils.titleCase(b, o1.asString("example"));
						b.append('"');
					}

					embedBuilder.field((i + 1) + ". " + o1.asString("type"), FormattingUtils.trim(b.toString(), 1024));
				}

				builder.addEmbed(embedBuilder);

				List<ActionComponent> list = new ArrayList<>();

				for (var o1 : o.asArray("phonetics").ofObjects()) {
					if (!o1.asString("audio_url").isEmpty()) {
						var url = o1.asString("audio_url");

						if (url.endsWith(".mp3")) {
							if (url.startsWith("https:https://")) {
								url = url.substring(6);
							}

							list.add(Button.link(url, ReactionEmoji.unicode("\uD83C\uDFB5"), o1.asString("text")));
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
