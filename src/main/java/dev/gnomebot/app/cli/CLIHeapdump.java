package dev.gnomebot.app.cli;

public class CLIHeapdump {
	public static final CLICommand COMMAND = CLICommand.make("heapdump")
			.description("Heapdump")
			.ownerOnly()
			.run(CLIHeapdump::run);

	private static void run(CLIEvent event) {
		event.respond("WIP!");

		/*
		System.gc();

		try {
			MBeanServer server = ManagementFactory.getPlatformMBeanServer();
			HotSpotDiagnosticMXBean mxBean = ManagementFactory.newPlatformMXBeanProxy(server, "com.sun.management:type=HotSpotDiagnostic", HotSpotDiagnosticMXBean.class);
			mxBean.getDiagnosticOptions().forEach(App::info);
			String filename = "run/files/heapdump-" + Instant.now().toString().replace(':', '-') + ".hprof";
			mxBean.dumpHeap(filename, false);
			warn("Heap dump saved: " + ((Files.size(AppPaths.DATA_GUILDS.resolve(filename)) / 1024L) / 1024D) + " MB");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		 */
	}
}
