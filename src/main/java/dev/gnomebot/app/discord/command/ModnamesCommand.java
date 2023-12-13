package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.URLRequest;

import java.util.ArrayList;
import java.util.List;

public class ModnamesCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("modnames")
			.description("Displays 10 randomly generated mod names")
			.add(string("type"))
			.run(ModnamesCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledge();
		String type = event.get("type").asString();

		EmbedBuilder builder = EmbedBuilder.create("10 randomly generated mod names:");

		List<String> names = new ArrayList<>();

		for (int i = 0; i < 10; i++) {
			try {
				URLRequest<String> request = URLRequest.of("https://modname.mcmc.dev/generate/" + type).toJoinedString();
				String m = request.block();
				names.add("[" + m + "](https://modname.mcmc.dev/" + request.getHeader("x-modname-permalink") + ")");
			} catch (Exception ex) {
			}
		}

		builder.description(names);
		event.respond(builder);
	}
}
