package dev.gnomebot.app.script.event;

import dev.latvian.mods.rhino.BaseFunction;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.type.TypeInfo;
import dev.latvian.mods.rhino.util.HideFromJS;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EventHandler<T extends EventJS> extends BaseFunction {
	@FunctionalInterface
	public interface EventCallback {
		void post(EventJS event);
	}

	public static final TypeInfo CALLBACK_TYPE_INFO = TypeInfo.of(EventCallback.class);

	public final boolean canCancel;
	private List<EventCallback> consumers;
	private Map<String, List<EventCallback>> extraConsumers;

	public EventHandler(boolean c) {
		canCancel = c;
	}

	@Override
	public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		if (args.length == 1) {
			if (consumers == null) {
				consumers = new ArrayList<>();
			}

			var callback = (EventCallback) cx.jsToJava(args[0], CALLBACK_TYPE_INFO);
			consumers.add(callback);
		} else if (args.length == 2) {
			var extra = String.valueOf(args[0]);
			var callback = (EventCallback) cx.jsToJava(args[1], CALLBACK_TYPE_INFO);

			if (extra.isEmpty()) {
				if (consumers == null) {
					consumers = new ArrayList<>();
				}

				consumers.add(callback);
			} else {
				if (extraConsumers == null) {
					extraConsumers = new HashMap<>();
				}

				var list = extraConsumers.get(extra);

				if (list == null) {
					list = new ArrayList<>();
					extraConsumers.put(extra, list);
				}

				list.add(callback);
			}
		}

		return null;
	}

	public boolean hasListeners() {
		return consumers != null || extraConsumers != null;
	}

	@HideFromJS
	public boolean post(String extra, T event) {
		return extraConsumers != null && !extra.isEmpty() && post0(event, extraConsumers.get(extra)) || post0(event, consumers);
	}

	private boolean post0(T event, @Nullable List<EventCallback> consumers) {
		if (consumers == null || consumers.isEmpty()) {
			return false;
		}

		for (var callback : consumers) {
			try {
				callback.post(event);

				if (canCancel && event.cancelled) {
					return true;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return false;
	}
}
