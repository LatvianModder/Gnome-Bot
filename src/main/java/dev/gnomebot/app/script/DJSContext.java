package dev.gnomebot.app.script;

import dev.latvian.mods.rhino.Context;
import dev.latvian.mods.rhino.ContextFactory;
import dev.latvian.mods.rhino.util.ClassVisibilityContext;

public class DJSContext extends Context {
	public static class Factory extends ContextFactory {
		@Override
		protected Context createContext() {
			return new DJSContext(this);
		}
	}

	public DJSContext(Factory factory) {
		super(factory);
	}

	@Override
	public boolean visibleToScripts(String fullClassName, ClassVisibilityContext type) {
		return true;
	}
}
