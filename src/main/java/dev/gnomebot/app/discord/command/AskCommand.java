package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.Config;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;

import java.nio.charset.StandardCharsets;

/**
 * @author LatvianModder
 */
public class AskCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("ask")
			.description("Gnome knows a lot of things. Not all, but a lot. Try asking him something!")
			.add(string("question").required())
			.add(bool("detailed"))
			.run(AskCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		if (Config.get().wolfram_alpha_token.isEmpty()) {
			throw error("Wolfram Alpha token is not set! Owner of this bot instance has to set that up.");
		}

		String question = event.get("question").asString();
		boolean detailed = event.get("detailed").asBoolean(false);

		if (question.isEmpty()) {
			throw error("Invalid question!");
		}

		event.acknowledge();
		respond(event, question, detailed, 0);
	}

	private static void respond(ChatInputInteractionEventWrapper event, String question, boolean detailed, int loops) {
		if (loops >= 3) {
			event.respond("> " + question + "\nI got stuck in a loop");
			return;
		}

		var request = URLRequest.of("https://api.wolframalpha.com/v1/" + (detailed ? "simple" : "result")).toBytes();
		request.query("i", question);
		request.query("appid", Config.get().wolfram_alpha_token);

		try {
			byte[] bytes = request.block();
			String contentType = request.getHeader("Content-Type");

			if (contentType.startsWith("image/")) {
				event.respond(MessageBuilder.create("\"" + question + "\"").addFile("image." + contentType.substring(6).split("\\W", 2)[0], bytes));
			} else if (contentType.startsWith("text/")) {
				event.respond("> " + question + "\n" + new String(bytes, StandardCharsets.UTF_8));
			} else {
				event.respond("Unknown content type: " + contentType);
			}
		} catch (URLRequest.UnsuccesfulRequestException ex) {
			if (ex.code == 501 && ex.response.equals("Wolfram|Alpha did not understand your input")) {
				event.respond("> " + question + "\nI don't know what that means :/");
			} else if (ex.code == 501 && ex.response.equals("No short answer available")) {
				respond(event, question, true, loops + 1);
			} else {
				event.respond("> " + question + "\n" + ex.getMessage());
			}
		} catch (Exception ex) {
			event.respond("Failed to connect to Wolfram Alpha API!");
			ex.printStackTrace();
		}
	}
}