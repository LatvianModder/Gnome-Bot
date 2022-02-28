package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.Config;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;

import java.nio.charset.StandardCharsets;

/**
 * @author LatvianModder
 */
public class AskCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("ask")
			.description("Gnome knows a lot of things. Not all, but a lot. Try asking him something!")
			.add(string("question").required())
			.add(bool("detailed"))
			.run(AskCommand::run);

	private static void run(ApplicationCommandEventWrapper event) {
		if (Config.get().wolfram_alpha_token.isEmpty()) {
			throw error("Wolfram Alpha token is not set! Owner of this bot instance has to set that up.");
		}

		String question = event.get("question").asString();
		boolean detailed = event.get("detailed").asBoolean(false);

		if (question.isEmpty()) {
			throw error("Invalid question!");
		}

		event.acknowledge();

		var request = URLRequest.of("https://api.wolframalpha.com/v1/" + (detailed ? "simple" : "result")).toBytes();
		request.query("i", question);
		request.query("appid", Config.get().wolfram_alpha_token);

		try {
			byte[] bytes = request.block();
			String contentType = request.getHeader("Content-Type");

			if (contentType.startsWith("image/")) {
				event.respond(MessageBuilder.create("\"" + question + "\"").addFile("image." + contentType.substring(6).split("\\W", 2)[0], bytes));
			} else if (contentType.startsWith("text/")) {
				event.respond("\"" + question + "\"\n" + new String(bytes, StandardCharsets.UTF_8));
			} else {
				event.respond("Unknown content type: " + contentType);
			}
		} catch (URLRequest.UnsuccesfulRequestException ex) {
			if (ex.code == 501) {
				event.respond("Wolfram|Alpha did not understand your input!");
			} else {
				event.respond("Failed to connect to Wolfram Alpha API!\n" + ex.getMessage());
			}
		} catch (Exception ex) {
			event.respond("Failed to connect to Wolfram Alpha API!");
			ex.printStackTrace();
		}
	}
}