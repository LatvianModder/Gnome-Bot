package dev.gnomebot.app.discord.legacycommand;

import com.mongodb.BasicDBObject;
import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.object.component.SelectMenu;
import discord4j.core.object.entity.Message;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.core.spec.MessageEditSpec;
import discord4j.rest.util.Permission;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class PollCommand {
	@LegacyDiscordCommand(name = "poll", help = "Create a poll", arguments = "<title> [options each in new line]")
	public static final CommandCallback COMMAND = (context, reader) -> {
		context.checkBotPerms(Permission.SEND_MESSAGES);

		List<String> s = Arrays.stream(reader.readRemainingString().orElse("").split("\n")).filter(str -> !str.isEmpty()).collect(Collectors.toList());

		if (s.isEmpty()) {
			throw new GnomeException("Poll can't have empty question!").ephemeral().deleteMessage();
		} else if (s.size() == 2) {
			throw new GnomeException("Poll must have at least 2 options!").ephemeral().deleteMessage();
		} else if (s.size() > 11) {
			throw new GnomeException("Poll has too many options! (Max 10)").ephemeral().deleteMessage();
		} else if (s.size() == 1) {
			s.add("Yes");
			s.add("No");
		}

		String content = s.remove(0);

		context.message.delete().subscribe();

		int number = context.gc.pollNumber.get() + 1;
		context.gc.pollNumber.set(number);
		context.gc.pollNumber.save();

		context.referenceMessage = false;

		List<SelectMenu.Option> selectMenuOptions = new ArrayList<>();

		selectMenuOptions.add(SelectMenu.Option.of("Remove my vote", "vote/none").withEmoji(Emojis.VOTENONE));

		for (int i = 0; i < s.size(); i++) {
			selectMenuOptions.add(SelectMenu.Option.of(s.get(i), "vote/" + i).withEmoji(Emojis.NUMBERS[i]));
		}

		Message m = context.reply(MessageBuilder.create()
				.addEmbed(EmbedBuilder.create()
						.color(EmbedColor.GRAY)
						.url(App.url("poll/" + context.gc.guildId.asString() + "/" + number))
						.title("Loading poll #" + number + "...")
				)
				.addComponentRow(SelectMenu.of("poll/" + number, selectMenuOptions)
						.withPlaceholder("Click here to vote!")
						.withMinValues(1)
						.withMaxValues(1)
				)
		);

		Document document = new Document();
		document.put("_id", m.getId().asLong());
		document.put("author", context.sender.getId().asLong());
		document.put("timestamp", Date.from(m.getTimestamp()));
		document.put("channel", context.channelInfo.id.asLong());
		document.put("number", number);
		document.put("content", content);
		document.put("options", s);
		document.put("votes", new BasicDBObject());
		context.gc.polls.insert(document);

		m.edit(MessageEditSpec.builder().addEmbed(context.gc.polls.findFirst(m).edit(context.gc, EmbedCreateFields.Footer.of(context.sender.getTag(), context.sender.getAvatarUrl()))).build()).subscribe();
	};
}
