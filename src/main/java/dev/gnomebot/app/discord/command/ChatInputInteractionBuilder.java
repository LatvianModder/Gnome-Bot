package dev.gnomebot.app.discord.command;

import discord4j.core.event.domain.interaction.ChatInputInteractionEvent;
import discord4j.core.object.command.ApplicationCommandOption;

public class ChatInputInteractionBuilder extends ApplicationCommandInteractionBuilder<ChatInputInteractionEvent, ChatInputInteractionEventWrapper, ChatInputInteractionBuilder> {
	public ChatInputInteractionBuilder(ApplicationCommandOption.Type t, String n) {
		super(InteractionType.CHAT_INPUT, t, n);
	}
}