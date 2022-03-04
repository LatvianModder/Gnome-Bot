package dev.gnomebot.app.discord.command;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
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
			JsonObject o = Utils.readInternalJson("api/info/define/" + Utils.encode(event.get("word").asString())).getAsJsonObject();

			if (o.get("found").getAsBoolean()) {
				MessageBuilder builder = MessageBuilder.create();
				String word = o.get("word").getAsString();

				EmbedBuilder embedBuilder = EmbedBuilder.create();
				embedBuilder.color(EmbedColors.GRAY);
				embedBuilder.title(word);

				JsonArray meanings = o.get("meanings").getAsJsonArray();

				for (int i = 0; i < Math.min(25, meanings.size()); i++) {
					JsonObject o1 = meanings.get(i).getAsJsonObject();

					StringBuilder b = new StringBuilder("*");
					Utils.titleCase(b, o1.get("definition").getAsString());
					b.append('*');

					if (!o1.get("example").getAsString().isEmpty()) {
						b.append("\n\n\"");
						Utils.titleCase(b, o1.get("example").getAsString());
						b.append('"');
					}

					embedBuilder.field((i + 1) + ". " + o1.get("type").getAsString(), Utils.trim(b.toString(), 1024));
				}

				builder.addEmbed(embedBuilder);

				List<ActionComponent> list = new ArrayList<>();

				for (JsonElement e : o.get("phonetics").getAsJsonArray()) {
					JsonObject o1 = e.getAsJsonObject();

					if (!o1.get("audio_url").getAsString().isEmpty()) {
						list.add(Button.link(o1.get("audio_url").getAsString(), ReactionEmoji.unicode("\uD83C\uDFB5"), o1.get("text").getAsString()));
					}
				}

				if (!list.isEmpty()) {
					builder.addComponent(ActionRow.of(list));
				}

				event.edit().respond(builder);
				return;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		event.respond("No Definitions Found!");
	}
}
