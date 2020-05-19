package patentdata.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

public class Log {

	private Common common = new Common();

	public File folderLog = null;

	public Log(String path) throws Exception {
		try {
			folderLog = new File(path, "logs");
			try {
				if (!folderLog.exists())
					folderLog.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			throw new Exception("initial failed. " + e.getMessage());
		}
	}

	public void printErr(Exception e) throws Exception {
		printErr(e, "", true);
	}

	public void writeErr(Exception e) throws Exception {
		printErr(e, "", false);
	}

	public void printErr(String message) throws Exception {
		printErr(null, message, true);
	}

	public void printErr(Exception e, String message) throws Exception {
		printErr(e, message, true);
	}

	public void printErr(Exception e, String message, boolean isPrintStackTrace) throws Exception {
		StringBuilder sb = new StringBuilder();
		if (null != e) {
			if (isPrintStackTrace)
				e.printStackTrace();
			sb.append(e.getMessage());
		}
		if (!StringUtils.isEmpty(message))
			sb.append("\n").append(message);
		print(sb.toString(), "err");
	}

	public void print(String message) throws Exception {
		print(message, "");
	}

	public void print(String message, String prefix) throws Exception {
		print(message, prefix, folderLog);
	}

	public void print(String message, String prefix, File folder) throws Exception {
		message = getLogMessage(message);
		System.out.print((prefix.matches("(?i)err.*") ? "ERROR : " : "") + message);
		write(message, prefix, folder);
	}

	public void write(String message, String prefix, File folder) throws Exception {
		common.WriteFile(
				new File(folder, prefix + (StringUtils.isEmpty(prefix) ? "" : "_") + getNow("yyyyMMddHH") + ".log")
						.toString(),
				message, true);
	}

	public String getLogMessage(String message) throws Exception {
		return new StringBuilder(getNow("yyyyMMdd HH:mm:ss.SSS")).append("\t\t").append(message).append("\n")
				.toString();
	}

	public String getNow(String pattern) {
		return new SimpleDateFormat(pattern).format(Calendar.getInstance().getTime());
	}

}
