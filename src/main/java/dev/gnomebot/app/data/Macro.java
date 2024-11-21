package dev.gnomebot.app.data;

import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.MacroCommands;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.data.HexId32;
import dev.latvian.apps.webutils.data.Lazy;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Macro implements Comparable<Macro>, Callable<String> {
	public final GuildCollections guild;
	private final Lazy<String> content;
	public HexId32 id = HexId32.NONE;
	public String stringId = "";
	public String name = "";
	public long author = 0L;
	public Instant created = null;
	public long slashCommand = 0L;
	private String description;

	private ChatCommandSuggestion chatCommandSuggestion;
	private Pair<ContentType, Object> cachedContent;

	public Macro(GuildCollections guild) {
		this.guild = guild;
		this.content = Lazy.of(this);
	}

	public Path getContentPath(boolean write) {
		var ids = id.toString();
		var dir = guild.paths.macros.resolve(ids.substring(0, 2));

		if (write) {
			dir = AppPaths.makeDir(dir);
		}

		return dir.resolve(ids + ".txt");
	}

	public void setContent(String content) {
		try {
			if (content.isEmpty()) {
				Files.deleteIfExists(getContentPath(false));
			} else {
				Files.writeString(getContentPath(true), content);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		invalidateCache();
	}

	public String getContent() {
		var c = content.get();
		return c == null ? "" : c;
	}

	public void invalidateCache() {
		content.invalidate();
		cachedContent = null;
		chatCommandSuggestion = null;
	}

	public Pair<ContentType, Object> getCachedContent() {
		if (cachedContent == null) {
			cachedContent = ContentType.parse(guild, getContent());
		}

		return cachedContent;

	}

	public MessageBuilder createMessage(GuildCollections gc, @Nullable CommandReader reader, long sender) {
		var cached = getCachedContent();
		return cached.a().render(gc, reader, cached.b(), sender);
	}

	public CompletableFuture<MessageBuilder> createMessageOrTimeout(GuildCollections gc, @Nullable CommandReader reader, long sender) {
		return CompletableFuture.supplyAsync(() -> createMessage(gc, reader, sender)).completeOnTimeout(MessageBuilder.create("Macro timed out!"), 2500L, TimeUnit.MILLISECONDS);
	}

	public void rename(String rename) {
		var l = setSlashCommand(false);

		guild.getMacroMap().remove(stringId);
		stringId = rename.toLowerCase();
		name = rename;
		guild.getMacroMap().put(stringId, this);

		if (l != 0L && !isHidden()) {
			setSlashCommand(true);
		}
	}

	public long setSlashCommand(boolean b) {
		if (b) {
			var data = guild.getClient().getRestClient().getApplicationService().createGuildApplicationCommand(guild.db.app.discordHandler.selfId, guild.guildId, buildCommand()).block();

			var id = data == null ? 0L : data.id().asLong();

			if (data != null) {
				slashCommand = id;
			}

			return id;
		} else {
			if (slashCommand != 0L) {
				try {
					guild.getClient().getRestClient().getApplicationService().deleteGuildApplicationCommand(guild.db.app.discordHandler.selfId, guild.guildId, slashCommand).block();
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				slashCommand = 0L;
			}

			return slashCommand;
		}
	}

	public String getDescription() {
		if (description == null) {
			description = "Macro created by " + guild.db.app.discordHandler.getUserName(this.author).orElse("Deleted User");
		}

		return description;
	}

	public ApplicationCommandRequest buildCommand() {
		var builder = ApplicationCommandRequest.builder();
		builder.name(stringId);
		builder.description(getDescription());

		var content = getCachedContent();

		if (content.a() == ContentType.MACRO_BUNDLE) {
			var bundle = (MacroBundle) content.b();

			for (var item : bundle.macros.values()) {
				var option = ApplicationCommandOptionData.builder();
				option.name(item.id());
				option.description(item.description());
				var contentSub = item.macro().getCachedContent();

				if (contentSub.a() == ContentType.MACRO_BUNDLE) {
					option.type(ApplicationCommandOption.Type.SUB_COMMAND_GROUP.getValue());
					var bundleSub = (MacroBundle) contentSub.b();

					for (var itemSub : bundleSub.macros.values()) {
						var optionSub = ApplicationCommandOptionData.builder();
						optionSub.name(itemSub.id());
						optionSub.description(itemSub.description());
						optionSub.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue());
						option.addOption(optionSub.build());
					}
				} else {
					option.type(ApplicationCommandOption.Type.SUB_COMMAND.getValue());
				}

				builder.addOption(option.build());
			}
		}

		return builder.build();
	}

	public ChatCommandSuggestion getChatCommandSuggestion() {
		if (chatCommandSuggestion == null) {
			chatCommandSuggestion = new ChatCommandSuggestion(name, id.toString(), stringId, 0);
		}

		return chatCommandSuggestion;
	}

	public int getUses() {
		return guild.getMacroUses(id.getAsInt());
	}

	public void addUse() {
		guild.addMacroUse(id.getAsInt());
	}

	@Override
	public int compareTo(@NotNull Macro o) {
		return stringId.compareToIgnoreCase(o.stringId);
	}

	public String chatFormatted() {
		return chatFormatted(false);
	}

	public String chatFormatted(boolean escape) {
		if (slashCommand != 0L) {
			return "</" + stringId + ":" + Long.toUnsignedString(slashCommand) + ">";
		} else if (escape) {
			return '`' + name + '`';
		} else {
			return MacroCommands.FORMAT_ESCAPE.matcher(name).replaceAll("\\\\$1");
		}
	}

	public boolean isHidden() {
		return name.startsWith("__");
	}

	public void save() {
	}

	public void delete() {
	}

	@Override
	public String call() throws Exception {
		var path = getContentPath(false);

		if (Files.exists(path)) {
			return Files.readString(path);
		}

		return "";
	}
}