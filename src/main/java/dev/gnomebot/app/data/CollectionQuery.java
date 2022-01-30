package dev.gnomebot.app.data;

import com.mongodb.client.FindIterable;
import com.mongodb.client.model.CountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.gnomebot.app.util.MapWrapper;
import dev.gnomebot.app.util.Utils;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Entity;
import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CollectionQuery<T extends WrappedDocument<T>> implements Iterable<T> {
	public static final DeleteResult ZERO_DELETES = DeleteResult.acknowledged(0L);
	public static final UpdateResult ZERO_UPDATES = UpdateResult.acknowledged(0L, 0L, null);

	private static class WrappedIterator<E extends WrappedDocument<E>> implements Iterator<E> {
		private final CollectionQuery<E> iterable;
		private final Iterator<Document> parent;

		private WrappedIterator(CollectionQuery<E> i) {
			iterable = i;
			parent = iterable.findIterable().iterator();
		}

		@Override
		public boolean hasNext() {
			return parent.hasNext();
		}

		@Override
		public E next() {
			return iterable.collection.create(MapWrapper.wrap(parent.next()));
		}
	}

	private final WrappedCollection<T> collection;
	private List<Bson> filters = null;
	private int limit = 0;
	private int skip = 0;
	private Bson sort = null;
	private List<Bson> projectionFields = null;
	private boolean many = false;

	public CollectionQuery(WrappedCollection<T> c) {
		collection = c;
	}

	public CollectionQuery<T> filter(@Nullable Bson f) {
		if (f == null) {
			return this;
		}

		if (filters == null) {
			filters = new ArrayList<>(1);
		}

		filters.add(f);
		return this;
	}

	public CollectionQuery<T> filters(List<Bson> f) {
		filters = f;
		return this;
	}

	public CollectionQuery<T> eq(String key, Object o) {
		return filter(Filters.eq(key, o));
	}

	public CollectionQuery<T> eqOr(String key, Object... o) {
		return filter(Filters.or(Arrays.stream(o).map(f -> Filters.eq(key, f)).collect(Collectors.toList())));
	}

	public CollectionQuery<T> id(@Nullable Object id) {
		if (id instanceof Snowflake) {
			return id(((Snowflake) id).asLong());
		} else if (id instanceof Entity) {
			return id(((Entity) id).getId());
		}

		return eq("_id", id);
	}

	public CollectionQuery<T> neq(String key, Object o) {
		return filter(Filters.not(Filters.eq(key, o)));
	}

	public CollectionQuery<T> exists(String key) {
		return filter(Filters.exists(key));
	}

	public CollectionQuery<T> regex(String key, Pattern pattern) {
		return filter(Filters.regex(key, pattern));
	}

	public CollectionQuery<T> limit(int l) {
		limit = l;
		return this;
	}

	public CollectionQuery<T> skip(int s) {
		skip = s;
		return this;
	}

	public CollectionQuery<T> sort(Bson s) {
		sort = s;
		return this;
	}

	public CollectionQuery<T> ascending(String... keys) {
		return sort(Sorts.ascending(keys));
	}

	public CollectionQuery<T> descending(String... keys) {
		return sort(Sorts.descending(keys));
	}

	public CollectionQuery<T> projectionFields(String... field) {
		if (projectionFields == null) {
			projectionFields = new ArrayList<>(1);
		}

		projectionFields.add(Projections.include(field));
		return this;
	}

	public CollectionQuery<T> many() {
		many = true;
		return this;
	}

	public Bson getFilterBson() {
		return filters == null || filters.isEmpty() ? new BsonDocument() : filters.size() == 1 ? filters.get(0) : Filters.and(filters);
	}

	public FindIterable<Document> findIterable() {
		FindIterable<Document> findIterable = collection.getCollection().find();

		if (filters != null) {
			findIterable.filter(getFilterBson());
		}

		if (limit > 0) {
			findIterable.limit(limit);
		}

		if (skip > 0) {
			findIterable.skip(skip);
		}

		if (sort != null) {
			findIterable.sort(sort);
		}

		if (projectionFields != null) {
			findIterable.projection(Projections.fields(projectionFields));
		}

		return findIterable;
	}

	@NotNull
	@Override
	public WrappedIterator<T> iterator() {
		return new WrappedIterator<>(this);
	}

	public Stream<T> toStream() {
		return Utils.toStream(this);
	}

	@Nullable
	public Document firstDocument() {
		return limit(1).findIterable().first();
	}

	@Nullable
	public T first() {
		Document doc = firstDocument();
		return doc == null ? null : collection.create(MapWrapper.wrap(doc));
	}

	public long count() {
		CountOptions options = new CountOptions();

		if (limit > 0) {
			options.limit(limit);
		}

		if (skip > 0) {
			options.skip(skip);
		}

		return collection.getCollection().countDocuments(getFilterBson(), options);
	}

	public DeleteResult delete() {
		if (filters != null) {
			return many ? collection.getCollection().deleteMany(getFilterBson()) : collection.getCollection().deleteOne(getFilterBson());
		}

		return ZERO_DELETES;
	}

	public UpdateResult update(Bson... updates) {
		if (filters != null) {
			Bson u = updates.length == 1 ? updates[0] : Updates.combine(updates);
			return many ? collection.getCollection().updateMany(getFilterBson(), u) : collection.getCollection().updateOne(getFilterBson(), u);
		}

		return ZERO_UPDATES;
	}

	public UpdateResult update(List<Bson> updates) {
		if (filters != null) {
			Bson u = updates.size() == 1 ? updates.get(0) : Updates.combine(updates);
			return many ? collection.getCollection().updateMany(getFilterBson(), u) : collection.getCollection().updateOne(getFilterBson(), u);
		}

		return ZERO_UPDATES;
	}

	public UpdateResult upsert(List<Bson> updates) {
		if (filters != null) {
			Bson u = updates.size() == 1 ? updates.get(0) : Updates.combine(updates);
			return collection.getCollection().updateOne(getFilterBson(), u, Databases.UPSERT);
		}

		return ZERO_UPDATES;
	}

	public UpdateResult replace(Document document) {
		if (filters != null) {
			return collection.getCollection().replaceOne(getFilterBson(), document);
		}

		return ZERO_UPDATES;
	}

	public void drop() {
		collection.getCollection().drop();
	}
}
