package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.data.GuildCollections;
import dev.gnomebot.app.discord.EmbedColor;
import dev.gnomebot.app.discord.WebHook;
import dev.gnomebot.app.script.event.ComponentEventJS;
import dev.gnomebot.app.script.event.EventHandler;
import dev.gnomebot.app.script.event.MessageEventJS;
import dev.gnomebot.app.script.event.ModalEventJS;
import dev.gnomebot.app.util.EmbedBuilder;
import dev.gnomebot.app.util.MessageBuilder;
import dev.gnomebot.app.util.Utils;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import discord4j.common.util.Snowflake;
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
			var context = Context.enterWithNewFactory();
			context.setClassShutter((fullClassName, type) -> true);

			try {
				var typeWrappers = context.getTypeWrappers();
				typeWrappers.register(Snowflake.class, o -> o instanceof Number ? Snowflake.of(((Number) o).longValue()) : Utils.snowflake(o.toString()));
				typeWrappers.register(WrappedId.class, o -> new WrappedId(o instanceof Number ? Snowflake.of(((Number) o).longValue()) : Utils.snowflake(o.toString())));
				typeWrappers.register(ReactionEmoji.class, o -> Utils.stringToReaction(o.toString()));
				typeWrappers.register(MessageBuilder.class, MessageBuilder::of);
				typeWrappers.register(EmbedBuilder.class, EmbedBuilder::of);

				for (var file : Files.walk(gc.paths.scripts).filter(Files::isRegularFile).toList()) {
					var rfile = gc.paths.scripts.relativize(file);

					try (Reader reader = Files.newBufferedReader(file)) {
						Scriptable scope = context.initStandardObjects();

						ScriptableObject.putProperty(scope, "console", new NativeJavaClass(scope, ConsoleWrapper.class));
						ScriptableObject.putProperty(scope, "Utils", new NativeJavaClass(scope, ScriptUtils.class));
						ScriptableObject.putProperty(scope, "Snowflake", new NativeJavaClass(scope, Snowflake.class));
						ScriptableObject.putProperty(scope, "WrappedId", new NativeJavaClass(scope, WrappedId.class));
						ScriptableObject.putProperty(scope, "Discord", new NativeJavaObject(scope, this, DiscordJS.class));
						ScriptableObject.putProperty(scope, "EmbedColor", new NativeJavaClass(scope, EmbedColor.class));
						ScriptableObject.putProperty(scope, "WebHook", new NativeJavaClass(scope, WebHook.class));
						ScriptableObject.putProperty(scope, "WebhookExecuteRequest", new NativeJavaClass(scope, WebhookExecuteRequest.class));

						context.evaluateReader(scope, reader, rfile.toString(), 1, null);
						App.info("Loaded script " + rfile);
					} catch (Exception ex) {
						App.error("Failed to load script " + rfile);
						ex.printStackTrace();
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			Context.exit();
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
