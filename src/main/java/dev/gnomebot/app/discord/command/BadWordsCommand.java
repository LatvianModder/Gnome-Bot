package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.discord.MessageFilter;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import discord4j.core.object.component.ActionRow;
import discord4j.core.object.component.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class BadWordsCommand extends ApplicationCommands {
	@RootCommand
	public static final CommandBuilder COMMAND = root("bad_words")
			.add(sub("test")
					.add(string("text").required())
					.run(BadWordsCommand::test)
			)
			.add(sub("list")
					.run(BadWordsCommand::list)
			)
			.add(sub("add")
					.add(string("word").required())
					.run(BadWordsCommand::add)
			)
			.add(sub("remove")
					.add(string("word").required())
					.run(BadWordsCommand::remove)
			)
			.add(sub("view_regex")
					.description("View generated regex")
					.run(BadWordsCommand::viewRegex)
			);

	private static void test(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.acknowledgeEphemeral();

		if (event.context.gc.badWordRegex == null) {
			throw new DiscordCommandException("Bad words aren't configured for this server!");
		}

		String text = event.get("text").asString();
		StringBuilder sb = new StringBuilder(event.context.gc.badWordRegex.matcher(text).replaceAll(" **__ $0 __** "));

		if (event.context.isAdmin()) {
			List<String> badWords1 = event.context.gc.badWords.get().stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
			List<String> matched = new ArrayList<>();

			for (String w : badWords1) {
				StringBuilder sbr = new StringBuilder();

				for (int j = 0; j < w.length(); j++) {
					char c = Character.toLowerCase(w.charAt(j));

					if (j > 0) {
						sbr.append("[\\s\\W]*");
					}

					MessageFilter.alias(sbr, c);
				}

				if (Pattern.compile(sbr.toString(), Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(text).find()) {
					matched.add(w);
				}
			}

			sb.append("\nMatched: ");
			sb.append(matched);
		}

		String r1 = sb.toString();

		event.embedResponse(spec -> {
			if (text.equals(r1)) {
				spec.title("Doesn't contain bad words");
				spec.color(EmbedColors.GREEN);
			} else {
				spec.title("Contains bad words!");
				spec.color(EmbedColors.RED);
			}

			spec.description(r1);
		});
	}

	private static void list(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.context.checkSenderAdmin();

		if (event.context.gc.badWordRegex == null) {
			throw new DiscordCommandException("Bad words aren't configured for this server!");
		}

		event.respond(String.join("\n", event.context.gc.badWords.get()));
	}

	private static void add(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		throw wip();
	}

	private static void remove(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		throw wip();
	}

	private static void viewRegex(ApplicationCommandEventWrapper event) throws DiscordCommandException {
		event.context.checkSenderAdmin();

		if (event.context.gc.badWordRegex == null) {
			throw new DiscordCommandException("Bad words aren't configured for this server!");
		}

		event.respond(builder -> builder.content("Bad Word Regex").addComponent(ActionRow.of(Button.link("https://gnomebot.dev/api/guild/bad-word-regex/" + event.context.gc.guildId.asString(), "View")).getData()));
	}
}