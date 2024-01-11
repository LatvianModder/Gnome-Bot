package dev.gnomebot.app.discord.command.admin;

import dev.gnomebot.app.Assets;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.MemberHandler;
import dev.gnomebot.app.discord.command.ApplicationCommands;
import dev.gnomebot.app.discord.command.ChatInputInteractionEventWrapper;
import dev.gnomebot.app.util.EmbedBuilder;

import java.time.Instant;
import java.util.stream.Collectors;

public class LockdownCommand extends ApplicationCommands {
	public static void enable(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderAdmin();

		var wasOff = !event.context.gc.lockdownMode.get();

		var sec = Math.min(event.get("kick-time").asSeconds().orElse(300L), 86400L);

		if (wasOff) {
			event.acknowledge();
		} else {
			event.acknowledgeEphemeral();
		}

		if (wasOff) {
			event.context.gc.lockdownMode.set(true);

			event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.LOCKDOWN_ENABLED)
					.source(event.context.sender)
			);

			if (event.context.channelInfo == null || !event.context.gc.adminLogChannel.is(event.context.channelInfo.id)) {
				event.context.gc.adminLogChannelEmbed(null, event.context.gc.adminLogChannel, spec -> {
					spec.title("Lockdown mode enabled!");
					spec.description(Emojis.ALERT.asFormat());
					spec.thumbnail(Assets.EMERGENCY.getPath());
					spec.author(event.context.sender.getUsername(), event.context.sender.getAvatarUrl());
				});
			}

			if (sec > 0L) {
				var time = Instant.now().getEpochSecond() - sec;
				var list = event.context.gc.getGuild().getMembers().filter(member -> member.getJoinTime().isPresent() && member.getJoinTime().get().getEpochSecond() > time).toStream().collect(Collectors.toList());

				list.forEach(m -> {
					if (event.context.gc.logLeavingChannel.isSet()) {
						MemberHandler.lockdownKicks.add(m.getId());
					}

					m.kick("Lockdown Mode").subscribe();
				});
			}

			event.respond(EmbedBuilder.create("Lockdown mode enabled!", Emojis.ALERT.asFormat()).redColor().thumbnail(Assets.EMERGENCY.getPath()));
		} else {
			event.respond("Lockdown mode is already enabled!");
		}
	}

	public static void disable(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderAdmin();

		boolean wasOn = event.context.gc.lockdownMode.get();

		if (wasOn) {
			if (event.context.channelInfo == null || !event.context.gc.adminLogChannel.is(event.context.channelInfo.id)) {
				event.acknowledgeEphemeral();
			} else {
				event.acknowledge();
			}

			event.context.gc.lockdownMode.set(false);

			event.context.gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.LOCKDOWN_DISABLED)
					.source(event.context.sender)
			);

			if (event.context.channelInfo == null || !event.context.gc.adminLogChannel.is(event.context.channelInfo.id)) {
				event.context.gc.adminLogChannelEmbed(null, event.context.gc.adminLogChannel, spec -> {
					spec.title("Lockdown mode disabled!");
					spec.color(EmbedColor.GREEN);
					spec.description(Emojis.ALERT.asFormat());
					spec.thumbnail(Assets.EMERGENCY.getPath());
					spec.author(event.context.sender.getUsername(), event.context.sender.getAvatarUrl());
				});
			}

			event.respond(EmbedBuilder.create("Lockdown mode disabled!", Emojis.ALERT.asFormat()).greenColor().thumbnail(Assets.EMERGENCY.getPath()));
		} else {
			event.respond("Lockdown mode is already disabled!");
		}
	}
}