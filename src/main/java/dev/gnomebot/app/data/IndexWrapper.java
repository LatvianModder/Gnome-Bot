package dev.gnomebot.app.data;

import com.mongodb.client.model.IndexOptions;
import org.bson.conversions.Bson;

public class IndexWrapper {
	public final String indexId;
	public final Bson keys;
	public final IndexOptions indexOptions;

	public IndexWrapper(String id, Bson k, IndexOptions options) {
		indexId = id;
		keys = k;
		indexOptions = options.name(id);
	}
}
