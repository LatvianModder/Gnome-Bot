package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.URLRequest;

import java.util.ArrayList;
import java.util.List;

public class ModnamesCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("modnames")
			.supportsDM()
			.description("Displays 10 randomly generated mod names")
			.add(string("type"))
			.run(ModnamesCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		var type = event.get("type").asString();

		var builder = EmbedBuilder.create("10 randomly generated mod names:");

		List<String> names = new ArrayList<>();

		for (var i = 0; i < 10; i++) {
			try {
				var request = URLRequest.of("https://modname.mcmc.dev/generate/" + type).toJoinedString();
				var m = request.block();
				names.add("[" + m + "](https://modname.mcmc.dev/" + request.getHeader("x-modname-permalink") + ")");
			} catch (Exception ex) {
			}
		}

		builder.description(names);
		event.respond(builder);
	}
}
