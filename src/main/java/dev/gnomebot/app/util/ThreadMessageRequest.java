package dev.gnomebot.app.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = ThreadMessageRequest.class)
public class ThreadMessageRequest {
	private final String name;
	private final int auto_archive_duration;

	public ThreadMessageRequest(String n, int a) {
		name = n;
		auto_archive_duration = a;
	}

	public ThreadMessageRequest(String n) {
		this(n, 1440);
	}

	@JsonProperty("name")
	public String name() {
		return name;
	}

	@JsonProperty("auto_archive_duration")
	public int auto_archive_duration() {
		return auto_archive_duration;
	}
}
