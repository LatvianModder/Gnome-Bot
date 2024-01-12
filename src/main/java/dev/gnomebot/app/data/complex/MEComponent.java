package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.GuildCollections;
import discord4j.core.object.component.ActionComponent;

import java.util.List;

public abstract class MEComponent {
	public abstract void getLines(List<String> lines);

	public abstract ActionComponent toActionComponent(GuildCollections sourceGuild, GuildCollections targetGuild, long sender);
}