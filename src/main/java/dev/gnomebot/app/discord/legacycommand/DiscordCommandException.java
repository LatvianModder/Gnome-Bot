package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.discord.Emojis;
import discord4j.core.object.reaction.ReactionEmoji;

/**
 * @author LatvianModder
 */
public class DiscordCommandException extends Exception {
	public enum Type {
		ERROR,
		NO_PERMISSION,
		NOT_FOUND
	}

	public final Type type;
	public boolean ephemeral;
	public boolean deleteMessage;
	public int position;
	public ReactionEmoji reaction;

	public DiscordCommandException(Type t, String s) {
		super(s);
		type = t;
		ephemeral = false;
		deleteMessage = false;
		position = -1;
		reaction = Emojis.NO_ENTRY;
	}

	public DiscordCommandException(String s) {
		this(Type.ERROR, s);
	}

	public DiscordCommandException ephemeral() {
		ephemeral = true;
		return this;
	}

	public DiscordCommandException deleteMessage() {
		deleteMessage = true;
		return this;
	}

	public DiscordCommandException pos(int p) {
		position = p;
		return this;
	}

	public DiscordCommandException reaction(ReactionEmoji r) {
		reaction = r;
		return this;
	}
}