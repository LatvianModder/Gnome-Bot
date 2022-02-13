package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.DiscordCommandException;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.BasicOption;
import dev.gnomebot.app.util.Pair;
import discord4j.common.util.Snowflake;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;

import java.util.Optional;

public class CommandOption extends BasicOption {
	public final CommandContext context;
	public final boolean focused;

	public CommandOption(CommandContext c, String n, String v, boolean f) {
		super(n, v);
		context = c;
		focused = f;
	}

	public CommandOption(CommandContext c, ApplicationCommandInteractionOption o) {
		this(c, o.getName(), o.getValue().map(ApplicationCommandInteractionOptionValue::getRaw).orElse(""), o.isFocused());
	}

	@Override
	public String toString() {
		return "CommandOption{" +
				"name='" + name + '\'' +
				", value='" + value + '\'' +
				(focused ? ", focused=true" : "") +
				'}';
	}

	public Optional<User> asUser() throws DiscordCommandException {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readUser();
	}

	public Optional<ChannelInfo> asChannelInfo() throws DiscordCommandException {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readChannelInfo();
	}

	public ChannelInfo asChannelInfoOrCurrent() throws DiscordCommandException {
		return asChannelInfo().orElse(context.channelInfo);
	}

	public Optional<Member> asMember() throws DiscordCommandException {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readMember();
	}

	public Optional<CachedRole> asRole() throws DiscordCommandException {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readRole();
	}

	public Optional<Pair<ChannelInfo, Snowflake>> asChannelAndMessage() {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readChannelAndMessage();
	}

	public Optional<WebHook> asWebhook() throws DiscordCommandException {
		ChannelInfo ci = asChannelInfo().orElse(null);

		if (ci != null) {
			if (!context.gc.getAuthLevel(context.sender).is(AuthLevel.OWNER)) {
				throw new DiscordCommandException(DiscordCommandException.Type.NO_PERMISSION, "Wait a minute, you're not the server owner! Only the server owner can do this");
			}

			return Optional.ofNullable(ci.getOrCreateWebhook());
		}

		String n = value.trim().toLowerCase();
		UserWebhook webhook = context.gc.db.userWebhooks.query().eq("name", n).eq("user", context.sender.getId().asLong()).first();
		return Optional.ofNullable(webhook == null ? null : webhook.createWebhook());
	}
}
