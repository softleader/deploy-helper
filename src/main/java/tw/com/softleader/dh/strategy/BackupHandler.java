package tw.com.softleader.dh.strategy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;

import tw.com.softleader.dh.basic.Config;
import tw.com.softleader.dh.basic.Constants;
import tw.com.softleader.dh.basic.TomcatComponent;
import tw.com.softleader.dh.basic.VerifyException;
import tw.com.softleader.dh.basic.ZipUtils;

public class BackupHandler {

	// 使用者輸入
	private String chooseFileName;

	// 全域設定檔(+使用者鍵入)
	private Config config;

	// 自動產生
	private File backupDir;
	private File tomcatDir;
	private List<File> backupFiles;
	private Path tomcatBinPath;
	private Path tomcatWebAppPath;

	public BackupHandler(final Config config) {
		this.config = config;
		setting();
	}

	private void setting() {
		this.backupDir = new File(this.config.getBackupPath());
		this.tomcatDir = new File(this.config.getTomcatPath());
		this.tomcatBinPath = tomcatDir.toPath().resolve("bin");
		this.tomcatWebAppPath = tomcatDir.toPath().resolve("webapps");
	}

	public void reload() {
		setting();
		if (this.backupDir != null && this.backupDir.exists()) {
			final File[] files = this.backupDir.listFiles((dir, name) -> name.startsWith(Constants.BACKUP_PREFIX) && name.endsWith(Constants.BACKUP_EXTENSION));
			if (files == null) {
				this.backupFiles = new ArrayList<>();
			} else {
				this.backupFiles = Arrays.asList(files);
			}
		}
	}

	public void restore(final Consumer<String> logHandle, final Consumer<Throwable> errorHandle, final Runnable callback) throws VerifyException {
		logHandle.accept("資料校驗中...");
		setting();
		verify();

		CompletableFuture.runAsync(() -> {
			try {
				logHandle.accept("正在嘗試關閉Tomcat...");
				TomcatComponent.shutdownTomcat(config, tomcatBinPath);

				logHandle.accept("正在進行還原...");
//				recreatFolder();
//				restoreZip();
				restoreWar();

				TomcatComponent.startupTomcat(config, tomcatBinPath);
				logHandle.accept("還原完畢，您已經可以結束此佈署程式");
			} catch (final Exception e) {
				logHandle.accept("異常發生，中斷操作");
				errorHandle.accept(e);
			} finally {
				callback.run();
			}
		});
	}

	@SuppressWarnings("unused") // TODO for option of restore full webapps
	private void recreatFolder() throws IOException {
		FileUtils.deleteDirectory(tomcatWebAppPath.toFile());
		tomcatWebAppPath.toFile().mkdir();
	}

	@SuppressWarnings("unused") // TODO for option of restore full webapps
	private void restoreZip() throws IOException {
		final File zipFile = backupFiles.stream().filter(file -> file.getName().equals(chooseFileName)).findFirst().orElse(null);
		ZipUtils.decompress(new ZipFile(zipFile), tomcatWebAppPath);
	}

	private void restoreWar() throws IOException {
		final File backupFile = backupFiles.stream().filter(file -> file.getName().equals(chooseFileName)).findFirst().orElse(null);
		final String targetFileName = backupFile.getName().substring((Constants.BACKUP_PREFIX + "." + Constants.TIME_PATTERN + ".").length());
		final File targetFile = tomcatWebAppPath.resolve(targetFileName).toFile();
		if (targetFile.exists()) {
			targetFile.delete();
		}

		try (
			FileOutputStream fos = new FileOutputStream(targetFile);
			FileInputStream fis = new FileInputStream(backupFile);
		) {
			IOUtils.copy(fis, fos);
		}
	}

	private void verify() throws VerifyException {
		final List<String> msgs = new ArrayList<>();

		if (!isFileCanUse(tomcatDir)) {
			msgs.add("請選擇Tomcat路徑");
		} else {
			if (!isPathCanUse(this.tomcatBinPath)) {
				msgs.add("找不到 " + this.tomcatBinPath.toString() + " 請確認您選擇的是正確的Tomcat");
			}

			if (!isPathCanUse(this.tomcatWebAppPath)) {
				msgs.add("找不到 " + this.tomcatWebAppPath.toString() + " 請確認您選擇的是正確的Tomcat");
			}

			if (!isFileCanUse(this.backupDir)) {
				msgs.add("找不到 " + this.tomcatWebAppPath.toString() + " 請確認您選擇的是正確的備份路徑");
			}
		}

		if (chooseFileName == null || chooseFileName.isEmpty()) {
			msgs.add("請選擇還原檔");
		}

		if (!msgs.isEmpty()) {
			throw new VerifyException(msgs);
		} else {
			config.verify();
		}
	}

	private boolean isPathCanUse(final Path path) {
		return path == null || Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS);
	}

	private boolean isFileCanUse(final File file) {
		return file != null && file.exists();
	}

	public String getChooseFileName() {
		return chooseFileName;
	}

	public void setChooseFileName(final String chooseFileName) {
		this.chooseFileName = chooseFileName;
	}

	public List<File> getBackupFiles() {
		return backupFiles;
	}

}
