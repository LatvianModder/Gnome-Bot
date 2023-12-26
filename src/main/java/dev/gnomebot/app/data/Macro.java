package dev.gnomebot.app.data;

import dev.gnomebot.app.discord.command.ChatCommandSuggestion;
import dev.gnomebot.app.discord.command.MacroCommands;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.util.MessageBuilder;
import dev.latvian.apps.webutils.data.Pair;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandOption;
import discord4j.discordjson.json.ApplicationCommandOptionData;
import discord4j.discordjson.json.ApplicationCommandRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;

public class Macro implements Comparable<Macro> {
	public final GuildCollections guild;
	public String id = "";
	public String name = "";
	public String content = "";
	public long author = 0L;
	public Instant created = null;
	public int uses = 0;
	public long slashCommand = 0L;
	private String description;

	private ChatCommandSuggestion chatCommandSuggestion;
	private Pair<ContentType, Object> cachedContent;

	public Macro(GuildCollections guild) {
		this.guild = guild;
	}

	public Pair<ContentType, Object> getCachedContent() {
		if (cachedContent == null) {
			cachedContent = ContentType.parse(guild, content);
		}

		return cachedContent;

	}

	public MessageBuilder createMessage(@Nullable CommandReader reader, Snowflake sender) {
		return getCachedContent().a().render(reader, getCachedContent().b(), sender);
	}

	public void rename(String rename) {
		long l = setSlashCommand(false);

		guild.getMacroMap().remove(id);
		id = rename.toLowerCase();
		name = rename;
		guild.getMacroMap().put(id, this);

		if (l != 0L && !isHidden()) {
			setSlashCommand(true);
		}
	}

	public void updateContent(String content) {
		this.content = content;
		this.cachedContent = null;
		this.chatCommandSuggestion = null;
	}

	public long setSlashCommand(boolean b) {
		if (b) {
			var data = guild.getClient().getRestClient().getApplicationService().createGuildApplicationCommand(guild.db.app.discordHandler.applicationId, guild.guildId.asLong(), buildCommand()).block();

			long id = data == null ? 0L : Snowflake.of(data.id()).asLong();

			if (data != null) {
				slashCommand = id;
			}

			return id;
		} else {
			if (slashCommand != 0L) {
				try {
					guild.getClient().getRestClient().getApplicationService().deleteGuildApplicationCommand(guild.db.app.discordHandler.applicationId, guild.guildId.asLong(), slashCommand).block();
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
			description = "Macro created by " + guild.db.app.discordHandler.getUserName(Snowflake.of(this.author)).orElse("Deleted User");
		}

		return description;
	}

	public ApplicationCommandRequest buildCommand() {
		var builder = ApplicationCommandRequest.builder();
		builder.name(id);
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
			chatCommandSuggestion = new ChatCommandSuggestion(name, name, id, 0);
		}

		return chatCommandSuggestion;
	}

	public void addUse() {
		uses++;
	}

	@Override
	public int compareTo(@NotNull Macro o) {
		return id.compareToIgnoreCase(o.id);
	}

	public String chatFormatted() {
		return chatFormatted(false);
	}

	public String chatFormatted(boolean escape) {
		if (slashCommand != 0L) {
			return "</" + id + ":" + Long.toUnsignedString(slashCommand) + ">";
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
}