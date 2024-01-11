package dev.gnomebot.app.discord.interaction;

import dev.gnomebot.app.data.Databases;
import dev.gnomebot.app.discord.ComponentEventWrapper;
import org.bson.Document;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface CustomInteractionTypes {
	Map<String, CustomInteractionType> MAP = new HashMap<>();
	Map<UUID, CustomInteractionCallback> TEMP = new HashMap<>();
	Object TEMP_LOCK = new Object();

	static CustomInteractionType register(String id, CustomInteractionCallback.Provider callback) {
		var type = new CustomInteractionType(id, callback);
		MAP.put(id, type);
		return type;
	}

	CustomInteractionType NONE = register("none", NoInteractionCallback::new);

	static void init() {
	}

	@Nullable
	static CustomInteractionCallback get(Databases db, UUID id) {
		synchronized (TEMP_LOCK) {
			var c = TEMP.get(id);

			if (c == null) {
				var doc = db.interactions.query().eq("idm", id.getMostSignificantBits()).eq("idl", id.getLeastSignificantBits()).first();

				if (doc != null) {
					var t = MAP.get(doc.getType());

					if (t != null) {
						return t.callback.create(doc.document);
					}
				}
			}

			return c;
		}
	}

	@Nullable
	static CustomInteractionCallback remove(Databases db, UUID id) {
		var c = get(db, id);

		if (c != null) {
			synchronized (TEMP_LOCK) {
				if (TEMP.remove(id) != c) {
					db.interactions.query().eq("idm", id.getMostSignificantBits()).eq("idl", id.getLeastSignificantBits()).delete();
				}
			}
		}

		return c;
	}

	@Nullable
	static CustomInteractionCallback execute(Databases db, UUID id, ComponentEventWrapper event) {
		var c = get(db, id);

		if (c != null) {
			if (!c.getType().keep) {
				synchronized (TEMP_LOCK) {
					if (TEMP.remove(id) != c) {
						db.interactions.query().eq("idm", id.getMostSignificantBits()).eq("idl", id.getLeastSignificantBits()).delete();
					}
				}
			}

			c.execute(event);
		}

		return c;
	}

	static void create(Databases db, CustomInteractionCallback callback) {
		synchronized (TEMP_LOCK) {
			if (callback.getType().persistent) {
				var doc = new Document();
				doc.put("idm", callback.id.getMostSignificantBits());
				doc.put("idl", callback.id.getLeastSignificantBits());
				callback.save(doc);
				db.interactions.insert(doc);
			} else {
				TEMP.put(callback.id, callback);
			}
		}
	}
}
