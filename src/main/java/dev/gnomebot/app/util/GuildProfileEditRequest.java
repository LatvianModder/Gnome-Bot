package dev.gnomebot.app.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import discord4j.discordjson.possible.Possible;

@JsonSerialize(as = GuildProfileEditRequest.class)
public class GuildProfileEditRequest {
	public Possible<String> avatar = Possible.absent();

	@JsonProperty("avatar")
	public Possible<String> avatar() {
		return avatar;
	}
}
