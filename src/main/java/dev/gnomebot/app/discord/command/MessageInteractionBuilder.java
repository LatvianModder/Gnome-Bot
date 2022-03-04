package dev.gnomebot.app.discord.command;

import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;

public class MessageInteractionBuilder extends ApplicationCommandInteractionBuilder<MessageInteractionEvent, MessageInteractionEventWrapper, MessageInteractionBuilder> {
	public MessageInteractionBuilder(ApplicationCommandOption.Type t, String n) {
		super(InteractionType.MESSAGE, t, n);
	}
}