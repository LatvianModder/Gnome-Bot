package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.App;
import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.data.DiscordMessage;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.EmbedColors;
import dev.gnomebot.app.discord.InteractionEventWrapper;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.util.Utils;
import discord4j.core.event.domain.interaction.ApplicationCommandInteractionEvent;
import discord4j.core.event.domain.interaction.ChatInputAutoCompleteEvent;
import discord4j.core.event.domain.interaction.DeferrableInteractionEvent;
import discord4j.core.event.domain.interaction.InteractionCreateEvent;
import discord4j.core.event.domain.interaction.MessageInteractionEvent;
import discord4j.core.event.domain.interaction.UserInteractionEvent;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.component.ActionRow;
import discord4j.core.spec.EmbedCreateSpec;
import discord4j.discordjson.json.FollowupMessageRequest;
import discord4j.discordjson.json.ImmutableFollowupMessageRequest;
import discord4j.discordjson.json.MessageData;
import discord4j.discordjson.json.WebhookMessageEditRequest;
import discord4j.rest.http.client.ClientException;
import discord4j.rest.util.MultipartRequest;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * @author LatvianModder
 */
public class ApplicationCommandEventWrapper extends InteractionEventWrapper<InteractionCreateEvent> {
	public final Map<String, CommandOption> options;
	public final CommandOption focused;
	private boolean acknowledged;

	public ApplicationCommandEventWrapper(GuildCollections gc, InteractionCreateEvent e, List<ApplicationCommandInteractionOption> o) {
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

		if (event instanceof UserInteractionEvent e1) {
			options.put("user", new CommandOption(context, "user", e1.getTargetId().asString(), false));
		}

		if (event instanceof MessageInteractionEvent e1) {
			options.put("message", new CommandOption(context, "message", e1.getTargetId().asString(), false));
		}

		focused = f;
	}

	@Override
	public String toString() {
		if (event instanceof ApplicationCommandInteractionEvent e) {
			return e.getCommandName();
		} else if (event instanceof ChatInputAutoCompleteEvent e) {
			return e.getCommandName();
		}

		return event.getClass().getName();
	}

	public void acknowledge() {
		if (!acknowledged && event instanceof DeferrableInteractionEvent e) {
			acknowledged = true;
			e.deferReply().subscribe();
		}
	}

	public void acknowledgeEphemeral() {
		if (!acknowledged && event instanceof DeferrableInteractionEvent e) {
			acknowledged = true;
			e.deferReply().withEphemeral(true).subscribe();
		}
	}

	public Optional<MessageData> respond(Consumer<ImmutableFollowupMessageRequest.Builder> msg) {
		return respond(msg, Collections.emptyList());
	}

	public Optional<MessageData> respond(Consumer<ImmutableFollowupMessageRequest.Builder> msg, List<Tuple2<String, InputStream>> files) {
		if (!acknowledged) {
			acknowledgeEphemeral();
		}

		ImmutableFollowupMessageRequest.Builder builder = FollowupMessageRequest.builder();
		builder.allowedMentions(DiscordMessage.noMentions().toData());
		msg.accept(builder);

		try {
			return Optional.ofNullable(getResponse().createFollowupMessage(MultipartRequest.ofRequestAndFiles(builder.build(), files)).block());
		} catch (ClientException ex) {
			App.error("Failed to respond to slash command " + this + " " + options + ": " + ex.getMessage());

			for (Throwable t : ex.getSuppressed()) {
				t.printStackTrace();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return Optional.empty();
	}

	public Optional<MessageData> respondFile(Consumer<ImmutableFollowupMessageRequest.Builder> msg, String fileName, byte[] fileBytes) {
		if (!acknowledged) {
			acknowledge();
		}

		return respond(msg, Collections.singletonList(Tuples.of(fileName, new ByteArrayInputStream(fileBytes))));
	}

	public Optional<MessageData> respondFile(Consumer<ImmutableFollowupMessageRequest.Builder> msg, String filename, String ext, byte[] data, boolean createLocalFile) throws DiscordCommandException {
		String actualFileName = filename + "-" + Instant.now().toString().replace(':', '-') + "." + ext;

		try {
			if (createLocalFile) {
				Path directory = AppPaths.DATA_GUILDS.resolve(filename);

				if (Files.notExists(directory)) {
					Files.createDirectories(directory);
				}

				Path path = directory.resolve(actualFileName);
				App.info("Writing to " + path.toAbsolutePath());
				Files.createFile(path);
				Files.write(path, data);
			}

			return respondFile(msg, actualFileName, data);
		} catch (Exception ex) {
			if (createLocalFile) {
				throw new DiscordCommandException("Could not upload file, it's too large (" + (data.length / 1048576L) + " MB)! DM Gnome and request \n" + filename);
			} else {
				throw new DiscordCommandException("Could not upload file, it's too large (" + (data.length / 1048576L) + " MB)!");
			}
		}
	}

	public Optional<MessageData> respond(String content, ActionRow... actions) {
		return respond(builder -> {
			builder.content(Utils.trimContent(content));

			if (actions.length > 0) {
				for (ActionRow row : actions) {
					builder.addComponent(row.getData());
				}
			}
		});
	}

	public Optional<MessageData> respond(String content) {
		return respond(content, new ActionRow[0]);
	}

	public Optional<MessageData> respond(List<String> content) {
		return respond(String.join("\n", content));
	}

	public Optional<MessageData> embedResponse(Consumer<EmbedCreateSpec.Builder> spec) {
		EmbedCreateSpec.Builder s = EmbedCreateSpec.builder();
		s.color(EmbedColors.GRAY);
		spec.accept(s);
		return respond(builder -> builder.embeds(Collections.singletonList(s.build().asRequest())));
	}

	public Optional<MessageData> embedResponse(String title, String description) {
		return respond(builder -> builder.embeds(Collections.singletonList(EmbedCreateSpec.builder().color(EmbedColors.GRAY).title(title).description(description).build().asRequest())));
	}

	public void editInitial(WebhookMessageEditRequest request) {
		if (!acknowledged) {
			acknowledgeEphemeral();
		}

		getResponse().editInitialResponse(request).block();
	}

	public void editInitial(String content) {
		editInitial(WebhookMessageEditRequest.builder().contentOrNull(content).build());
	}

	public boolean has(String id) {
		return options.containsKey(id);
	}

	public CommandOption get(String id) throws DiscordCommandException {
		CommandOption o = options.get(id);

		if (o == null) {
			return new CommandOption(context, id, "", false);
		}

		return o;
	}
}
