package dev.gnomebot.app.data.ai;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record AIResponse(int code, List<AIMessage> messages, @Nullable AIMessage query, AIMessage response) {
	public void save() {
		if (query != null) {
			messages.add(query);
			messages.add(response);
		}
	}
}
