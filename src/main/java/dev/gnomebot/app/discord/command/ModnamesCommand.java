package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.util.URLRequest;

/**
 * @author LatvianModder
 */
public class ModnamesCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("modnames")
			.description("Displays 10 randomly generated mod names")
			.add(string("type"))
			.run(ModnamesCommand::run);

	private static void run(ApplicationCommandEventWrapper event) throws Exception {
		event.acknowledge();
		String type = event.get("type").asString();

		event.embedResponse(spec -> {
			spec.title("10 randomly generated mod names:");
			String[] s = new String[10];

			for (int i = 0; i < 10; i++) {
				s[i] = "";

				try {
					URLRequest<String> request = URLRequest.of("https://modname.mcmc.dev/generate/" + type).toJoinedString();
					String m = request.block();
					s[i] = "[" + m + "](https://modname.mcmc.dev/" + request.getHeader("x-modname-permalink") + ")";
				} catch (Exception ex) {
				}
			}

			spec.description(String.join("\n", s));
		});
	}
}
