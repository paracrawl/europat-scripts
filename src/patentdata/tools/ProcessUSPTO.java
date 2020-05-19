package patentdata.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;

import patentdata.utils.Common;
import patentdata.utils.CompressFile;
import patentdata.utils.USPTOXml;

public class ProcessUSPTO {

	public static void main(String[] args) {

		if (args == null || args.length == 0 || args[0].equals("--help")) {

			/**
			 * Print out help
			 */
			printHelp();
			System.exit(0);

		}

		String inputDir = "", outputDir = "", tempDir = "", logFile = "";

		/**
		 * Get command-line arguments
		 */
		String key = "";
		for (String parm : args) {

			if (parm.startsWith("-")) {
				key = parm.substring(1);
			} else {
				if (key.equals("I")) {
					inputDir = parm;
				} else if (key.equals("O")) {
					outputDir = parm;
				} else if (key.equals("T")) {
					tempDir = parm;
				} else if (key.equals("L")) {
					logFile = parm;
				}
				key = "";
			}
		}

		ProcessUSPTO oProcess = new ProcessUSPTO();
		try {
			oProcess.Start(inputDir, outputDir, tempDir, logFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printHelp() {
		System.out.println("Usage:");
		System.out.println("java -jar patent-uspto.jar -I inputDir -O outputDir -T tempDir -L logFile");
		System.out.println("Arguments:");
		System.out.print("-I <input_dir>\t");
		System.out.println("specifies the path to the input file or directory");

		System.out.print("-O <output_dir>\t");
		System.out.println("specifies the path to the output directory");

		System.out.print("-T <temp_dir>\t");
		System.out.println("(optional) specifies the path to the temp directory");

		System.out.print("-L <log_path>\t");
		System.out.println("(optional) specifies the path to the log file");
	}

	private String service = "USPTO";
	private String lang = "EN";
	private String sTitle = "Title";
	private String sAbstract = "Abstract";
	private String sClaims = "Claims";
	private String sDescription = "Description";
	private String sMetadata = "Metadata";
	private USPTOXml oXml = null;
	private String tempDirectory = "";
	private String outputDirectory = "";
	private HashMap<String, FileWriter> fwList = new HashMap<String, FileWriter>();
	private Common common = null;
	private String logPath = "";

	public ProcessUSPTO() {
		common = new Common();
	}

	public void Start(String inputPath, String outputPath, String tempPath, String logPath) throws Exception {
		try {
			// input
			if (!new File(inputPath).exists()) {
				throw new Exception("Input path does not exist.");
			}
			common.writeLog(this.logPath, "Input: " + inputPath);
			File oInput = new File(inputPath);

			// output
			this.outputDirectory = outputPath;
			if (!new File(this.outputDirectory).exists()) {
				new File(this.outputDirectory).mkdirs();
			}
			common.writeLog(this.logPath, "Output: " + this.outputDirectory);
			
			if (inputPath.equals(this.outputDirectory)) {
				throw new Exception("Input and Output path cannot be the same.");
			}

			// temp
			this.tempDirectory = tempPath;
			if (common.IsEmpty(this.tempDirectory)) {
				this.tempDirectory = (oInput.isDirectory() ? oInput.getPath() : oInput.getParent()) + "-temp";
			}
			if (!new File(this.tempDirectory).exists()) {
				new File(this.tempDirectory).mkdirs();
			}
			common.writeLog(this.logPath, "Temp: " + this.tempDirectory);

			if (inputPath.equals(this.tempDirectory)) {
				throw new Exception("Input and Temp path cannot be the same.");
			}

			// log
			this.logPath = logPath;

			oXml = new USPTOXml();
			CompressFile gZipFile = new CompressFile();
			if (oInput.isDirectory()) {
				File[] files = oInput.listFiles();
				for (File file : files) {

					try {
						common.writeLog(this.logPath, "File: " + file.getName());

						String name = file.getName();
						name = name.substring(0, name.lastIndexOf("."));
						String decompressedFile = common.combine(this.tempDirectory, name);
						gZipFile.unzip(file.getPath(), decompressedFile);

						Read(decompressedFile);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				common.writeLog(this.logPath, "File: " + oInput.getName());

				String name = oInput.getName();
				name = name.substring(0, name.lastIndexOf("."));
				String decompressedFile = common.combine(this.tempDirectory, name);
				gZipFile.unzip(oInput.getPath(), decompressedFile);

				Read(decompressedFile);
			}
		} finally {
			End();
		}
	}

	private void End() {
		if (fwList != null) {
			for (Entry<String, FileWriter> entry : fwList.entrySet()) {
				try {
					entry.getValue().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		fwList = null;
	}

	private void Read(String inputFilePath) {
		BufferedReader br;
		File inFile = new File(inputFilePath);
		try {
			StringBuilder sb = new StringBuilder("");
			br = new BufferedReader(new FileReader(inFile));
			String line;
			int i = 0, x = 1;
			String marker = "";
			while ((line = br.readLine()) != null) {

				if (i == 0) {
					if (line.trim().indexOf("<?xml ") > -1) {
						marker = "<?xml ";
					} else if (line.trim().indexOf("<!DOCTYPE ") > -1) {
						marker = "<!DOCTYPE ";
					} else if (line.trim().indexOf("HHHHHT ") > -1) {
						marker = "PATN";

						// not support
						break;
					}
				}

				if (i > 0 && line.trim().startsWith(marker)) {
					// extract xml
					String xml = sb.toString();
					common.writeLog(this.logPath, "- start index: " + x);
					Extract(xml);
					common.writeLog(this.logPath, "- finish index: " + x);

					// reset
					sb = new StringBuilder("");
					x++;
				}

				sb.append(line);
				i++;
			}

			if (sb.toString().length() > 0) {
				// extract xml
				String xml = sb.toString();
				common.writeLog(this.logPath, "- start index: " + x);
				Extract(xml);
				common.writeLog(this.logPath, "- finish index: " + x);

				sb = new StringBuilder("");
			}

		} catch (FileNotFoundException e) {
			common.writeLog(this.logPath, "Error(Read): " + common.getStackTrace(e));
		} catch (IOException e) {
			common.writeLog(this.logPath, "Error(Read): " + common.getStackTrace(e));
		} finally {
			if (inFile != null && inFile.exists()) {
				inFile.delete();
				inFile = null;
			}
		}
	}

	private String getTabFileName(String year, String type) throws IOException {
		String name = String.format("%s-%s-%s-%s.tab", this.service, this.lang, year, type);
		String filePath = common.combine(this.outputDirectory, name);
		if (!fwList.containsKey(name)) {
			FileWriter fw = new FileWriter(filePath, true);
			fwList.put(name, fw);
		}

		return name;
	}

	private void Extract(String xml) {
		try {

			oXml.setXmlString(xml);

			String docid = oXml.getDocID();
			String year = oXml.getYear();
			String date = oXml.getDate();
			//
			String title = oXml.getTitle();
			String abstract_ = oXml.getAbstract();
			String claims = oXml.getClaims();
			String description = oXml.getDescription();
			String metadata = oXml.getMetaData();

			Write(year, sTitle, docid, date, title);
			Write(year, sAbstract, docid, date, abstract_);
			Write(year, sClaims, docid, date, claims);
			Write(year, sDescription, docid, date, description);
			Write(year, sMetadata, docid, date, metadata);

		} catch (IOException e) {
			common.writeLog(this.logPath, "Error(Extract): " + common.getStackTrace(e));
		} catch (Exception e) {
			common.writeLog(this.logPath, "Error(Extract): " + common.getStackTrace(e));
		}
	}

	private void Write(String year, String type, String docid, String date, String data) throws IOException {
		if (!common.IsEmpty(data)) {
			String fileName = getTabFileName(year, type);
			String line = String.format("%s\t%s\t%s\n", docid, date, data);
			fwList.get(fileName).write(line);
			fwList.get(fileName).flush();
		}
	}
}
