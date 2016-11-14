package tw.com.softleader.dh.basic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Enumeration;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.IOUtils;

public class ZipUtils {

	public static void compress(final ZipArchiveOutputStream zaos, final File dir) throws FileNotFoundException, IOException {
		compress(zaos, dir, "");
	}

	public static void compress(final ZipArchiveOutputStream zaos, final File dir, final String pathName) throws FileNotFoundException, IOException {
		// 返回此絕對路徑下的檔
		final File[] files = dir.listFiles();
		if (files == null || files.length < 1) {
			return;
		}
		for (int i = 0; i < files.length; i++) {
			// 判斷此檔是否是一個資料夾
			if (files[i].isDirectory()) {
				compress(zaos, files[i], pathName + files[i].getName() + File.separator);
			} else {
				try (FileInputStream fio = new FileInputStream(files[i])) {
					zaos.putArchiveEntry(new ZipArchiveEntry(pathName + files[i].getName()));
					IOUtils.copy(fio, zaos);
					zaos.closeArchiveEntry();
				} catch (final Exception e) {
					throw e;
				}
			}
		}
	}

	public static void decompress(final ZipFile zipFile, final Path saveFilePath) throws IOException {
		for (final Enumeration<ZipArchiveEntry> files = zipFile.getEntries(); files.hasMoreElements();) {
			final ZipArchiveEntry zipArchiveEntry = files.nextElement();
			final File outFile = saveFilePath.resolve(zipArchiveEntry.getName()).toFile();
			outFile.toPath().getParent().toFile().mkdirs();

			try (
				FileOutputStream fos = new FileOutputStream(outFile);
			) {
				IOUtils.copy(zipFile.getInputStream(zipArchiveEntry), fos);
			} catch (final IOException e) {
				throw new IOException(e);
			}
		}
	}

}
