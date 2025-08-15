package dev.gnomebot.app.data;

import dev.gnomebot.app.util.MapWrapper;
import dev.latvian.apps.codec.Codec;
import dev.latvian.apps.codec.MapCodec;

public class EchoLogEntry extends WrappedDocument<EchoLogEntry> {
	public record Data(long id, long channel, long author, String content) {
		public static final MapCodec<Data> CODEC = MapCodec.of(Data::new,
				Codec.LONG.optionalField("_id", 0L).of(Data::id),
				Codec.LONG.optionalField("channel", 0L).of(Data::channel),
				Codec.LONG.optionalField("author", 0L).of(Data::author),
				Codec.STRING.optionalField("content", "").of(Data::content)
		);
	}

	public EchoLogEntry(WrappedCollection<EchoLogEntry> c, MapWrapper d) {
		super(c, d);
	}

	public long channel() {
		return document.getLong("channel");
	}

	public long author() {
		return document.getLong("author");
	}

	public String content() {
		return document.getString("content");
	}
}