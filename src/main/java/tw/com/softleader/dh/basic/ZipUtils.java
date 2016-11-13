package tw.com.softleader.dh.basic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;

public class ZipUtils {

	public static void packToolFiles(ZipArchiveOutputStream zaos, File dir) throws FileNotFoundException, IOException {
		packToolFiles(zaos, dir, "");
	}

	public static void packToolFiles(ZipArchiveOutputStream zaos, File dir, String pathName) throws FileNotFoundException, IOException {
		// 返回此絕對路徑下的檔
		final File[] files = dir.listFiles();
		if (files == null || files.length < 1) {
			return;
		}
		for (int i = 0; i < files.length; i++) {
			// 判斷此檔是否是一個資料夾
			if (files[i].isDirectory()) {
				packToolFiles(zaos, files[i], pathName + files[i].getName() + File.separator);
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

}
