package patentdata.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author MickeyVI
 */
public class Common {
	Object _oLockerFile = new Object();
	private int verbose = 1;

	public static boolean isWindows() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("win") >= 0);
	}

	public static boolean isMac() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("mac") >= 0);
	}

	public static boolean isUnix() {
		String os = System.getProperty("os.name").toLowerCase();
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
	}

	public String getJarPath() {
		String applicationDir = getClass().getProtectionDomain().getCodeSource().getLocation().getPath();

		if (applicationDir.endsWith(".jar")) {
			String regex = "[/]([^\\/]+\\.jar)";
			Pattern p = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher(applicationDir);
			String jar = "";
			while (m.find()) {
				jar = m.group(1);
			}
			applicationDir = applicationDir.replace(jar, "");
		}
		return applicationDir;
	}

	public String combine(String path1, String path2) {
		File file1 = new File(path1);
		File file2 = new File(file1, path2);
		String path = file2.getPath();
		if (isWindows())
			path = path.replace("/", "\\");
		else
			path = path.replace("\\", "/");
		return path;
	}

	public String readFile(String filePath) throws Exception {
		String data = "";

		try {
			File file = new File(filePath);
			if (file.exists()) {

				@SuppressWarnings("resource")
				FileInputStream fin = new FileInputStream(file);
				byte fileContent[] = new byte[(int) file.length()];
				fin.read(fileContent);
				data = new String(fileContent);
			} else
				return "";
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}

		// remove bom
		data = data.replace("﻿", "");
		data = data.replace("﻿\r\n", "\n");
		data = data.replace("﻿\r", "\n");
		return data.replace("ï»¿", "");

	}

	@SuppressWarnings("deprecation")
	public List<String> readLines(String filePath) throws Exception {
		return FileUtils.readLines(getFile(filePath));
	}

	public void copyFileToFolder(File file, File folder) throws IOException {
		String newPath = combine(folder.getPath(), file.getName());
		File fileNew = new File(newPath);
		if (fileNew.exists()) {
			fileNew.delete();
		}
		FileUtils.copyFileToDirectory(file, folder);
	}

	public String getStr(Object obj) {
		try {
			if (obj == null)
				return "";
			else
				return obj.toString();

		} catch (Exception e) {
			return "";
		}
	}

	public Boolean getBool(Object obj) {
		try {
			if (obj == null)
				return false;
			else {
				if (obj.toString().equals("1") || obj.toString().toLowerCase().trim().equals("true"))
					return true;
				else
					return Boolean.valueOf(obj.toString());
			}

		} catch (Exception e) {
			return false;
		}
	}

	public int getInt(Object obj) {
		try {
			if (obj == null)
				return 0;
			else if (obj.equals(""))
				return 0;
			else
				return Integer.parseInt(obj.toString());
		} catch (Exception e) {
			return 0;
		}
	}

	public long getLong(Object obj) {
		try {
			if (obj == null)
				return 0;
			else if (obj.equals(""))
				return 0;
			else
				return Long.parseLong(obj.toString());
		} catch (Exception e) {
			return 0;
		}
	}

	public float getFloat(Object obj) {
		try {
			if (obj == null)
				return 0;
			else if (obj == "")
				return 0;
			else
				return Float.parseFloat(obj.toString().trim());

		} catch (Exception e) {
			return 0;
		}
	}

	public double getDouble(Object obj) {
		try {
			if (obj == null)
				return 0;
			else if (obj == "")
				return 0;
			else
				return Double.parseDouble(obj.toString().trim());

		} catch (Exception e) {
			return 0;
		}
	}

	public boolean IsEmpty(Object object) {
		if (object == null)
			return true;

		if (object.toString().trim().length() == 0)
			return true;

		return false;
	}

	public boolean IsNull(Object object) {
		if (object == null)
			return true;

		return false;
	}

	public boolean IsExist(java.io.File file) {
		if (file != null && file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean IsExist(String filepath) {
		File file = getFile(filepath);
		if (file != null && file.exists()) {
			return true;
		} else {
			return false;
		}
	}

	public boolean validateFile(String filepath) {
		File file = getFile(filepath);
		if (file != null && file.exists()) {
			if (!file.canWrite()) {
				return false;
			}
			/* Java lies on Windows */
			try {
				new FileOutputStream(file, true).close();
			} catch (IOException e) {
				return false;
			}
			return true;
		} else {
			try {
				file.createNewFile();
				file.delete();
				return true;
			} catch (Exception e) {
				return false;
			}
		}
	}

	public void createDir(String filepath) {
		getFile(filepath).mkdirs();
	}

	public File getFile(String filepath) {
		return new File(filepath);
	}

	public String getExtension(String filepath) {
		return FilenameUtils.getExtension(filepath);
	}

	public String getName(String filepath) {
		return FilenameUtils.getName(filepath);
	}

	public String getBaseName(String filepath) {
		return FilenameUtils.getBaseName(filepath);
	}

	public String getParentPath(String filepath) {
		File file = getFile(filepath);
		if (file != null && file.getParentFile() != null) {
			return file.getParentFile().getPath();
		} else {
			return "";
		}
	}

	public void WriteFile(String filePath, String content) throws Exception {
		WriteFile(filePath, content, false);
	}

	public void WriteFile(String filePath, String content, boolean isAppend) throws Exception {
		if (IsEmpty(filePath) || IsEmpty(content))
			return;

		try {
			File file = new File(filePath);
			if (file.getParentFile() != null && (!IsExist(file.getParentFile()) || !file.getParentFile().isDirectory()))
				file.getParentFile().mkdirs();

			FileUtils.writeStringToFile(file, content, "UTF-8", isAppend);
			// FileUtils.writeStringToFile(file, content);
		} catch (Exception ex) {
			throw ex;
		}
	}

	public String getLogPath() {
		String jarPath = getJarPath();
		String logPath = combine(jarPath, "Log");

		return logPath;
	}

	public void writeLog(String path, String message) {
		writeLog(path, message, false);
	}

	public void writeLog(String path, String message, boolean isError) {
		writeLog(path, "", message, isError);
	}

	public void writeLog(String path, String inputfile, String message, boolean isError) {
		if (verbose == 1) {
			synchronized (_oLockerFile) {

				BufferedWriter oBuffer = null;
				try {

					// Get current date
					Calendar oCal = Calendar.getInstance();
					//
					SimpleDateFormat oDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
					String logfilepath = path;
					if (isWindows())
						logfilepath = logfilepath.replace("/", "\\");
					else
						logfilepath = logfilepath.replace("\\", "/");

					File file = new File(logfilepath);
					if (file.getParent() != null) {
						File dir = new File(file.getParent());
						if (!dir.exists()) {
							// create directory
							dir.mkdirs();
						}
					}

					if (!IsEmpty(inputfile)) {
						message = "File: " + inputfile + ", " + message;
					}

					// Create or append file
					FileWriter oFileWriter = new FileWriter(logfilepath, true);
					oBuffer = new BufferedWriter(oFileWriter);
					String text = oDateTimeFormat.format(oCal.getTime()) + "\t" + (isError ? "ERROR" : "INFO") + "\t"
							+ message;
					//
					oBuffer.write(text);
					oBuffer.newLine();

				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					if (oBuffer != null) {
						try {
							oBuffer.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void copyFile(String sorucePath, String targetPath) throws Exception {
		copyFile(getFile(sorucePath), getFile(targetPath));
	}

	public void copyFile(File sourceFile, File targetFile) throws Exception {
		if (!sourceFile.exists()) {
			throw new Exception("File does not exist");
		}

		if (targetFile.isDirectory()) {
			String filename = sourceFile.getName();
			String targetFilePath = combine(targetFile.getPath(), filename);
			targetFile = new File(targetFilePath);
		}

		if (targetFile.exists())
			targetFile.delete();
		FileUtils.copyFile(sourceFile, targetFile);

	}

	public void moveFile(String sourcePath, String targetPath) throws Exception {
		moveFile(getFile(sourcePath), getFile(targetPath));
	}

	public void moveFile(File sourceFile, File targetFile) throws Exception {
		if (!sourceFile.exists()) {
			throw new Exception("File does not exist");
		}

		if (targetFile.isDirectory()) {
			String filename = sourceFile.getName();
			String targetFilePath = combine(targetFile.getPath(), filename);
			targetFile = new File(targetFilePath);
		}

		if (targetFile.exists())
			targetFile.delete();
		FileUtils.moveFile(sourceFile, targetFile);

	}

	public void deleteFile(String filePath) {
		deleteFile(getFile(filePath));
	}

	public void deleteFile(File file) {
		file.delete();
	}

	public void checkPermissions(String path) {
		try {
			File file = getFile(path);
			file.setExecutable(true);
			file.setReadable(true);
			file.setWritable(true);
		} catch (Exception e) {
		}
	}

	public void print(String message) {
		print("", message);
	}

	public void print(String file, String message) {
		if (verbose == 1) {
			if (!IsEmpty(file)) {
				message = "File: " + file + ", " + message;
			}
			System.out.println(message);
		}
	}

	public void setVerbose(int val) {
		verbose = val;
	}

	public String getConfigPath() {
		return combine(getJarPath(), "patent.json");
	}

	public String replaceText(HashMap<String, String> hash, String text) {
		String rtext = text;
		if (hash != null && hash.size() > 0) {
			for (@SuppressWarnings("rawtypes")
			Map.Entry me : hash.entrySet()) {
				String search = me.getKey().toString();
				String replace = me.getValue().toString();

				// rtext = rtext.replaceAll(Pattern.quote(search), replace);
				rtext = rtext.replaceAll(search, replace);
			}
		}

		return rtext;
	}

	public String getStackTrace(Exception exception) {
		String text = "";
		Writer writer = null;
		try {
			writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			exception.printStackTrace(printWriter);
			text = writer.toString();
		} catch (Exception e) {

		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {

				}
			}
		}
		return text;
	}

	public String getOutputError(Exception e) {

		StringBuffer html = new StringBuffer("");
		html.append("<html>" + "\n");
		html.append("<error>" + "\n");
		html.append("<message>" + "\n");
		html.append("<![CDATA[" + "\n");
		html.append(e.getMessage() + "\n");
		html.append("]]>" + "\n");
		html.append("</message>" + "\n");
		html.append("<stacktrace>" + "\n");
		html.append("<![CDATA[" + "\n");
		html.append(getStackTrace(e) + "\n");
		html.append("]]>" + "\n");
		html.append("</stacktrace>" + "\n");
		html.append("</error>" + "\n");
		html.append("</html>" + "\n");

		return html.toString();
	}

	public JSONObject getJSONObject(String sData) {
		if (sData == null || sData.trim().length() == 0)
			return null;
		else
			return new JSONObject(sData);
	}

	public String getJSONValue(JSONObject json, String name) {
		if (json != null && json.has(name))
			return json.get(name).toString();
		else
			return null;
	}

	public String getJSONValue(JSONObject json, String parentname, String name) {
		if (json != null && json.has(parentname)) {
			JSONObject jobj = json.getJSONObject(parentname);

			if (jobj != null && jobj.has(name)) {
				return jobj.get(name).toString();
			} else {
				return null;
			}
		} else
			return null;
	}

	public JSONObject getJSONObject(JSONObject json, String name) {
		if (json != null && json.has(name))
			return json.getJSONObject(name);
		else
			return null;
	}

	public JSONObject getJSONObject(JSONObject json, String parentname, String name) {
		if (json != null && json.has(parentname)) {
			JSONObject jobj = json.getJSONObject(parentname);

			if (jobj != null && jobj.has(name)) {
				return jobj.getJSONObject(name);
			} else {
				return null;
			}
		} else
			return null;
	}

	public JSONArray getJSONArray(JSONObject json, String name) {
		if (json != null && json.has(name))
			return json.getJSONArray(name);
		else
			return null;
	}

	public JSONArray getJSONArray(JSONObject json, String parentname, String name) {
		if (json != null && json.has(parentname)) {
			JSONObject jobj = json.getJSONObject(parentname);

			if (jobj != null && jobj.has(name)) {
				return jobj.getJSONArray(name);
			} else {
				return null;
			}
		} else
			return null;
	}

	public String[] getFiles(String dir, String[] pattern, boolean recursive) {
		IOFileFilter fileFilter = new WildcardFileFilter(pattern);
		Collection<File> files = FileUtils.listFiles(new File(dir), fileFilter,
				recursive ? TrueFileFilter.INSTANCE : null);

		File[] afiles = files.toArray(new File[0]);
		files = Arrays.asList(afiles);

		List<String> fileList = new ArrayList<String>();
		if (files != null && files.size() > 0) {
			for (File file : files) {
				fileList.add(file.getPath());
			}
			return fileList.toArray(new String[0]);
		} else {
			return new String[] {};
		}
	}
}