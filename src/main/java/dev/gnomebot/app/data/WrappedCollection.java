package dev.gnomebot.app.data;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import dev.gnomebot.app.util.MapWrapper;
import dev.latvian.apps.ansi.log.Log;
import discord4j.common.util.Snowflake;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

public class WrappedCollection<T extends WrappedDocument<T>> {
	public final Databases db;
	public final MongoDatabase database;
	public final String id;
	public final BiFunction<WrappedCollection<T>, MapWrapper, T> wrapper;
	private MongoCollection<Document> collection;
	private List<IndexWrapper> indexes;
	public GuildCollections gc;

	public WrappedCollection(Databases db, MongoDatabase database, String ci, BiFunction<WrappedCollection<T>, MapWrapper, T> w) {
		this.db = db;
		this.database = database;
		this.id = ci;
		this.wrapper = w;
	}

	public WrappedCollection<T> gc(GuildCollections g) {
		gc = g;
		return this;
	}

	public WrappedCollection<T> index(IndexWrapper wrapper) {
		if (indexes == null) {
			indexes = new ArrayList<>();
		}

		indexes.add(wrapper);
		return this;
	}

	public WrappedCollection<T> expires(String indexId, String key, long time, TimeUnit timeUnit, @Nullable Bson filter) {
		return index(new IndexWrapper(indexId, Indexes.ascending(key), new IndexOptions().partialFilterExpression(filter).expireAfter(time, timeUnit)));
	}

	public WrappedCollection<T> expiresAfterMonth(String indexId, String key, @Nullable Bson filter) {
		return expires(indexId, key, 28L, TimeUnit.DAYS, filter);
	}

	public synchronized MongoCollection<Document> getCollection() {
		if (collection == null) {
			//App.info("Initialized collection " + id);

			boolean newCollection = true;

			for (var name : database.listCollectionNames()) {
				if (name.equals(id)) {
					newCollection = false;
					break;
				}
			}

			collection = database.getCollection(id);

			if (newCollection && indexes != null) {
				for (var iw : indexes) {
					try {
						// collection.dropIndex(iw.indexId);
						collection.createIndex(iw.keys, iw.indexOptions);
					} catch (Exception ex) {
						Log.error(ex);
					}
				}
			}
		}

		return collection;
	}

	public T create(MapWrapper doc) {
		return wrapper.apply(this, doc);
	}

	public long count() {
		return getCollection().countDocuments();
	}

	public long count(Bson filters) {
		return getCollection().countDocuments(filters);
	}

	public CollectionQuery<T> query() {
		return new CollectionQuery<>(this);
	}

	public CollectionQuery<T> query(@Nullable Object id) {
		return query().id(id);
	}

	public void insert(Document document) {
		getCollection().insertOne(document);
	}

	public void insertMany(List<Document> documents) {
		while (documents.size() > 100_000) {
			getCollection().insertMany(documents.subList(0, 100_000));
			documents = documents.subList(100_000, documents.size());
		}

		if (!documents.isEmpty()) {
			getCollection().insertMany(documents);
		}
	}

	public AggregateIterable<Document> aggregate(List<Bson> pipeline) {
		return getCollection().aggregate(pipeline);
	}

	public void drop() {
		getCollection().drop();
	}

	@Nullable
	public T findFirst(@Nullable Object id) {
		return switch (id) {
			case null -> null;
			case Long l when l == 0L -> null;
			case Snowflake s when s.asLong() == 0L -> null;
			case String s when id.toString().isEmpty() -> null;
			default -> query(id).first();
		};
	}
}