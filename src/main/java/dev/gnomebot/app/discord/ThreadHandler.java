package dev.gnomebot.app.discord;

/**
 * @author LatvianModder
 */
public class ThreadHandler {
	/*
	public static void channelCreate(DiscordHandler handler, ThreadChannelCreateEvent event) {
		GuildCollections gc = handler.app.db.guild(event.getChannel().getGuildId());

		if (gc != null) {
			App.info("Thread channel create: " + gc + "/" + event.getChannel().getName());
		}
	}

	public static void channelDelete(DiscordHandler handler, ThreadChannelDeleteEvent event) {
		GuildCollections gc = handler.app.db.guild(event.getChannel().getGuildId());

		if (gc != null) {
			App.info("Thread channel delete: " + gc + "/" + event.getChannel().getId().asString());
		}
	}

	public static void channelUpdate(DiscordHandler handler, ThreadChannelUpdateEvent event) {
		GuildCollections gc = handler.app.db.guild(event.getChannel().getGuildId());

		if (gc != null) {
			App.info("Thread channel update: " + gc + "/" + event.getChannel().getName());
		}
	}
	 */

	// public static void memberUpdate(DiscordHandler handler, ThreadMemberUpdateEvent event) {
	// no guild ID?
		/*
		GuildCollections gc = handler.app.db.guild(event.getMember().getGuildId());

		if (gc != null) {
			App.info("Thread member update: " + event.getMember().getUserId());
		}
		 */
	// }

	/*
	public static void membersUpdate(DiscordHandler handler, ThreadMembersUpdateEvent event) {
		GuildCollections gc = handler.app.db.guild(event.getGuildId());

		if (gc != null) {
			App.info("Thread members update: " + gc + "/" + event.getMembers().stream().map(m -> m.getUserId().asString()).collect(Collectors.toList()));
		}
	}

	public static void listSync(DiscordHandler handler, ThreadListSyncEvent event) {
		GuildCollections gc = handler.app.db.guild(event.getGuildId());

		if (gc != null) {
			App.info("Thread list sync: " + gc + "/" + event.getSyncedThreads().stream().map(ThreadChannel::getName).collect(Collectors.toList()));
		}
	}
	 */
}
