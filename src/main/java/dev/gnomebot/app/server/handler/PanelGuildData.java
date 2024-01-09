package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.server.AuthLevel;
import org.jetbrains.annotations.NotNull;

public record PanelGuildData(GuildCollections gc, AuthLevel authLevel) implements Comparable<PanelGuildData> {
	@Override
	public int compareTo(@NotNull PanelGuildData o) {
		return gc.toString().compareToIgnoreCase(o.gc.toString());
	}
}