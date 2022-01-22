package dev.gnomebot.app.cli;

import dev.gnomebot.app.App;
import dev.gnomebot.app.discord.command.RootCommand;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.FieldInfo;
import io.github.classgraph.ScanResult;

import java.util.HashMap;
import java.util.Map;

public class CLI {
	public static final Map<String, CLICommand> COMMANDS = new HashMap<>();

	public static void find() {
		COMMANDS.clear();

		String pkg = CLI.class.getPackage().getName();
		String commandAnnotation = RootCommand.class.getName();
		//enableAllInfo()
		try (ScanResult scanResult = new ClassGraph().enableFieldInfo().enableAnnotationInfo().acceptPackages(pkg).scan()) {
			for (ClassInfo commandClassInfo : scanResult.getClassesWithFieldAnnotation(commandAnnotation)) {
				for (FieldInfo fieldInfo : commandClassInfo.getFieldInfo()) {
					if (fieldInfo.hasAnnotation(commandAnnotation)) {
						Object o = fieldInfo.loadClassAndGetField().get(null);

						if (o instanceof CLICommand c) {
							if (COMMANDS.containsKey(c.name)) {
								throw new RuntimeException("CLI Command already registered! " + c.name);
							}

							COMMANDS.put(c.name, c);
						}
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		App.info("Found " + COMMANDS.size() + " CLI commands");
	}
}
