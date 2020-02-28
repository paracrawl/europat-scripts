package patentdata.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class Log {

	private Common common = new Common();

	private File folderLog = null;

	public Log() throws Exception {
		try {
			folderLog = new File(new Config(common.getConfigPath())._config.WorkingDir, "logs");
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

	public void printErr(String message) throws Exception {
		print(message, "err");
	}

	public void print(String message) throws Exception {
		print(message, "");
	}

	public void print(String message, String prefix) throws Exception {
		print(message, prefix, folderLog);
	}

	public void print(String message, String prefix, File folder) throws Exception {
		System.out.println(message);
		write(message, prefix + "_", folder);
	}

	public void write(String message, String prefix, File folder) throws Exception {
		common.WriteFile(new File(folder, prefix + getNow("yyyyMMdd") + ".log").toString(),
				getLogMessage(message), true);
	}

	public String getLogMessage(String message) throws Exception {
		return new StringBuilder(getNow("yyyyMMdd HH:mm:ss.SSS")).append("\t\t").append(message).append("\n")
				.toString();
	}

	public String getNow(String pattern) {
		return new SimpleDateFormat(pattern).format(Calendar.getInstance().getTime());
	}

}
