package dev.gnomebot.app.discord.legacycommand;

import dev.gnomebot.app.discord.Emojis;
import discord4j.core.object.emoji.Emoji;
import discord4j.rest.http.client.ClientException;

public class GnomeException extends RuntimeException {
	public enum Type {
		ERROR,
		NO_PERMISSION,
		NOT_FOUND
	}

	public final Type type;
	public boolean ephemeral;
	public boolean deleteMessage;
	public int position;
	public Emoji reaction;
	public ClientException clientException;

	public GnomeException(Type t, String s) {
		super(s);
		type = t;
		ephemeral = false;
		deleteMessage = false;
		position = -1;
		reaction = Emojis.NO;
	}

	public GnomeException(String s) {
		this(Type.ERROR, s);
	}

	public GnomeException ephemeral() {
		ephemeral = true;
		return this;
	}

	public GnomeException deleteMessage() {
		deleteMessage = true;
		return this;
	}

	public GnomeException pos(int p) {
		position = p;
		return this;
	}

	public GnomeException reaction(Emoji r) {
		reaction = r;
		return this;
	}

	public GnomeException clientException(ClientException e) {
		clientException = e;
		return this;
	}
}