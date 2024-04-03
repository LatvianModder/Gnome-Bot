package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.Emojis;
import dev.gnomebot.app.discord.MemberCache;
import dev.gnomebot.app.server.AuthLevel;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.SnowFlake;
import dev.latvian.apps.webutils.json.JSONObject;
import discord4j.core.spec.EmbedCreateFields;
import discord4j.rest.util.Color;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.Objects;

public class DiscordFeedback extends WrappedDocument<DiscordFeedback> {
	public enum Status {
		NONE(0, "None", EmbedColor.BLUE, ""),
		APPROVED(1, "Approved", EmbedColor.GREEN, "Approved"),
		DENIED(2, "Denied", EmbedColor.RED, "Denied"),
		CONSIDERED(3, "Considered", EmbedColor.TEAL, "Considered"),
		IMPLEMENTED(4, "Implemented", EmbedColor.GREEN, "Implemented");

		public static final Status[] VALUES = values();

		public final int id;
		public final String name;
		public final Color color;
		public final String titleSuffix;

		Status(int i, String n, Color c, String t) {
			id = i;
			name = n;
			color = c;
			titleSuffix = t;
		}

		public boolean isNone() {
			return this == NONE;
		}

		public boolean canEdit() {
			return isNone() || this == CONSIDERED;
		}
	}

	public DiscordFeedback(WrappedCollection<DiscordFeedback> c, MapWrapper d) {
		super(c, d);
	}

	public long getAuthor() {
		return document.getLong("author");
	}

	@Override
	public Date getDate() {
		return document.getDate("timestamp");
	}

	public int getNumber() {
		return document.getInt("number");
	}

	public String getContent() {
		return document.getString("content");
	}

	public Status getStatus() {
		return Status.VALUES[document.getInt("status")];
	}

	public String getReason() {
		return document.getString("reason");
	}

	public long getReasonAuthor() {
		return document.getLong("reason_author");
	}

	public int[] countVotes() {
		var v = new int[2];

		for (var o : getVotes().map.values()) {
			if (Boolean.TRUE.equals(o)) {
				v[0]++;
			} else {
				v[1]++;
			}
		}

		return v;
	}

	public MapWrapper getVotes() {
		return document.getMap("votes");
	}

	public Vote getVote(String key) {
		return Vote.fromBoolean(getVotes().getNullableBoolean(key));
	}

	public boolean setVote(String key, Vote vote) {
		var v = getVote(key);

		if (!Objects.equals(vote, v)) {
			if (vote == Vote.NONE) {
				getVotes().map.remove(key);
				update(Updates.unset("votes." + key));
			} else {
				getVotes().map.put(key, vote.value);
				update("votes." + key, vote.value);
			}

			return true;
		}

		return false;
	}

	public boolean isDeleted() {
		return document.getBoolean("deleted");
	}

	public EmbedBuilder edit(GuildCollections gc, @Nullable EmbedCreateFields.Footer footer) {
		var builder = EmbedBuilder.create();

		var status = getStatus();
		var v = countVotes();

		if (status == Status.NONE) {
			builder.title("Suggestion #" + getNumber());
		} else {
			builder.title("Suggestion #" + getNumber() + " - " + status.titleSuffix);
		}

		builder.url(App.url("guild/feedback/" + gc.guildId + "/" + getNumber()));
		builder.description(getContent());
		builder.field(Emojis.VOTEUP.asFormat() + " Upvotes", "**" + v[0] + "** [" + (v[0] * 100 / Math.max(1, v[0] + v[1])) + "%]", true);
		builder.field(Emojis.VOTEDOWN.asFormat() + " Downvotes", "**" + v[1] + "** [" + (v[1] * 100 / Math.max(1, v[0] + v[1])) + "%]", true);
		builder.color(status.color);
		builder.footer(footer);

		builder.timestamp(getDate().toInstant());

		if (status != Status.NONE) {
			if (gc.anonymousFeedback.get()) {
				builder.field("Reason", getReason(), false);
			} else {
				builder.field("Reason", getReason() + " - <@" + getReasonAuthor() + ">", false);
			}
		}

		return builder;
	}

	public static boolean canSee(GuildCollections gc, AuthLevel authLevel) {
		return !gc.anonymousFeedback.get() || authLevel.is(AuthLevel.OWNER) || (gc.adminsBypassAnonFeedback.get() && authLevel.is(AuthLevel.ADMIN));
	}

	public void toJson(JSONObject json, MemberCache memberCache, boolean canSee, boolean owner) {
		var voteMap = getVotes();
		var v = new int[2];

		for (var o : voteMap.map.values()) {
			if (Boolean.TRUE.equals(o)) {
				v[0]++;
			} else {
				v[1]++;
			}
		}

		json.put("id", SnowFlake.str(getUID()));
		json.put("number", getNumber());
		json.put("content", getContent());
		json.put("author", canSee ? SnowFlake.str(getAuthor()) : "0");
		json.put("author_name", canSee ? memberCache.getDisplayName(getAuthor()) : "Anonymous");
		json.put("status", getStatus().name.toLowerCase());

		if (getReasonAuthor() != 0L) {
			json.put("reason", getReason());
			json.put("reason_author", canSee ? SnowFlake.str(getReasonAuthor()) : "0");
			json.put("reason_author_name", canSee ? memberCache.getDisplayName(getReasonAuthor()) : "Anonymous");
		}

		json.put("created", getDate().toInstant().toString());
		json.put("upvotes", v[0]);
		json.put("downvotes", v[1]);
		json.put("deleted", isDeleted());

		if (owner) {
			var upTags = json.addArray("upvoters");
			var downTags = json.addArray("downvoters");

			for (var o : voteMap.map.entrySet()) {
				if (Boolean.TRUE.equals(o.getValue())) {
					upTags.add(memberCache.getDisplayName(SnowFlake.num(o.getKey())));
				} else {
					downTags.add(memberCache.getDisplayName(SnowFlake.num(o.getKey())));
				}
			}
		}
	}
}