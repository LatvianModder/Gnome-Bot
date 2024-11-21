package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.DeferrableInteractionEventWrapper;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;

public class ApplicationCommandInteractionEventWrapper<E extends ApplicationCommandInteractionEvent> extends DeferrableInteractionEventWrapper<E> {
	public ApplicationCommandInteractionEventWrapper(App app, GuildCollections gc, E e) {
		super(app, gc, e);
	}

	@Override
	public String toString() {
		return event.getCommandName();
	}
}
