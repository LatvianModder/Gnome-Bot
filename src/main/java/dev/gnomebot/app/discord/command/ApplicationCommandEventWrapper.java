package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ApplicationCommandEventWrapper extends DeferrableInteractionEventWrapper<ApplicationCommandInteractionEvent> {
	public final Map<String, CommandOption> options;
	public final CommandOption focused;

	public ApplicationCommandEventWrapper(GuildCollections gc, ApplicationCommandInteractionEvent e, List<ApplicationCommandInteractionOption> o) {
		super(gc, e);
		options = new HashMap<>();
		CommandOption f = null;

		for (ApplicationCommandInteractionOption option : o) {
			CommandOption o1 = new CommandOption(context, option);
			options.put(o1.name, o1);

			if (o1.focused) {
				f = o1;
			}
		}

		focused = f;

		if (event instanceof UserInteractionEvent e1) {
			options.put("user", new CommandOption(context, "user", e1.getTargetId().asString(), false));
		}

		if (event instanceof MessageInteractionEvent e1) {
			options.put("message", new CommandOption(context, "message", e1.getTargetId().asString(), false));
		}
	}

	@Override
	public String toString() {
		return event.getCommandName() + " " + options;
	}

	public boolean has(String id) {
		return options.containsKey(id);
	}

	public CommandOption get(String id) {
		CommandOption o = options.get(id);

		if (o == null) {
			return new CommandOption(context, id, "", false);
		}

		return o;
	}
}
