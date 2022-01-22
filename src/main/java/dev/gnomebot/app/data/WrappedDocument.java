package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import dev.gnomebot.app.util.MapWrapper;
import discord4j.common.util.Snowflake;
import org.bson.conversions.Bson;

import java.util.Date;
import java.util.List;

/**
 * @author LatvianModder
 */
public abstract class WrappedDocument<T extends WrappedDocument<T>> {
	public final WrappedCollection<T> collection;
	public final MapWrapper document;
	private long uid = 0L;
	private String id = null;
	protected String name = null;

	public WrappedDocument(WrappedCollection<T> c, MapWrapper d) {
		collection = c;
		document = d;
	}

	public long getUID() {
		if (uid == 0L) {
			uid = document.getLong("_id");
		}

		return uid;
	}

	public Snowflake getUIDSnowflake() {
		return Snowflake.of(getUID());
	}

	public String getUIDString() {
		return document.getString("_id");
	}

	public Date getDate() {
		return document.getObjectId("_id").getDate();
	}

	public String getID() {
		if (id == null) {
			id = document.getString("id");

			if (id.isEmpty()) {
				id = getUIDString();
			}
		}

		return id;
	}

	public String getName() {
		if (name == null) {
			name = document.getString("name");

			if (name.isEmpty()) {
				name = getID();
			}
		}

		return name;
	}

	@Override
	public int hashCode() {
		return Long.hashCode(getUID());
	}

	public boolean equals(Object o) {
		if (o == this) {
			return true;
		} else if (o instanceof WrappedDocument) {
			return getUID() == ((WrappedDocument<?>) o).getUID();
		}

		return false;
	}

	@Override
	public String toString() {
		return getID();
	}

	public void update(Bson update) {
		Object id = document.get("_id", null);

		if (id != null) {
			collection.query(id).update(update);
		}
	}

	public void update(String key, Object value) {
		document.map.put(key, value);
		update(Updates.set(key, value));
	}

	public void update(List<Bson> updates) {
		if (!updates.isEmpty()) {
			update(updates.size() == 1 ? updates.get(0) : Updates.combine(updates));
		}
	}

	public final void delete() {
		Object id = document.get("_id", null);

		if (id != null) {
			collection.query(id).delete();
		}
	}
}