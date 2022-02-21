package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.MessageFilter;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
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

	private static void test(ApplicationCommandEventWrapper event) {
		event.acknowledgeEphemeral();

		if (event.context.gc.badWordRegex == null) {
			throw new GnomeException("Bad words aren't configured for this server!");
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

		if (text.equals(r1)) {
			event.respond(EmbedBuilder.create("Doesn't contain bad words", r1).greenColor());
		} else {
			event.respond(EmbedBuilder.create("Contains bad words!", r1).redColor());
		}
	}

	private static void list(ApplicationCommandEventWrapper event) {
		event.context.checkSenderAdmin();

		if (event.context.gc.badWordRegex == null) {
			throw new GnomeException("Bad words aren't configured for this server!");
		}

		event.respond(String.join("\n", event.context.gc.badWords.get()));
	}

	private static void add(ApplicationCommandEventWrapper event) {
		throw wip();
	}

	private static void remove(ApplicationCommandEventWrapper event) {
		throw wip();
	}

	private static void viewRegex(ApplicationCommandEventWrapper event) {
		event.context.checkSenderAdmin();

		if (event.context.gc.badWordRegex == null) {
			throw new GnomeException("Bad words aren't configured for this server!");
		}

		event.respond(MessageBuilder.create("Bad Word Regex").addComponentRow(Button.link(App.url("api/guild/bad-word-regex/" + event.context.gc.guildId.asString()), "View")));
	}
}