package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.channel.ChannelInfo;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.WebHookDestination;
import dev.gnomebot.app.discord.legacycommand.CommandContext;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import dev.gnomebot.app.discord.legacycommand.GnomeException;
import dev.gnomebot.app.util.BasicOption;
import discord4j.core.object.command.ApplicationCommandInteractionOption;
import discord4j.core.object.command.ApplicationCommandInteractionOptionValue;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;

import java.util.Objects;
import java.util.Optional;

public class CommandOption extends BasicOption {
	public final transient CommandContext context;
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

	public Optional<User> asUser() {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readUser();
	}

	public Optional<ChannelInfo> asChannelInfo() {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readChannelInfo();
	}

	public ChannelInfo asChannelInfoOrCurrent() {
		return asChannelInfo().orElse(context.channelInfo);
	}

	public Optional<Member> asMember() {
		var u = asUser();

		if (u.isPresent()) {
			try {
				return Optional.of(Objects.requireNonNull(context.gc.getMember(u.get().getId().asLong())));
			} catch (Exception ex) {
				throw new GnomeException("Too late! They're gone already.");
			}
		}

		return Optional.empty();
	}

	public Optional<Member> asOptionalMember() {
		var u = asUser();

		if (u.isPresent()) {
			try {
				return Optional.ofNullable(context.gc.getMember(u.get().getId().asLong()));
			} catch (Exception ex) {
				return Optional.empty();
			}
		}

		return Optional.empty();
	}

	public Optional<CachedRole> asRole() {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readRole();
	}

	public Optional<WebHookDestination> asWebhook() {
		var ci = asChannelInfo().orElse(null);

		if (ci != null) {
			context.checkSenderOwner();
			return ci.getWebHook();
		}

		var n = value.trim().toLowerCase();
		var webhook = context.gc.db.userWebhooksDB.query().eq("name", n).eq("user", context.sender.getId().asLong()).first();
		return Optional.ofNullable(webhook == null ? null : webhook.createWebhook());
	}
}
