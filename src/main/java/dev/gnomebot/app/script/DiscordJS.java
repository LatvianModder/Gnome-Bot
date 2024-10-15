package dev.gnomebot.app.script;

import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.script.event.ComponentEventJS;
import dev.gnomebot.app.script.event.EventHandler;
import dev.gnomebot.app.script.event.MessageEventJS;
import dev.gnomebot.app.script.event.ModalEventJS;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.SnowFlake;
import dev.gnomebot.app.util.Utils;
import dev.latvian.apps.ansi.log.Log;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.type.TypeInfo;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.discordjson.json.WebhookExecuteRequest;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DiscordJS {
	public final transient GuildCollections gc;
	public final WrappedId id;
	public final boolean readOnly;

	public final Map<String, Object> customData = new HashMap<>();
	public final EventHandler<MessageEventJS> onMessage = new EventHandler<>(true);
	public final EventHandler<MessageEventJS> onAfterMessage = new EventHandler<>(false);
	public final EventHandler<ComponentEventJS> onButton = new EventHandler<>(true);
	public final EventHandler<ComponentEventJS> onSelectMenu = new EventHandler<>(true);
	public final EventHandler<ModalEventJS> onModal = new EventHandler<>(true);
	public final Map<String, Consumer<MessageEventJS>> customMacros = new HashMap<>();

	public DiscordJS(GuildCollections g, boolean ro) {
		gc = g;
		id = gc.wrappedId;
		readOnly = ro;

		if (Files.exists(gc.paths.scripts) && hasFiles(gc.paths.scripts)) {
			var factory = new DJSContext.Factory();
			var cx = factory.enter();

			try {
				var typeWrappers = factory.getTypeWrappers();
				typeWrappers.registerDirect(WrappedId.class, o -> new WrappedId(o instanceof Number n ? n.longValue() : SnowFlake.num(o.toString())));
				typeWrappers.registerDirect(ReactionEmoji.class, o -> Utils.stringToReaction(o.toString()));
				typeWrappers.registerDirect(MessageBuilder.class, MessageBuilder::of);
				typeWrappers.registerDirect(EmbedBuilder.class, EmbedBuilder::of);

				for (var file : Files.walk(gc.paths.scripts).filter(Files::isRegularFile).toList()) {
					var rfile = gc.paths.scripts.relativize(file);

					try (Reader reader = Files.newBufferedReader(file)) {
						Scriptable scope = cx.initStandardObjects();

						ScriptableObject.putProperty(scope, "console", new NativeJavaClass(cx, scope, ConsoleWrapper.class), cx);
						ScriptableObject.putProperty(scope, "Utils", new NativeJavaClass(cx, scope, ScriptUtils.class), cx);
						ScriptableObject.putProperty(scope, "WrappedId", new NativeJavaClass(cx, scope, WrappedId.class), cx);
						ScriptableObject.putProperty(scope, "Discord", new NativeJavaObject(scope, this, TypeInfo.of(DiscordJS.class), cx), cx);
						ScriptableObject.putProperty(scope, "EmbedColor", new NativeJavaClass(cx, scope, EmbedColor.class), cx);
						ScriptableObject.putProperty(scope, "WebHook", new NativeJavaClass(cx, scope, WebHook.class), cx);
						ScriptableObject.putProperty(scope, "WebhookExecuteRequest", new NativeJavaClass(cx, scope, WebhookExecuteRequest.class), cx);

						cx.evaluateReader(scope, reader, rfile.toString(), 1, null);
						Log.info("Loaded script " + rfile);
					} catch (Exception ex) {
						Log.error("Failed to load script " + rfile);
						ex.printStackTrace();
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}
	}

	public void checkReadOnly() {
		if (readOnly) {
			throw new IllegalStateException("Script is in read-only mode!");
		}
	}

	private static boolean hasFiles(Path path) {
		try {
			return Files.newDirectoryStream(path).iterator().hasNext();
		} catch (Exception ex) {
			return false;
		}
	}

	public void addMacro(String name, Consumer<MessageEventJS> callback) {
		customMacros.put(name, callback);
	}
}
