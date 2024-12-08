package dev.gnomebot.app.data;

import dev.gnomebot.app.AppPaths;
import dev.gnomebot.app.data.complex.ComplexMessageRenderContext;
import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.MacroCommands;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.data.HexId32;
import dev.latvian.apps.webutils.data.Lazy;
import dev.latvian.apps.webutils.data.Pair;
import dev.latvian.apps.webutils.html.Tag;
import dev.latvian.apps.webutils.html.TagFunction;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Macro implements Comparable<Macro>, Callable<String>, TagFunction {
	public final GuildCollections gc;
	private final Lazy<String> content;
	public HexId32 id = HexId32.NONE;
	public String stringId = "";
	public String name = "";
	public long author = 0L;
	public Instant created = null;
	public long slashCommand = 0L;
	private String description;
	public String displayName = "";
	public ReactionEmoji emoji = null;

	private ChatCommandSuggestion chatCommandSuggestion;
	private Pair<ContentType, Object> cachedContent;

	public Macro(GuildCollections gc) {
		this.gc = gc;
		this.content = Lazy.of(this);
	}

	public Path getContentPath(boolean write) {
		var ids = id.toString();
		var dir = gc.paths.macros.resolve(ids.substring(0, 2));

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
			cachedContent = ContentType.parse(gc, getContent());
		}

		return cachedContent;

	}

	public MessageBuilder createMessage(ComplexMessageRenderContext ctx) {
		var cached = getCachedContent();
		var ctx2 = ctx.copy();
		ctx2.sourceGuild = gc;
		ctx2.cached = cached.b();
		ctx2.macro = this;
		return cached.a().render(ctx2);
	}

	public CompletableFuture<MessageBuilder> createMessageOrTimeout(ComplexMessageRenderContext ctx) {
		return CompletableFuture.supplyAsync(() -> createMessage(ctx)).completeOnTimeout(MessageBuilder.create("Macro timed out!"), 2500L, TimeUnit.MILLISECONDS);
	}

	public void rename(String rename) {
		var l = setSlashCommand(false);

		gc.getMacroMap().remove(stringId);
		stringId = rename.toLowerCase();
		name = rename;
		gc.getMacroMap().put(stringId, this);

		if (l != 0L && !isHidden()) {
			setSlashCommand(true);
		}
	}

	public long setSlashCommand(boolean b) {
		if (b) {
			var data = gc.getClient().getRestClient().getApplicationService().createGuildApplicationCommand(gc.db.app.discordHandler.selfId, gc.guildId, buildCommand()).block();

			var id = data == null ? 0L : data.id().asLong();

			if (data != null) {
				slashCommand = id;
			}

			return id;
		} else {
			if (slashCommand != 0L) {
				try {
					gc.getClient().getRestClient().getApplicationService().deleteGuildApplicationCommand(gc.db.app.discordHandler.selfId, gc.guildId, slashCommand).block();
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
			description = "Macro created by " + gc.db.app.discordHandler.getUserName(this.author).orElse("Deleted User");
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
		return gc.getMacroUses(id.getAsInt());
	}

	public void addUse() {
		gc.addMacroUse(id.getAsInt());
	}

	@Override
	public int compareTo(@NotNull Macro o) {
		return stringId.compareToIgnoreCase(o.stringId);
	}

	public String getDisplayName() {
		return displayName.isEmpty() ? name : displayName;
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

	public String url() {
		return gc.url() + "/macros/" + id;
	}

	@Override
	public void acceptTag(Tag parent) {
		acceptTag0(parent);
	}

	public Tag acceptTag0(Tag parent) {
		var t = parent.a(gc.url() + "/macros/" + id, name);

		if (getCachedContent().a() == ContentType.MACRO_BUNDLE) {
			t.classes("macro-bundle");
		}

		return t;
	}
}