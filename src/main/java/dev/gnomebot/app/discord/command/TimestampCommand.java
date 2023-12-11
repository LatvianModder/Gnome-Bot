package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.Config;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.URLRequest;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimestampCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("timestamp")
			.description("Converts your input to discord timestamp format")
			.add(string("input").required())
			.run(TimestampCommand::run);

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		String input = event.get("input").asString();

		if (input.isEmpty()) {
			throw error("Invalid input!");
		}

		if (input.startsWith("in ")) {
			input = input.substring(3) + " later";
		}

		var request = URLRequest.of("https://api.wolframalpha.com/v1/result").toBytes();
		request.query("i", input + " to unix timestamp");
		request.query("appid", Config.get().wolfram_alpha_token);

		byte[] bytes = request.block();
		String contentType = request.getHeader("Content-Type");

		if (contentType.startsWith("text/")) {
			String result = new String(bytes, StandardCharsets.UTF_8);
			Matcher matcher = Pattern.compile("^(\\d+) \\(Unix time\\)$").matcher(result);

			if (matcher.find()) {
				String time = matcher.group(1);
				List<String> list = new ArrayList<>();
				list.add("Unix timestamp for `" + input + "`:");
				list.add("`<t:" + time + ">` = <t:" + time + ">");
				list.add("`<t:" + time + ":R>` = <t:" + time + ":R>");
				event.respond(MessageBuilder.create().content(list).ephemeral(true));
			}
		}
	}
}
