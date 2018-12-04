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

import com.sun.javafx.scene.control.LabeledText;

import javafx.scene.input.MouseEvent;
import tw.com.softleader.dh.basic.Config;
import tw.com.softleader.dh.basic.VerifyException;

@SuppressWarnings("restriction")
public class RemarkHandler {

	// 全域設定檔(+使用者鍵入)
	private Config config;

	// 自動產生
	private File backupDir;

	public RemarkHandler(final Config config) {
		this.config = config;
		setting();
	}

	private void setting() {
		this.backupDir = Optional.ofNullable(this.config.getBackupPath()).map(File::new).orElse(null);
	}

	public void loadRemark(final MouseEvent event, final Consumer<Throwable> errorHandle, final Consumer<String> afterLoad, final Runnable callback) throws VerifyException {
		setting();
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

		if (!isFileCanUse(backupDir)) {
			msgs.add("請選擇備份路徑");
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
		final Path remarkFile = backupDir.toPath().resolve(fileName);
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
			BufferedReader bfr = new BufferedReader(new InputStreamReader(fis, "UTF8"));
		) {
			String strtmp;
			while ((strtmp = bfr.readLine()) != null) {
				sj.add(strtmp);
			}
		}
		return sj.toString();
	}

	private void writeRemarkFile(final String fileName, final String text) throws IOException {
		final File remarkFile = backupDir.toPath().resolve(fileName).toFile();
		try (
			FileOutputStream fos = new FileOutputStream(remarkFile);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF8");
		) {
			osw.write(text);
		}
	}

	private void tryDeleteFile(final String fileName) {
		final Path remarkFile = backupDir.toPath().resolve(fileName);
		if (remarkFile.toFile().exists()) {
			remarkFile.toFile().delete();
		}
	}

}
