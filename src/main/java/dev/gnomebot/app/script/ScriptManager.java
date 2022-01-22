package dev.gnomebot.app.script;

import dev.gnomebot.app.App;
import dev.gnomebot.app.util.Utils;
import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.NativeJavaClass;
import dev.latvian.mods.rhino.NativeJavaObject;
import dev.latvian.mods.rhino.Scriptable;
import dev.latvian.mods.rhino.ScriptableObject;
import dev.latvian.mods.rhino.util.wrap.TypeWrappers;
import discord4j.common.util.Snowflake;
import discord4j.core.object.reaction.ReactionEmoji;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LatvianModder
 */
public class ScriptManager {
	public final App app;
	public final Map<WrappedId, GuildScripts> scriptsMap;

	public ScriptManager(App a) {
		app = a;
		scriptsMap = new HashMap<>();
	}

	private static boolean isValidSnowflake(Path dir) {
		try {
			return Snowflake.of(dir.getFileName().toString()).asLong() != 0L;
		} catch (Exception e) {
			return false;
		}
	}

	public void loadScripts(App app, Path path) throws Exception {
		scriptsMap.clear();
		App.info("Loading scripts...");
		Context context = Context.enterWithNewFactory();
		context.setClassShutter((fullClassName, type) -> true);

		TypeWrappers typeWrappers = context.getTypeWrappers();
		typeWrappers.register(Snowflake.class, o -> o instanceof Number ? Snowflake.of(((Number) o).longValue()) : Snowflake.of(o.toString()));
		typeWrappers.register(WrappedId.class, o -> new WrappedId(o instanceof Number ? Snowflake.of(((Number) o).longValue()) : Snowflake.of(o.toString())));
		typeWrappers.register(ReactionEmoji.class, o -> Utils.stringToReaction(o.toString()));

		for (Path dir : Files.list(path).filter(Files::isDirectory).filter(ScriptManager::isValidSnowflake).toList()) {
			GuildScripts guildScripts = new GuildScripts(app, new WrappedId(Snowflake.of(dir.getFileName().toString())));

			for (Path file : Files.walk(dir).filter(Files::isRegularFile).toList()) {
				try (Reader reader = Files.newBufferedReader(file)) {
					Scriptable scope = context.initStandardObjects();

					ScriptableObject.putProperty(scope, "console", new NativeJavaClass(scope, ConsoleWrapper.class));
					ScriptableObject.putProperty(scope, "Utils", new NativeJavaClass(scope, ScriptUtils.class));
					ScriptableObject.putProperty(scope, "Snowflake", new NativeJavaClass(scope, Snowflake.class));
					ScriptableObject.putProperty(scope, "WrappedId", new NativeJavaClass(scope, WrappedId.class));
					ScriptableObject.putProperty(scope, "Discord", new NativeJavaObject(scope, guildScripts, GuildScripts.class));

					context.evaluateReader(scope, reader, path.relativize(file).toString(), 1, null);
					App.info("Loaded script " + path.relativize(file));
					scriptsMap.put(guildScripts.id, guildScripts);
				} catch (Exception ex) {
					App.error("Failed to load script " + path.relativize(file));
					ex.printStackTrace();
				}
			}
		}

		Context.exit();
	}
}