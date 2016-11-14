package tw.com.softleader.dh.basic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class TomcatComponent {

	public static void shutdownTomcat(final Config config, final Path tomcatBinPath) throws IOException, InterruptedException {
		final List<String> cmdAndArgs;
		if (TomcatType.service.equals(config.getTomcatType())) {
			cmdAndArgs = Arrays.asList("cmd", "/c", "sc stop " + config.getTomcatServiceName());
		} else {
			cmdAndArgs = Arrays.asList("cmd", "/c", "shutdown.bat");
		}
	    final ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
	    pb.directory(tomcatBinPath.toFile());
	    pb.start().waitFor();
	}

	public static void startupTomcat(final Config config, final Path tomcatBinPath) throws IOException, InterruptedException {
		final List<String> cmdAndArgs;
		if (TomcatType.service.equals(config.getTomcatType())) {
			cmdAndArgs = Arrays.asList("cmd", "/c", "sc start " + config.getTomcatServiceName());
		} else {
			cmdAndArgs = Arrays.asList("cmd", "/k", "startup.bat");
		}
	    final ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
	    pb.directory(tomcatBinPath.toFile());
	    pb.start();
	}


}
