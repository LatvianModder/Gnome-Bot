package dev.gnomebot.app.data;

import dev.latvian.apps.webutils.ansi.Table;

import java.time.Instant;
import java.util.Comparator;

/**
 * @author LatvianModder
 */
public class ExportedMessage implements Comparable<ExportedMessage> {
	public static final Comparator<ExportedMessage> COMPARATOR = ExportedMessage::compareTo;

	public long timestamp;
	public long channel;
	public String channelName;
	public long flags;
	public String content;

	@Override
	public int compareTo(ExportedMessage o) {
		return Long.compare(timestamp, o.timestamp);
	}

	public void toString(int i, String[] row) {
		row[0] = Integer.toString(i);
		row[1] = Table.escapeCSVSpecialCharacters(Instant.ofEpochMilli(timestamp).toString());
		row[2] = Long.toUnsignedString(channel);
		row[3] = channelName;
		row[4] = Long.toUnsignedString(flags);
		row[5] = Table.escapeCSVSpecialCharacters(content);
	}
}