package dev.gnomebot.app.data;

import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.gnomebot.app.util.MapWrapper;
import org.bson.conversions.Bson;

import java.util.Date;
import java.util.List;

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

	public String getUIDString() {
		return document.getString("_id");
	}

	public Date timestamp() {
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
		return o == this || o instanceof WrappedDocument<?> d && getUID() == d.getUID();
	}

	@Override
	public String toString() {
		return getID();
	}

	public UpdateResult update(Bson update) {
		var id = document.get("_id", null);

		if (id != null) {
			return collection.query(id).update(update);
		}

		return CollectionQuery.ZERO_UPDATES;
	}

	public UpdateResult update(String key, Object value) {
		document.map.put(key, value);
		return update(Updates.set(key, value));
	}

	public UpdateResult update(List<Bson> updates) {
		if (!updates.isEmpty()) {
			return update(updates.size() == 1 ? updates.getFirst() : Updates.combine(updates));
		}

		return CollectionQuery.ZERO_UPDATES;
	}

	public final DeleteResult delete() {
		var id = document.get("_id", null);

		if (id != null) {
			return collection.query(id).delete();
		}

		return CollectionQuery.ZERO_DELETES;
	}
}