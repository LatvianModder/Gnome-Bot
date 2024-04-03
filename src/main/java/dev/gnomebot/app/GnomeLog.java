package dev.gnomebot.app;

import dev.latvian.apps.webutils.ansi.AnsiComponent;
import dev.latvian.apps.webutils.ansi.Log;
import discord4j.rest.http.client.ClientException;

public class GnomeLog extends Log {
	@Override
	public AnsiComponent format(Object message, Type type) {
		if (message instanceof ClientException) {
			return super.format(((ClientException) message).getMessage(), type);
		}

		return super.format(message, type);
	}
}
