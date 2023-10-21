package dev.gnomebot.app.cli;

import com.mongodb.client.model.Filters;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.discord.command.RegisterCommand;
import dev.latvian.apps.webutils.data.MutableInt;
import discord4j.common.util.Snowflake;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.stream.Collectors;

public class CLIRoleMentionChart {
	@RegisterCommand
	public static final CLICommand COMMAND = CLICommand.make("role_mention_chart")
			.description("Print breakdown of where role has been mentioned the most")
			.run(CLIRoleMentionChart::run);

	private static void run(CLIEvent event) {
		var role = event.reader.readRole().orElse(null);

		if (role == null) {
			throw new NullPointerException("Role not found!");
		}

		var map = new HashMap<Long, MutableInt>();

		for (var msg : event.gc.messages.query()
				.filter(Filters.bitsAllSet("flags", DiscordMessage.FLAG_MENTIONS_ROLES))
				.filter(Filters.bitsAllClear("flags", DiscordMessage.FLAG_BOT))
				.filter(Filters.eq("role_mentions", role.id.asLong()))
				.projectionFields("channel")
		) {
			map.computeIfAbsent(msg.getChannelID(), MutableInt.MAP_VALUE).add(1);
		}

		event.respond(map.entrySet().stream()
				.map(e -> new AbstractMap.SimpleEntry<>(event.gc.getChannelMap().get(Snowflake.of(e.getKey())), e.getValue()))
				.filter(e -> e.getKey() != null && e.getValue().value > 0)
				.sorted((o1, o2) -> Integer.compare(o2.getValue().value, o1.getValue().value))
				.map(e -> e.getKey().getMention() + "," + e.getValue().value)
				.collect(Collectors.joining("\n", "Role mentions:\n\n", ""))
		);
	}
}
