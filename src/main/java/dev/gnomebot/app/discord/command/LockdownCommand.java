package dev.gnomebot.app.discord.command;

import dev.gnomebot.app.Assets;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.MemberHandler;
import dev.gnomebot.app.util.EmbedBuilder;
import discord4j.core.object.entity.Member;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author LatvianModder
 */
public class LockdownCommand extends ApplicationCommands {
	@RegisterCommand
	public static final ChatInputInteractionBuilder COMMAND = chatInputInteraction("lockdown")
			.add(sub("enable")
					.description("Enables lockdown mode")
					.add(time("kick_time", false, true))
					.run(LockdownCommand::enable)
			)
			.add(sub("disable")
					.description("Disables lockdown mode")
					.run(LockdownCommand::disable)
			);

	private static void enable(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderAdmin();

		boolean wasOff = !event.context.gc.lockdownMode.get();

		long sec = Math.min(event.get("kick_time").asSeconds().orElse(300L), 86400L);

		if (wasOff) {
			event.acknowledge();
		} else {
			event.acknowledgeEphemeral();
		}

		if (wasOff) {
			event.context.gc.lockdownMode.set(true);
			event.context.gc.lockdownMode.save();

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
				long time = Instant.now().getEpochSecond() - sec;
				List<Member> list = event.context.gc.getGuild().getMembers().filter(member -> member.getJoinTime().isPresent() && member.getJoinTime().get().getEpochSecond() > time).toStream().collect(Collectors.toList());

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

	private static void disable(ChatInputInteractionEventWrapper event) {
		event.context.checkSenderAdmin();

		boolean wasOn = event.context.gc.lockdownMode.get();

		if (wasOn) {
			if (event.context.channelInfo == null || !event.context.gc.adminLogChannel.is(event.context.channelInfo.id)) {
				event.acknowledgeEphemeral();
			} else {
				event.acknowledge();
			}

			event.context.gc.lockdownMode.set(false);
			event.context.gc.lockdownMode.save();

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