package dev.gnomebot.app.discord;

import dev.gnomebot.app.App;
import discord4j.core.event.domain.thread.ThreadChannelCreateEvent;
import discord4j.core.event.domain.thread.ThreadChannelDeleteEvent;
import discord4j.core.event.domain.thread.ThreadChannelUpdateEvent;
import discord4j.core.event.domain.thread.ThreadListSyncEvent;
import discord4j.core.event.domain.thread.ThreadMemberUpdateEvent;
import discord4j.core.event.domain.thread.ThreadMembersUpdateEvent;

/**
 * @author LatvianModder
 */
public class ThreadHandler {
	public static void channelCreate(DiscordHandler handler, ThreadChannelCreateEvent event) {
		var gc = handler.app.db.guild(event.getChannel().getGuildId());

		if (gc != null) {
			// App.info("Thread channel create: " + gc + "/" + event.getChannel().getName());
		}
	}

	public static void channelDelete(DiscordHandler handler, ThreadChannelDeleteEvent event) {
		var gc = handler.app.db.guild(event.getChannel().getGuildId());

		if (gc != null) {
			// App.info("Thread channel delete: " + gc + "/" + event.getChannel().getId().asString());
		}
	}

	public static void channelUpdate(DiscordHandler handler, ThreadChannelUpdateEvent event) {
		var gc = handler.app.db.guild(event.getChannel().getGuildId());

		if (gc != null) {
			if (event.getChannel().getParentId().isPresent() && event.getChannel().getParentId().get().equals(gc.adminLogChannel.get())) {
				App.info("Audit thread update: " + gc + "/" + event.getChannel().getName());
			}

			/*
			if(event.getChannel().isArchived() && event.getChannel().getParentId().isPresent() && event.getChannel().getParentId().get().equals(gc.adminLogChannel.get())) {
				event.getChannel().edit().withArchived(true).subscribe();
			}
			 */
		}
	}

	public static void memberUpdate(DiscordHandler handler, ThreadMemberUpdateEvent event) {
		// no guild ID?
		/*
		GuildCollections gc = handler.app.db.guild(event.getMember().getGuildId());

		if (gc != null) {
			App.info("Thread member update: " + event.getMember().getUserId());
		}
		 */

		// App.info("Thread member update: " + event.getMember().getUserId());
	}

	public static void membersUpdate(DiscordHandler handler, ThreadMembersUpdateEvent event) {
		var gc = handler.app.db.guild(event.getGuildId());

		if (gc != null) {
			// App.info("Thread members update: " + gc + "/" + event.getMembers().stream().map(m -> m.getUserId().asString()).toList());
		}
	}

	public static void listSync(DiscordHandler handler, ThreadListSyncEvent event) {
		var gc = handler.app.db.guild(event.getGuildId());

		if (gc != null) {
			// App.info("Thread list sync: " + gc + "/" + event.getSyncedThreads().stream().map(ThreadChannel::getName).toList());
		}
	}
}
