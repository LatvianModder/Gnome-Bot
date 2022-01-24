package dev.gnomebot.app;

import java.nio.file.Files;
import java.nio.file.Path;

public interface AppPaths {
	static Path make(Path p) {
		if (Files.notExists(p)) {
			try {
				Files.createDirectories(p);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return p;
	}

	Path RESOURCES = make(Path.of("resources"));
	Path ASSETS = make(RESOURCES.resolve("assets"));
	Path RUN = make(Path.of("run"));
	Path FILES = make(RUN.resolve("files"));
	Path PUBLIC_FILES = make(FILES.resolve("public"));
	Path SCRIPTS = make(RUN.resolve("scripts"));
	Path CONFIG_FILE = make(RUN.resolve("app.properties"));
}
