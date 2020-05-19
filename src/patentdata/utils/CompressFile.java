package patentdata.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.FileUtils;

public class CompressFile {

	public void unzip(String compressedFile, String decompressedFile) throws IOException {
		if (compressedFile.endsWith(".gz")) {
			unGunzipFile(compressedFile, decompressedFile);
		} else if (compressedFile.endsWith(".zip")) {
			unzipFile(compressedFile, decompressedFile);
		} else {
			FileUtils.copyFile(new File(compressedFile), new File(decompressedFile));
		}
	}

	private void unzipFile(String compressedFile, String decompressedFile) {
		try {
			byte[] buffer = new byte[1024];
			ZipInputStream zis = new ZipInputStream(new FileInputStream(compressedFile));
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				FileOutputStream fos = new FileOutputStream(decompressedFile);
				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}
				fos.close();
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
			zis.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	private void unGunzipFile(String compressedFile, String decompressedFile) {

		byte[] buffer = new byte[1024];

		try {

			FileInputStream fileIn = new FileInputStream(compressedFile);

			GZIPInputStream gZIPInputStream = new GZIPInputStream(fileIn);

			FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile);

			int bytes_read;

			while ((bytes_read = gZIPInputStream.read(buffer)) > 0) {

				fileOutputStream.write(buffer, 0, bytes_read);
			}

			gZIPInputStream.close();
			fileOutputStream.close();

			// System.out.println("The file was decompressed successfully!");

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

}
