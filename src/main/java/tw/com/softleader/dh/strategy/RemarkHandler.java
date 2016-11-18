package tw.com.softleader.dh.strategy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import com.sun.javafx.scene.control.skin.LabeledText;

import javafx.scene.input.MouseEvent;
import tw.com.softleader.dh.basic.Config;
import tw.com.softleader.dh.basic.VerifyException;

@SuppressWarnings("restriction")
public class RemarkHandler {

	// 使用者輸入
	private File tomcatDir;

	// 全域設定檔
	private Config config;

	public RemarkHandler(final Config config) {
		this.config = config;
		if (this.config != null) {
			if (this.config.getTomcatPath() != null && !this.config.getTomcatPath().isEmpty()) {
				tomcatDir = new File(this.config.getTomcatPath());
			} else {
				tomcatDir = Optional.ofNullable(System.getenv("TOMCAT_HOME")).map(File::new).orElse(null);
			}
		}
	}

	public void loadRemark(final MouseEvent event, final Consumer<Throwable> errorHandle, final Consumer<String> afterLoad, final Runnable callback) throws VerifyException {
		verify();
		CompletableFuture.runAsync(() -> {
			try {
				if (event.getTarget() instanceof LabeledText) {
					final LabeledText labeledText = (LabeledText) event.getTarget();
					final String fileName = getTxtFileName(labeledText.getText());
					afterLoad.accept(readRemarkFile(fileName));
				}
			} catch (final Exception e) {
				errorHandle.accept(e);
			} finally {
				callback.run();
			}
		});
	}

	public void saveRemark(final String text, final String file, final Consumer<Throwable> errorHandle, final Runnable callback) throws VerifyException {
		verify();
		CompletableFuture.runAsync(() -> {
			try {
				final String fileName = getTxtFileName(file);
				tryDeleteFile(fileName);
				writeRemarkFile(fileName, text);
			} catch (final IOException e) {
				errorHandle.accept(e);
			} finally {
				callback.run();
			}
		});
	}

	private String getTxtFileName(final String file) {
		return file.substring(0, file.lastIndexOf('.')) + ".txt";
	}

	private void verify() throws VerifyException {
		final List<String> msgs = new ArrayList<>();

		if (!isFileCanUse(tomcatDir)) {
			msgs.add("請選擇Tomcat路徑");
		}

		if (!msgs.isEmpty()) {
			throw new VerifyException(msgs);
		} else {
			config.verify();
		}
	}

	private boolean isFileCanUse(final File file) {
		return file != null && file.exists();
	}

	private String readRemarkFile(final String fileName) throws IOException {
		final Path remarkFile = tomcatDir.toPath().resolve(fileName);
		if (remarkFile.toFile().exists()) {
			return read(remarkFile.toFile());
		} else {
			return "";
		}
	}

	private String read(final File file) throws IOException {
		final StringJoiner sj = new StringJoiner("\n");
		try (
			FileInputStream fis = new FileInputStream(file);
			BufferedReader bfr = new BufferedReader(new InputStreamReader(fis));
		) {
			String strtmp;
			while ((strtmp = bfr.readLine()) != null) {
				sj.add(strtmp);
			}
		} catch (final IOException e) {
			throw e;
		}
		return sj.toString();
	}

	private void writeRemarkFile(final String fileName, final String text) throws IOException {
		final File remarkFile = tomcatDir.toPath().resolve(fileName).toFile();
		try (
			FileOutputStream fos = new FileOutputStream(remarkFile);
			OutputStreamWriter osw = new OutputStreamWriter(fos);
		) {
			osw.write(text);
		} catch (final IOException e) {
			throw e;
		}
	}

	private void tryDeleteFile(final String fileName) {
		final Path remarkFile = tomcatDir.toPath().resolve(fileName);
		if (remarkFile.toFile().exists()) {
			remarkFile.toFile().delete();
		}
	}

	public File getTomcatDir() {
		return tomcatDir;
	}

	public void setTomcatDir(final File tomcatDir) {
		this.tomcatDir = tomcatDir;
	}

}
