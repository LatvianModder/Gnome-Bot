package dev.gnomebot.app.discord;

import dev.gnomebot.app.data.GnomeAuditLogEntry;
import discord4j.core.event.domain.VoiceStateUpdateEvent;

public class VoiceHandler {
	public static void stateUpdate(DiscordHandler handler, VoiceStateUpdateEvent event) {
		var oldState = event.getOld().orElse(null);
		var state = event.getCurrent();

		var gc = handler.app.db.guild(event.getCurrent().getGuildId());

		if (event.isJoinEvent()) {
			if (state.getChannelId().isPresent()) {
				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.JOIN_VOICE)
						.channel(state.getChannelId().get().asLong())
						.user(state.getUserId().asLong())
				);
			}
		} else if (event.isLeaveEvent()) {
			if (oldState != null && oldState.getChannelId().isPresent()) {
				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.LEAVE_VOICE)
						.channel(oldState.getChannelId().get().asLong())
						.user(oldState.getUserId().asLong())
				);
			}
		} else if (event.isMoveEvent()) {
			if (oldState != null && oldState.getChannelId().isPresent()) {
				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.LEAVE_VOICE)
						.channel(oldState.getChannelId().get().asLong())
						.user(oldState.getUserId().asLong())
				);
			}

			if (state.getChannelId().isPresent()) {
				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.JOIN_VOICE)
						.channel(state.getChannelId().get().asLong())
						.user(state.getUserId().asLong())
				);
			}
		}
	}
}
