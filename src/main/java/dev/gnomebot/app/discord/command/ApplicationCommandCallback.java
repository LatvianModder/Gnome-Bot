package dev.gnomebot.app.discord.command;

import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;

@FunctionalInterface
public interface ApplicationCommandCallback<E extends ApplicationCommandInteractionEvent, W extends ApplicationCommandInteractionEventWrapper<E>> {
	void run(W event) throws Exception;
}
