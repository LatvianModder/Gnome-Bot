package dev.gnomebot.app;

import java.nio.file.Files;
import java.nio.file.Path;

public interface AppPaths {
	static Path makeDir(Path p) {
		if (Files.notExists(p)) {
			try {
				Files.createDirectories(p);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return p;
	}

	Path RESOURCES = makeDir(Path.of("resources"));
	Path ASSETS = makeDir(RESOURCES.resolve("assets"));
	Path RUN = makeDir(Path.of("run"));

	Path FILES = makeDir(RUN.resolve("files"));
	Path FILES_PUBLIC = makeDir(FILES.resolve("public"));
	Path FILES_BAD_DOMAINS = FILES.resolve("bad_domains.txt");
	Path FILES_BAD_DOMAIN_OVERRIDES = FILES.resolve("bad_domain_overrides.txt");

	Path SCRIPTS = makeDir(RUN.resolve("scripts"));

	Path CONFIG_FILE = makeDir(RUN.resolve("app.properties"));
}
