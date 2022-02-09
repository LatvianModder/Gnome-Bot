package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.util.Utils;
import discord4j.core.event.domain.interaction.ComponentInteractionEvent;
import discord4j.core.spec.InteractionApplicationCommandCallbackSpec;
import discord4j.discordjson.json.MessageData;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ComponentEventWrapper extends InteractionEventWrapper<ComponentInteractionEvent> {
	public final String[] path;
	public ComponentEventWrapper parent;
	private int edit = 0;
	private boolean acknowledged = false;

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

	public void acknowledge() {
		if (!acknowledged) {
			acknowledged = true;
			//event.acknowledge().block();
			event.deferEdit().block();
		}
	}

	public ComponentEventWrapper edit(int b) {
		edit = b;
		return this;
	}

	public void respond(Consumer<InteractionApplicationCommandCallbackSpec.Builder> msg) {
		InteractionApplicationCommandCallbackSpec.Builder builder = InteractionApplicationCommandCallbackSpec.builder();
		builder.allowedMentions(DiscordMessage.noMentions());

		if (edit >= 2) {
			builder.components(Collections.emptyList());
		}

		builder.ephemeral(true);
		msg.accept(builder);
		(edit > 0 ? event.edit(builder.build()) : event.reply(builder.build())).block();
	}

	public Optional<MessageData> respond(String content) {
		if (acknowledged) {
			return Optional.of(getResponse().createFollowupMessageEphemeral(Utils.trimContent(content)).block());
		} else {
			respond(builder -> builder.content(Utils.trimContent(content)));
			return Optional.empty();
		}
	}

	public Optional<MessageData> respond(List<String> content) {
		return respond(String.join("\n", content));
	}
}