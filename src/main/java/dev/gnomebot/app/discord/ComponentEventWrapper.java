package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.util.MessageBuilder;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.discordjson.json.MessageData;

import java.util.Optional;

public class ComponentEventWrapper extends DeferrableInteractionEventWrapper<ComponentInteractionEvent> {
	public final String[] path;
	public ComponentEventWrapper parent;
	private int edit = 0;

	public ComponentEventWrapper(GuildCollections gc, ComponentInteractionEvent event, String id) {
		super(gc, event);

		path = id.split("/");

		for (int i = 0; i < path.length; i++) {
			path[i] = decode(path[i]);
		}
	}

	@Override
	protected CommandContext createContext() {
		return new CommandContext() {
			/*
			@Override
			public Message reply(String content) {
				respond(content);
				return null;
			}

			@Override
			public Message reply(Consumer<EmbedCreateSpec.Builder> espec) {
				respond(builder -> {
					EmbedCreateSpec.Builder ebuilder = EmbedCreateSpec.builder();
					ebuilder.color(EmbedColors.GRAY);
					espec.accept(ebuilder);
					builder.addEmbed(ebuilder.build());
				});
				return null;
			}
			 */
		};
	}

	public ComponentEventWrapper copy(String id) {
		ComponentEventWrapper wrapper = new ComponentEventWrapper(context.gc, event, id);
		wrapper.parent = this;
		wrapper.edit = edit;
		wrapper.acknowledged = acknowledged;
		return wrapper;
	}

	@Override
	public String toString() {
		return String.join("/", path);
	}

	public ComponentEventWrapper edit(int b) {
		edit = b;
		return this;
	}

	@Override
	public Optional<MessageData> respond(MessageBuilder messageBuilder) {
		if (edit >= 2) {
			messageBuilder.noComponents();
		}

		if (edit > 0) {
			event.edit(messageBuilder.toInteractionApplicationCommandCallbackSpec()).block();
		} else {
			event.reply(messageBuilder.toInteractionApplicationCommandCallbackSpec()).block();
		}

		return Optional.empty();
	}

	/*
	public Optional<MessageData> respond(String content) {
		if (acknowledged) {
			return Optional.of(getResponse().createFollowupMessageEphemeral(Utils.trimContent(content)).block());
		} else {
			return super.respond(content);
		}
	}
	 */
}