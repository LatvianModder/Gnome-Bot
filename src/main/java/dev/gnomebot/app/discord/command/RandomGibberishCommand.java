package dev.gnomebot.app.discord.command;

import java.util.Random;

public class RandomGibberishCommand extends ApplicationCommands {
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("random_gibberish")
			.supportsDM()
			.description("Just generates random gibberish text. You can use it as 'alien language' or something")
			.run(RandomGibberishCommand::run);

	private static final String SET_1 = "aeiouy";
	private static final String SET_2 = "bcdfghjklmnpqrstvwxz";

	public static void putChars(char[] c, int index, Random r) {
		c[index] = SET_2.charAt(r.nextInt(SET_2.length()));
		c[index + 1] = c[index] == 'q' ? 'u' : SET_1.charAt(r.nextInt(SET_1.length()));
	}

	private static void run(ChatInputInteractionEventWrapper event) throws Exception {
		event.acknowledgeEphemeral();
		var sb = new StringBuilder();
		var random = new Random();

		for (var i = 0; i < 100; i++) {
			var len = random.nextInt(4) + random.nextInt(5);

			if (len < 2) {
				continue;
			}

			var c = new char[len / 2 * 2];

			for (var j = 0; j < len / 2; j++) {
				putChars(c, j * 2, random);
			}

			sb.append(c);

			switch (random.nextInt(40)) {
				case 0, 1, 2, 3, 4 -> sb.append(',');
				case 5 -> sb.append('.');
				case 6 -> sb.append('!');
				case 7 -> sb.append('?');
			}

			sb.append(' ');
		}

		event.respond(sb.toString());
	}
}
