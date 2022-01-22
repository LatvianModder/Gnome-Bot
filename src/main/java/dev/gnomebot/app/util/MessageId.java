package dev.gnomebot.app.util;

import discord4j.core.object.entity.Message;

import java.util.Objects;

public final class MessageId {
	public final long channel;
	public final long id;

	public MessageId(long c, long m) {
		channel = c;
		id = m;
	}

	public MessageId(Message message) {
		this(message.getChannelId().asLong(), message.getId().asLong());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MessageId messageId = (MessageId) o;
		return channel == messageId.channel && id == messageId.id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(channel, id);
	}
}
