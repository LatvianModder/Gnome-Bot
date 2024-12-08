package dev.gnomebot.app.data.complex;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.data.Macro;
import dev.gnomebot.app.discord.legacycommand.CommandReader;
import org.jetbrains.annotations.Nullable;

public class ComplexMessageRenderContext {
	public GuildCollections targetGuild;
	public long sender = 0L;

	public @Nullable CommandReader reader;
	public Object cached;

	public GuildCollections sourceGuild;
	public Macro macro = null;

	public ComplexMessageRenderContext() {
	}

	public ComplexMessageRenderContext(GuildCollections targetGuild, long sender) {
		this.targetGuild = targetGuild;
		this.sender = sender;
	}

	public ComplexMessageRenderContext copy() {
		var ctx = new ComplexMessageRenderContext();

		ctx.targetGuild = targetGuild;
		ctx.sender = sender;

		ctx.reader = reader;
		ctx.cached = cached;

		ctx.sourceGuild = sourceGuild;
		ctx.macro = macro;

		return ctx;
	}
}
