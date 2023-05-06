package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import dev.gnomebot.app.BrainEvents;
import dev.gnomebot.app.data.GnomeAuditLogEntry;
import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.event.domain.VoiceStateUpdateEvent;
import discord4j.core.object.VoiceState;

import java.time.Instant;

/**
 * @author LatvianModder
 */
public class VoiceHandler {
	public static void stateUpdate(DiscordHandler handler, VoiceStateUpdateEvent event) {
		VoiceState oldState = event.getOld().orElse(null);
		VoiceState state = event.getCurrent();

		Instant now = Instant.now();

		GuildCollections gc = handler.app.db.guild(event.getCurrent().getGuildId());

		if (event.isJoinEvent()) {
			// App.info(Utils.ANSI_GREEN + gc + "/" + c.getName() + " " + Utils.ANSI_YELLOW + m.getTag() + Utils.ANSI_RESET + " Changed voice state: " + Utils.ANSI_CYAN + "JOIN");
			App.LOGGER.event(BrainEvents.VOICE_JOINED);

			if (state.getChannelId().isPresent()) {
				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.JOIN_VOICE)
						.channel(state.getChannelId().get())
						.user(state.getUserId())
				);
			}
		} else if (event.isLeaveEvent()) {
			// App.info(Utils.ANSI_GREEN + gc + "/" + c.getName() + " " + Utils.ANSI_YELLOW + m.getTag() + Utils.ANSI_RESET + " Changed voice state: " + Utils.ANSI_CYAN + "LEAVE");
			App.LOGGER.event(BrainEvents.VOICE_LEFT);

			if (oldState != null && oldState.getChannelId().isPresent()) {
				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.LEAVE_VOICE)
						.channel(oldState.getChannelId().get())
						.user(oldState.getUserId())
				);
			}
		} else if (event.isMoveEvent()) {
			App.LOGGER.event(BrainEvents.VOICE_CHANGED);

			if (oldState != null && oldState.getChannelId().isPresent()) {
				// App.info(Utils.ANSI_GREEN + gc + "/" + c.getName() + " " + Utils.ANSI_YELLOW + m.getTag() + Utils.ANSI_RESET + " Changed voice state: " + Utils.ANSI_CYAN + "LEAVE");

				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.LEAVE_VOICE)
						.timestamp(now)
						.channel(oldState.getChannelId().get())
						.user(oldState.getUserId())
				);
			}

			// App.info(Utils.ANSI_GREEN + gc + "/" + c.getName() + " " + Utils.ANSI_YELLOW + m.getTag() + Utils.ANSI_RESET + " Changed voice state: " + Utils.ANSI_CYAN + "JOIN");

			if (state.getChannelId().isPresent()) {
				gc.auditLog(GnomeAuditLogEntry.builder(GnomeAuditLogEntry.Type.JOIN_VOICE)
						.timestamp(now)
						.channel(state.getChannelId().get())
						.user(state.getUserId())
				);
			}
		}
	}
}
