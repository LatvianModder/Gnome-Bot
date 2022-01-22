package dev.gnomebot.app.server.handler;

import dev.gnomebot.app.server.AuthLevel;
import discord4j.common.util.Snowflake;

public record PanelGuildData(Snowflake id, String name, Snowflake owner, AuthLevel authLevel) {
}