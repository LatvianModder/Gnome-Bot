package dev.gnomebot.app.discord.legacycommand;

import com.google.gson.JsonElement;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.LayoutComponent;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.MessageEditSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LatvianModder
 */
public class EditButtonsCommand {
	@LegacyDiscordCommand(name = "edit_buttons", help = "Edits buttons of a message", arguments = "<id> [message]", permissionLevel = AuthLevel.OWNER)
	public static final CommandCallback COMMAND = (context, reader) -> {
		Message m = context.findMessage(Snowflake.of(reader.readLong().orElse(0L))).orElse(null);

		if (m == null) {
			throw new DiscordCommandException("Message not found!");
		}

		String t = reader.readRemainingString().orElse("");

		if (t.isEmpty()) {
			context.reply(context.gc.prefix + "edit_buttons " + m.getId().asString() + "\n\\`\\`\\`\n```\n$ [ <button json goes here> ]\n```\\`\\`\\`");
		} else if (t.length() > 1 && t.startsWith("```\n") && t.endsWith("\n```")) {
			List<LayoutComponent> rows = new ArrayList<>();

			for (String s : t.substring(4, t.length() - 4).split("\n")) {
				if (s.startsWith("$ ")) {
					try {
						JsonElement element = Utils.GSON.fromJson(s.substring(2), JsonElement.class);

						List<ActionComponent> components = new ArrayList<>();
						Macro.addComponent(components, element);

						if (!components.isEmpty()) {
							rows.add(ActionRow.of(components));
						}
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}

			m.edit(MessageEditSpec.builder()
					.addAllComponents(rows)
					.allowedMentionsOrNull(DiscordMessage.noMentions())
					.build()
			).block();

			context.upvote();
		} else {
			throw new DiscordCommandException("Message must be written in a code block!");
		}
	};
}
