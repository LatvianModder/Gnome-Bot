package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.Config;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;
import io.javalin.http.HttpStatus;

import java.nio.charset.StandardCharsets;

/**
 * @author LatvianModder
 */
public class MathCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("math")
			.description("Gnome knows math pretty well")
			.add(string("equation").required())
			.add(bool("detailed"))
			.run(MathCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) {
		if (Config.get().wolfram_alpha_token.isEmpty()) {
			throw error("Wolfram Alpha token is not set! Owner of this bot instance has to set that up.");
		}

		String equation = event.get("equation").asString();
		boolean detailed = event.get("detailed").asBoolean(false);

		if (equation.isEmpty()) {
			throw error("Invalid question!");
		}

		event.acknowledge();
		respond(event, equation, detailed, 0);
	}

	private static void respond(ChatInputInteractionEventWrapper event, String equation, boolean detailed, int loops) {
		if (loops >= 3) {
			event.respond("> " + equation + "\nI got stuck in a loop");
			return;
		}

		var request = URLRequest.of("https://api.wolframalpha.com/v1/" + (detailed ? "simple" : "result")).toBytes();
		request.query("i", equation);
		request.query("appid", Config.get().wolfram_alpha_token);

		try {
			byte[] bytes = request.block();
			String contentType = request.getHeader("Content-Type");

			if (contentType.startsWith("image/")) {
				event.respond(MessageBuilder.create("\"" + equation + "\"").addFile("image." + contentType.substring(6).split("\\W", 2)[0], bytes));
			} else if (contentType.startsWith("text/")) {
				event.respond("> " + equation + "\n" + new String(bytes, StandardCharsets.UTF_8));
			} else {
				event.respond("Unknown content type: " + contentType);
			}
		} catch (URLRequest.UnsuccesfulRequestException ex) {
			if (ex.status == HttpStatus.NOT_IMPLEMENTED && ex.response.equals("Wolfram|Alpha did not understand your input")) {
				event.respond("> " + equation + "\nI don't know what that means :/");
			} else if (ex.status == HttpStatus.NOT_IMPLEMENTED && ex.response.equals("No short answer available")) {
				respond(event, equation, true, loops + 1);
			} else {
				event.respond("> " + equation + "\n" + ex.getMessage());
			}
		} catch (Exception ex) {
			event.respond("Failed to connect to Wolfram Alpha API!");
			ex.printStackTrace();
		}
	}
}