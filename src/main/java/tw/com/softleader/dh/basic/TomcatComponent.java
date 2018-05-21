package tw.com.softleader.dh.basic;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class TomcatComponent {

	public static void shutdownTomcat(final Config config, final Path tomcatBinPath) throws IOException, InterruptedException {
		final List<String> cmdAndArgs;
		if (TomcatType.service.equals(config.getTomcatType())) {
			cmdAndArgs = Arrays.asList("cmd.exe", "/c", "net", "stop", config.getTomcatServiceName());
		} else if (TomcatType.bat.equals(config.getTomcatType())) {
			cmdAndArgs = Arrays.asList("cmd.exe", "/c", "shutdown.bat");
		} else {
			return;
		}
	    final ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
	    pb.directory(tomcatBinPath.toFile());
	    pb.start().waitFor();
	}

	public static void startupTomcat(final Config config, final Path tomcatBinPath) throws IOException, InterruptedException {
		final List<String> cmdAndArgs;
		if (TomcatType.service.equals(config.getTomcatType())) {
			cmdAndArgs = Arrays.asList("cmd.exe", "/c", "net", "start", config.getTomcatServiceName());
		} else if (TomcatType.bat.equals(config.getTomcatType())) {
			cmdAndArgs = Arrays.asList("cmd.exe", "/c", "startup.bat");
		} else {
			return;
		}
	    final ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
	    pb.directory(tomcatBinPath.toFile());
	    pb.start();
	}


}
