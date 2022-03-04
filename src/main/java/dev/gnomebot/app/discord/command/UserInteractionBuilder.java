package dev.gnomebot.app.discord.command;

import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;

public class UserInteractionBuilder extends ApplicationCommandInteractionBuilder<UserInteractionEvent, UserInteractionEventWrapper, UserInteractionBuilder> {
	public UserInteractionBuilder(ApplicationCommandOption.Type t, String n) {
		super(InteractionType.USER, t, n);
	}
}