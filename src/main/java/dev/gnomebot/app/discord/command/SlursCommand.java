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
public class SlursCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("slurs")
			.add(sub("test")
					.add(string("text").required())
					.run(SlursCommand::test)
			)
			.add(sub("list")
					.run(SlursCommand::list)
			)
			.add(sub("add")
					.add(string("word").required())
					.run(SlursCommand::add)
			)
			.add(sub("remove")
					.add(string("word").required())
					.run(SlursCommand::remove)
			)
			.add(sub("view_regex")
					.description("View generated regex")
					.run(SlursCommand::viewRegex)
			);

	private static void test(ChatInputInteractionEventWrapper event) {
		event.acknowledgeEphemeral();

		if (event.context.gc.badWordRegex == null) {
			throw error("Slurs aren't configured for this server!");
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
			event.respond(EmbedBuilder.create("Doesn't contain any slurs", r1).greenColor());
		} else {
			event.respond(EmbedBuilder.create("Contains slurs!", r1).redColor());
		}
	}

	private static void list(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderAdmin();

		if (event.context.gc.badWordRegex == null) {
			throw new GnomeException("Slurs aren't configured for this server!");
		}

		event.respond(String.join("\n", event.context.gc.badWords.get()));
	}

	private static void add(ChatInputInteractionEventWrapper event) {
		throw wip();
	}

	private static void remove(ChatInputInteractionEventWrapper event) {
		throw wip();
	}

	private static void viewRegex(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderAdmin();

		if (event.context.gc.badWordRegex == null) {
			throw error("Slurs aren't configured for this server!");
		}

		event.respond(MessageBuilder.create("Slur Regex").addComponentRow(Button.link(App.url("api/guild/slur-regex/" + event.context.gc.guildId.asString()), "View")));
	}
}