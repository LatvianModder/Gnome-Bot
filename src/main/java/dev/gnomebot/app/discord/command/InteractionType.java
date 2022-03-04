package dev.gnomebot.app.discord.command;

import discord4j.core.object.command.ApplicationCommand;

import java.util.LinkedHashMap;
import java.util.Map;

public class InteractionType<T extends ApplicationCommandInteractionBuilder<?, ?, T>> {
	public static final Map<String, InteractionType<?>> TYPES = new LinkedHashMap<>();
	public static final Map<Integer, InteractionType<?>> TYPES_BY_ID = new LinkedHashMap<>();

	public static final InteractionType<ChatInputInteractionBuilder> CHAT_INPUT = new InteractionType<>("chat_input", ApplicationCommand.Type.CHAT_INPUT.getValue(), 100, true);
	public static final InteractionType<UserInteractionBuilder> USER = new InteractionType<>("user", ApplicationCommand.Type.USER.getValue(), 5, false);
	public static final InteractionType<MessageInteractionBuilder> MESSAGE = new InteractionType<>("message", ApplicationCommand.Type.MESSAGE.getValue(), 5, false);

	public final String name;
	public final int type;
	public final int limit;
	public final boolean hasDescription;
	public final Map<String, T> builders;

	private InteractionType(String name, int type, int limit, boolean hasDescription) {
		this.name = name;
		this.type = type;
		this.limit = limit;
		this.hasDescription = hasDescription;
		this.builders = new LinkedHashMap<>();
		TYPES.put(name, this);
		TYPES_BY_ID.put(type, this);
	}

	@Override
	public String toString() {
		return name;
	}
}
