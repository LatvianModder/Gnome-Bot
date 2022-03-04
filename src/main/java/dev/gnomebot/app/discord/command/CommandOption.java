package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.data.ChannelInfo;
import dev.gnomebot.app.data.UserWebhook;
import dev.gnomebot.app.discord.CachedRole;
import dev.gnomebot.app.discord.WebHook;
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
		Optional<User> u = asUser();

		if (u.isPresent()) {
			try {
				return Optional.of(Objects.requireNonNull(context.gc.getMember(u.get().getId())));
			} catch (Exception ex) {
				throw new GnomeException("Too late! They're gone already.");
			}
		}

		return Optional.empty();
	}

	public Optional<Member> asOptionalMember() {
		Optional<User> u = asUser();

		if (u.isPresent()) {
			try {
				return Optional.ofNullable(context.gc.getMember(u.get().getId()));
			} catch (Exception ex) {
				return Optional.empty();
			}
		}

		return Optional.empty();
	}

	public Optional<CachedRole> asRole() {
		return value.isEmpty() ? Optional.empty() : new CommandReader(context.gc, value).readRole();
	}

	public Optional<WebHook> asWebhook() {
		ChannelInfo ci = asChannelInfo().orElse(null);

		if (ci != null) {
			context.checkSenderOwner();
			return ci.getWebHook();
		}

		String n = value.trim().toLowerCase();
		UserWebhook webhook = context.gc.db.userWebhooks.query().eq("name", n).eq("user", context.sender.getId().asLong()).first();
		return Optional.ofNullable(webhook == null ? null : webhook.createWebhook());
	}
}
