package tw.com.softleader.dh.strategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

import tw.com.softleader.dh.basic.Config;
import tw.com.softleader.dh.basic.Constants;
import tw.com.softleader.dh.basic.TomcatComponent;
import tw.com.softleader.dh.basic.VerifyException;
import tw.com.softleader.dh.basic.ZipUtils;

public class DeployHandler {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.TIME_PATTERN);

	// 全域設定檔(+使用者鍵入)
	private Config config;

	// 自動產生
	private File deployFile;
	private File backupDir;
	private File tomcatDir;
	private Path tomcatBinPath;
	private Path tomcatWebAppPath;
	private File backupFile;
	private File deploiedFile;

	public DeployHandler(final Config config) {
		this.config = config;
		setting();
	}

	private void setting() {
		this.deployFile = Optional.ofNullable(this.config.getWarPath()).map(File::new).orElse(null);
		this.backupDir = Optional.ofNullable(this.config.getBackupPath()).map(File::new).orElse(null);
		this.tomcatDir = Optional.ofNullable(this.config.getTomcatPath()).map(File::new).orElse(null);
		this.tomcatBinPath = Optional.ofNullable(tomcatDir).map(File::toPath).map(p -> p.resolve("bin")).orElse(null);
		this.tomcatWebAppPath = Optional.ofNullable(tomcatDir).map(File::toPath).map(p -> p.resolve("webapps")).orElse(null);
	}

	public void book(final LocalDateTime bookTime, final Consumer<String> logHandle, final Consumer<Throwable> errorHandle, final Runnable callback) throws Exception {
		logHandle.accept("資料校驗中...");
		setting();
		verify();
		verifyBook(bookTime);

		final long delay = Duration.between(LocalDateTime.now(), bookTime).getSeconds();

		// 預約在N秒後執行
		final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		executor.schedule(() -> doDepoly(logHandle, errorHandle, callback), delay, TimeUnit.SECONDS);

		CompletableFuture.runAsync(() -> {
			while(LocalDateTime.now().isBefore(bookTime)) {
				try {
					logHandle.accept("即將在 " + countDown(bookTime) + " 後開始佈署, !!請勿關閉此程式!!");
					Thread.sleep(1000);
				} catch (final InterruptedException ignored) {
				}
			}
			logHandle.accept("即將開始佈署...");
		});
	}

	private String countDown(final LocalDateTime target) {
		final long d = Duration.between(LocalDateTime.now(), target).getSeconds();
		return String.format("%d:%02d:%02d", d / 3600, (d % 3600) / 60, (d % 60));
	}

	public void deploy(final Consumer<String> logHandle, final Consumer<Throwable> errorHandle, final Runnable callback) throws Exception {
		logHandle.accept("資料校驗中...");
		setting();
		verify();

		CompletableFuture.runAsync(() -> doDepoly(logHandle, errorHandle, callback));

	}

	private void doDepoly(final Consumer<String> logHandle, final Consumer<Throwable> errorHandle, final Runnable callback) {
		try {
			logHandle.accept("正在嘗試關閉Tomcat...");
			TomcatComponent.shutdownTomcat(config, tomcatBinPath);

			logHandle.accept("佈署中...");
			copyWarToWebApp();
			doBeforeStart();

			logHandle.accept("正在備份...");
			backupWar();

			TomcatComponent.startupTomcat(config, tomcatBinPath);
			logHandle.accept("佈署完畢，您已經可以結束此佈署程式");
		} catch (final Exception e) {
			logHandle.accept("異常發生，中斷操作");
			errorHandle.accept(e);
		} finally {
			callback.run();
		}
	}

	private void verify() throws VerifyException {
		final List<String> msgs = new ArrayList<>();

		if (!isFileCanUse(deployFile)) {
			msgs.add("請選擇佈署檔");
		}

		if (!isFileCanUse(tomcatDir)) {
			msgs.add("請選擇佈署路徑");
		} else {
			if (!isPathCanUse(this.tomcatBinPath)) {
				msgs.add("找不到 " + this.tomcatBinPath.toString() + " 請確認您選擇的是正確的Tomcat");
			}

			if (!isPathCanUse(this.tomcatWebAppPath)) {
				msgs.add("找不到 " + this.tomcatWebAppPath.toString() + " 請確認您選擇的是正確的Tomcat");
			}

			if (this.config.isKeepBackUpFile() && !isFileCanUse(this.backupDir)) {
				msgs.add("找不到 " + this.tomcatWebAppPath.toString() + " 請確認您選擇的是正確的備份路徑");
			}
		}

		if (!msgs.isEmpty()) {
			throw new VerifyException(msgs);
		} else {
			config.verify();
		}
	}

	private void verifyBook(final LocalDateTime reserveTime) throws VerifyException {
		final List<String> msgs = new ArrayList<>();

		if (reserveTime == null) {
			msgs.add("請指定佈署時間");
		} else if (!reserveTime.isAfter(LocalDateTime.now())) {
			msgs.add("請指定未來時間");
		}

		if (!msgs.isEmpty()) {
			throw new VerifyException(msgs);
		}
	}

	// 備份整個webapps
	@SuppressWarnings("unused") // TODO for option
	private void backupWebApps() throws Exception {
		backupFile = new File(tomcatDir.getPath() + "/" + Constants.BACKUP_PREFIX + ".webapps." + LocalDateTime.now().format(formatter) + ".zip");
		try (final ZipArchiveOutputStream zaos = new ZipArchiveOutputStream(backupFile)) {
			ZipUtils.compress(zaos, tomcatWebAppPath.toFile());
		}
	}

	private void backupWar() throws Exception {
		if (backupDir != null && backupDir.exists()) {
			backupFile = new File(backupDir.getPath() + "/" + Constants.BACKUP_PREFIX + "." + LocalDateTime.now().format(formatter) + "." + deployFile.getName());
			try (
				FileOutputStream fos = new FileOutputStream(backupFile);
				FileInputStream fis = new FileInputStream(deployFile);
			) {
				IOUtils.copy(fis, fos);
			}
		}
	}

	private void copyWarToWebApp() throws Exception {
		deploiedFile = tomcatWebAppPath.resolve(deployFile.getName()).toFile();
		try (
			final FileInputStream input = new FileInputStream(deployFile);
			final FileOutputStream output = new FileOutputStream(deploiedFile);
		) {
			IOUtils.copy(input, output);
		}
	}

	private void doBeforeStart() {
		if (!config.isKeepBackUpFile()) {
			backupFile.delete();
		}
	}

	private boolean isPathCanUse(final Path path) {
		return path == null || Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
	}

	private boolean isFileCanUse(final File file) {
		return file != null && file.exists();
	}

}
