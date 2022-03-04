package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;

/**
 * @author LatvianModder
 */
public class ApplicationCommandInteractionEventWrapper<E extends ApplicationCommandInteractionEvent> extends DeferrableInteractionEventWrapper<E> {
	public ApplicationCommandInteractionEventWrapper(GuildCollections gc, E e) {
		super(gc, e);
	}

	@Override
	public String toString() {
		return event.getCommandName();
	}
}
