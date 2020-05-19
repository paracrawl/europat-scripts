package patentdata.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import patentdata.utils.Common;

public class ProcessClean {

	public static void main(String[] args) {

		if (args == null || args.length == 0 || args[0].equals("--help")) {

			/**
			 * Print out help
			 */
			printHelp();
			System.exit(0);

		}

		String inputDir = "", outputDir = "", logFile = "";

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
				} else if (key.equals("L")) {
					logFile = parm;
				}
				key = "";
			}
		}

		ProcessClean oProcess = new ProcessClean();
		try {
			oProcess.Start(inputDir, outputDir, logFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void printHelp() {
		System.out.println("Usage:");
		System.out.println("java -jar patent-uspto.jar -I inputDir -O outputDir -L logFile");
		System.out.println("Arguments:");
		System.out.print("-I <input_dir>\t");
		System.out.println("specifies the path to the input file or directory");

		System.out.print("-O <output_dir>\t");
		System.out.println("specifies the path to the output directory");

		System.out.print("-L <log_path>\t");
		System.out.println("(optional) specifies the path to the log file");
	}

	private String outputDirectory = "";
	private Common common = null;
	private String logPath = "";
	private Pattern pRemoveInline;
	private Pattern pReplaceUnprocessable1, pReplaceUnprocessable2;
	private Pattern pAddSpacing;
	private Pattern pBreak;
	private Pattern pRemainingTags1, pRemainingTags2, pRemainingTags3, pRemainingTags4, pRemainingTags5;

	public ProcessClean() {
		common = new Common();
		pareparePattern();
	}

	public void Start(String inputPath, String outputPath, String logPath) throws Exception {
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
			
			// log
			this.logPath = logPath;

			if (oInput.isDirectory()) {
				File[] files = oInput.listFiles();
				for (File file : files) {

					try {
						common.writeLog(this.logPath, "File: " + file.getName());
						Read(file.getPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				common.writeLog(this.logPath, "File: " + oInput.getName());
				Read(oInput.getPath());
			}
		} finally {

		}
	}

	private void Read(String inputFilePath) {
		BufferedReader br;
		FileWriter fw = null;
		File inFile = new File(inputFilePath);
		try {
			String outputFilePath = common.combine(this.outputDirectory, inFile.getName());
			fw = new FileWriter(outputFilePath, true);
			
			br = new BufferedReader(new FileReader(inFile));
			String line;
			while ((line = br.readLine()) != null) {

				String[] cols = line.split("\t");
				if (cols.length < 4) continue;
				
				String docid1 = cols[0];
				String text1 = cols[1];
				
				String docid2 = cols[2];
				String text2 = cols[3];
				
				//clean
				String text1clean = cleanTags(text1);
				String text2clean = cleanTags(text2);

				String lineClean = String.format("%s\t%s\t%s\t%s\n", docid1, text1clean, docid2, text2clean);
				fw.write(lineClean);
				fw.flush();
			}

		} catch (FileNotFoundException e) {
			common.writeLog(this.logPath, "Error(Read): " + common.getStackTrace(e));
		} catch (IOException e) {
			common.writeLog(this.logPath, "Error(Read): " + common.getStackTrace(e));
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					common.writeLog(this.logPath, "Error(Read.finally): " + common.getStackTrace(e));
				}
				fw = null;
			}
		}
	}

	private void pareparePattern() {
	    //Remove inline unwanted
	    pRemoveInline = Pattern.compile("(<\\?delete-start.*?(<\\?delete-end).*?>)");
	    
	    //Replace unprocessable content with datatype markers so that alignment can still work
	    pReplaceUnprocessable1 = Pattern.compile("(<maths.*?(<\\/maths>))");
	    pReplaceUnprocessable2 = Pattern.compile("(<\\?in-line-formulae .end=\"lead\"\\?>).*(<\\?in-line-formulae .end=\"tail\"\\?>)");
	    
		//Add spacing
	    pAddSpacing = Pattern.compile("<(\\/{0,1})(sup2|sub2|sup|sub)>");

	    //Start breaking lines
		//Known paragraph breaks
	    pBreak = Pattern.compile("<\\/(claim|mrow|entry|p|tables|table|tbody|thead|tgroup|claim-text|chemistry|row|col)>|<(tables|mrow|tbody|thead|tgroup|entry|p|claim-text|chemistry|img|table|row|col).*?>|<claim .*?>|<claim>|<claim-text.*?>|<img.*?>|<br\\/>");

	    //Remove all remaining tags
	    pRemainingTags1 = Pattern.compile("<.*>");
	    pRemainingTags2 = Pattern.compile("[ ]{2,}");
	    pRemainingTags3 = Pattern.compile("\n{3,}");
	    pRemainingTags4 = Pattern.compile("[ ]\n|\n[ ]");
	    pRemainingTags5 = Pattern.compile("/\n{1,}");
	}

	private String cleanTags(String text) {
	    //Remove inline unwanted
		text = pRemoveInline.matcher(text).replaceAll(" ");

	    //Replace unprocessable content with datatype markers so that alignment can still work
		text = pReplaceUnprocessable1.matcher(text).replaceAll(" EPMATHMARKEREP ");
		text = pReplaceUnprocessable2.matcher(text).replaceAll(" EPFORMULAMARKEREP ");

		//Add spacing
		text = pAddSpacing.matcher(text).replaceAll(" ");

	    //Start breaking lines
		//Known paragraph breaks
		text = pBreak.matcher(text).replaceAll("\n\n");

		//Remove all remaining tags
	    text = pRemainingTags1.matcher(text).replaceAll("");
		text = pRemainingTags2.matcher(text).replaceAll(" ");
		text = pRemainingTags3.matcher(text).replaceAll("\n\n");
		text = pRemainingTags4.matcher(text).replaceAll("\n");
		text = pRemainingTags5.matcher(text).replaceAll("<p>");

		return text;
	}
}
