package tw.com.softleader.dh.strategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import lombok.Getter;
import lombok.Setter;
import tw.com.softleader.dh.basic.DeployVerifyException;
import tw.com.softleader.dh.basic.ZipUtils;

@Setter
@Getter
public class DeployHandler {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

	// 使用者輸入
	private boolean backup = true;
	private File deployFile;
	private File deployTomcatPath = Optional.ofNullable(System.getenv("TOMCAT_HOME")).map(File::new).orElse(null);

	// 自動產生
	private Path tomcatBinPath;
	private Path tomcatWebAppPath;
	private File backupFile;
	private File deploiedFile;

	ExecutorService executor = Executors.newSingleThreadExecutor();
	public void deploy(Consumer<String> logConsumer, Runnable callback) throws Exception {
		verify();

		executor.submit(() -> {
			try {
				logConsumer.accept("正在嘗試關閉Tomcat...");
				shutdownTomcat();

				logConsumer.accept("正在進行備份...");
				backupWebApps();

				logConsumer.accept("佈署中...");
				copyWarToWebApp();
				doBeforeStart();

				logConsumer.accept("佈署完畢，您已經可以結束此佈署程式");
				startupTomcat();
			} catch (final Exception e) {
				logConsumer.accept(e.getMessage());
			} finally {
				callback.run();
			}
		});
	}

	private void verify() throws DeployVerifyException {
		final List<String> msgs = new ArrayList<>();

		if (!isFileCanUse(deployFile)) {
			msgs.add("請選擇佈署檔");
		}

		if (!isFileCanUse(deployTomcatPath)) {
			msgs.add("請選擇佈署路徑");
		} else {
			final Path tomcatPath = deployTomcatPath.toPath();

			this.tomcatBinPath = tomcatPath.resolve("bin");
			if (!isPathCanUse(this.tomcatBinPath)) {
				msgs.add("找不到 " + this.tomcatBinPath.toString() + " 請確認您選擇的是正確的Tomcat");
			}

			this.tomcatWebAppPath = tomcatPath.resolve("webapps");
			if (!isPathCanUse(this.tomcatWebAppPath)) {
				msgs.add("找不到 " + this.tomcatWebAppPath.toString() + " 請確認您選擇的是正確的Tomcat");
			}
		}

		if (!msgs.isEmpty()) {
			throw new DeployVerifyException(msgs);
		}
	}

	private void shutdownTomcat() throws IOException, InterruptedException {
		final List<String> cmdAndArgs = Arrays.asList("cmd", "/c", "shutdown.bat");
	    final ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
	    pb.directory(tomcatBinPath.toFile());
	    pb.start().destroyForcibly();
	}

	private void backupWebApps() throws Exception {
		backupFile = new File(deployTomcatPath.getPath() + "\\webapp." + LocalDateTime.now().format(formatter) + ".zip");
		try (final ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(backupFile)) {
			ZipUtils.packToolFiles(zaos, tomcatWebAppPath.toFile());
		} catch (final Exception e) {
			throw e;
		}
	}

	private void copyWarToWebApp() throws Exception {
		deploiedFile = tomcatWebAppPath.resolve(deployFile.getName()).toFile();
		try (
			final FileInputStream input = new FileInputStream(deployFile);
			final FileOutputStream output = new FileOutputStream(deploiedFile);
		) {
			IOUtils.copy(input, output);
		} catch (final Exception e) {
			throw e;
		}
	}

	private void doBeforeStart() {
		if (!backup) {
			backupFile.delete();
		}
	}

	private void startupTomcat() throws IOException, InterruptedException {
		final List<String> cmdAndArgs = Arrays.asList("cmd", "/k", "startup.bat");
	    final ProcessBuilder pb = new ProcessBuilder(cmdAndArgs);
	    pb.directory(tomcatBinPath.toFile());
	    pb.start();
	}

	private boolean isPathCanUse(Path path) {
		return path == null || Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
	}

	private boolean isFileCanUse(File file) {
		return file != null && file.exists();
	}

}
