package dev.gnomebot.app.data.complex;

import discord4j.common.util.Snowflake;
import discord4j.core.object.component.ActionComponent;

import java.util.List;

public abstract class MEComponent {
	public abstract void getLines(List<String> lines);

	public abstract ActionComponent toActionComponent(Snowflake sender);
}