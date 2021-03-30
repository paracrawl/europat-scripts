package patentdata.tools;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import patentdata.utils.ConstantsVal;

public class PatentManager {

	public static void main(String[] args) throws Exception {

		PatentManager oTool = new PatentManager();
		if (oTool.validateOption(args)) {
			
			String line = String.join(" ", args);
			List<String> list = oTool.listStringPattern("(?i)-(search|extractid|getfamily|insertfamily|getpair|segment|"
														+ "clean|processuspto|processep|insertpatent|exportpair|cleanascii|segmentsingle|exportpairfamily|getepo|exportpairbydate|processepfront"
														+ "|exportpairfromtab)([ ])", line);
			if (list.isEmpty() || list.size() > 1)
				throw new Exception("Select method: -search or -extractid or -getfamily or -insertfamily or -getpair "
									+ "or -clean or -segment or -processuspto or -processep or -insertpatent or -exportpair or -cleanascii or -segmentsingle "
									+ "or -exportpairfamily or -getepo or -exportpairbydate or -exportpairfromtab or -processepfront");
			String method = (String) list.get(0);
			line = line.replace("-" + method, "").trim();
			args = line.split(" ");
			if ("search".equalsIgnoreCase(method)) {
				new Search(args);
			} else if ("extractid".equalsIgnoreCase(method)) {
				new ExtractId(args);
			} else if ("getfamily".equalsIgnoreCase(method)) {
				new GetFamily(args);
			} else if ("insertfamily".equalsIgnoreCase(method)) {
				new InsertFamily(args);
			} else if ("segment".equalsIgnoreCase(method)) {
				new SentenceSegmentCaller(args);
			}else if ("clean".equalsIgnoreCase(method)) {
				new ProcessCleanCaller(args);
			}else if ("processuspto".equalsIgnoreCase(method)) {
				new ProcessUsptoCaller(args);
			}else if ("processep".equalsIgnoreCase(method)) {
				new ProcessEPCaller(args);
			}else if ("insertpatent".equalsIgnoreCase(method)) {
				new ImportPatentToDBCaller(args);
			}else if ("exportpair".equalsIgnoreCase(method)) {
				new ExportPatentPairCaller(args);
			}else if ("cleanascii".equalsIgnoreCase(method)) {
				new ProcessCleanAsciiCaller(args);
			}else if ("segmentsingle".equalsIgnoreCase(method)) {
				new SentenceSegmentSingleCaller(args);
			}else if ("exportpairfamily".equalsIgnoreCase(method)) {
				new ExportPatentPairFamilyCaller(args);
			}else if ("getepo".equalsIgnoreCase(method)) {
				new GetPatentContentsFromEPO(args);
			}else if ("exportpairbydate".equalsIgnoreCase(method)) {
				new ExportPatentPairCallerByDate(args);
			}else if ("exportpairfromtab".equalsIgnoreCase(method)) {
				new ExportPatentPairFromTabCaller(args);
			}else if ("processepfront".equalsIgnoreCase(method)) {
				new ProcessEPFrontCaller(args);
			}
//			else if ("getiso8879".equalsIgnoreCase(method)) {
//				new GettingISO8879Caller(args);
//			} 
			else {
				throw new Exception("Option should be \"-search\" or \"-extractid\" or \"-getfamily\" or \"-insertfamily\"  or \"-getpair\" "
									+ "or \"-clean\" or \"-segment\" or \"-processuspto\" or \"-processep\" or \"-insertpatent\""
									+ " or \"-exportpair\" or \"-cleanascii\" or \"-segmentsingle\" or \"-exportpairfamily\" or \"-getepo\" or \"-exportpairbydate\""
									+ " or \"-exportpairfromtab\" or \"-processepfront\"");
			}
		}
	}

	private boolean validateOption(String[] args) {
		try {
			if (args == null || args.length == 0 || args.length < 1 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Select method: -search or -extractid or -getfamily or -getpair (Required)");
				System.out.println("------------------------");
				System.exit(0);
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
			return false;
		}
	}

	protected List<String> listStringPattern(String patternString, String text) {
		List<String> list = new ArrayList<String>();
		Matcher matcher = Pattern.compile(patternString).matcher(text);
		while (matcher.find()) {
			list.add(matcher.group(1));
		}
		return list;
	}

	protected String getOptionConfig(String line) throws Exception {
		String sConfigPath = "";
		if (line.matches(".*(\\-C).*")) {
			sConfigPath = getOptionValue("C", line);
			if (StringUtils.isEmpty(sConfigPath) || !sConfigPath.matches("(?i).*\\.json")) {
				throw new Exception("Expect a JSON file: -C \"config/path/patent.json\"");
			}
		}
		return sConfigPath;
	}

	protected String getOptionValue(String option, String line) {
		String[] arr = line.trim().split("-" + option);
		return arr.length > 1 ? arr[1].trim().split(" ")[0].trim() : "";
	}

}

class Search extends PatentManager {

	public Search(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String line = String.join(" ", args);
				String sConfigPath = getOptionConfig(line);

				String sDate = line.replace("-C " + sConfigPath, "").trim();
				if (sDate == null || sDate.length() == 0)
					throw new Exception("Date is required.");
				if (!sDate.matches("\\d{8}"))
					throw new Exception("Date should be in YYYYMMDD format.");

				new Patent(sConfigPath).getPatentByDate(sDate);
			}
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}finally {
			System.exit(0);
		}
	}

	private boolean validateMandatory(String[] args) {
		try {
			if (args == null || args.length == 0 || args.length < 1 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println("1. <Date> (Required): Date to search patents (YYYYMMDD)");
				System.out.println("2. -C <ConfigPath> (Optional): Config path (JSON only)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent.jar -search \"20191231\" -C \"config/path/patent.json\"");
				System.out.println("------------------------");
				System.exit(0);
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			System.exit(1);
			return false;
		}finally {
			System.exit(0);
		}
	}

}

class ExtractId extends PatentManager {

	public ExtractId(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String line = String.join(" ", args);

				String sInputPath = getOptionValue("I", line);
				if (sInputPath == null || sInputPath.length() == 0)
					throw new Exception("Required: -I <InputPath>");

				File fileInput = new File(sInputPath);
				if (!fileInput.exists())
					throw new Exception("InputPath is not exist.");
				if (!fileInput.isDirectory()
						&& !"xml".equalsIgnoreCase(FilenameUtils.getExtension(fileInput.toString())))
					throw new Exception("InputPath must be a XML file.");

				String sOutputPath = getOptionValue("O", line);
				if (sOutputPath == null || sOutputPath.length() == 0)
					throw new Exception("Required: -O <OutputPath>");

				File fileOutput = new File(sOutputPath);
				String sOutExt = FilenameUtils.getExtension(fileOutput.toString());
				if (!StringUtils.isEmpty(sOutExt) && !"txt".equalsIgnoreCase(sOutExt))
					throw new Exception("OutputPath must be a TXT file.");

				new Patent(getOptionConfig(line)).getPatentIds(fileInput, fileOutput);
			}
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private boolean validateMandatory(String[] args) {
		try {
			if (args == null || args.length == 0 || args.length < 2 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println("1. -I <InputPath> (Required): Input path or folder (XML only)");
				System.out.println("2. -O <OutputPath> (Required): Output path or folder (TXT only)");
				System.out.println("3. -C <ConfigPath> (Optional): Config path (JSON only)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println(
						"java -jar patent.jar -extractid -I \"input/directory\" -O \"output/directory\" -C \"config/path/patent.json\"");
				System.out.println("------------------------");
				System.exit(0);
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			System.exit(0);
			return false;
		}
	}

}

class GetFamily extends PatentManager {

	public GetFamily(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String line = String.join(" ", args);
				String sConfigPath = getOptionConfig(line);

				String sOutputFolder = getOptionValue("O", line);
				if (sOutputFolder == null || sOutputFolder.length() == 0)
					throw new Exception("Required: -O <OutputFolder>");
				File folderOutput = new File(sOutputFolder);

				if (line.matches(".*(\\-I).*")) {
			               		File fileInput = new File(getOptionValue("I", line));
					if (!fileInput.exists())
						throw new Exception("InputPath is not exist.");

					new Patent(sConfigPath).getFamily(fileInput, folderOutput);
				} else if (line.matches(".*(\\-D).*")) {
					new Patent(sConfigPath).getFamily(getOptionValue("D", line) ,folderOutput);
				} else {
					throw new Exception("Required: -I <InputPath> or -D <DocId> ");
				}
			}
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private boolean validateMandatory(String[] args) {
		try {
			if (args == null || args.length == 0 || args.length < 2 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println(
						"1. -I <InputPath> or -D <DocId> (Required): Input file path or folder (TXT only) or Document Id (CountryCode.DocNo.KindCode)");
				System.out.println("2. -O <OutputFolder> (Required): Output folder");
				System.out.println("3. -C <ConfigPath> (Optional): Config path (JSON only)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println(
						"java -jar patent.jar -getfamily -I \"input/path/ids_201912.txt\" -O \"output/folder/\" -C \"config/path/patent.json\"");
				System.out.println(
						"java -jar patent.jar -getfamily -D \"JP.H07196059.A\" -O \"output/folder/\" -C \"config/path/patent.json\"");
				System.out.println("------------------------");
				System.exit(0);
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			System.exit(0);
			return false;
		}
	}

}

class InsertFamily extends PatentManager {

	public InsertFamily(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				
				String sInputPath = commandLine.getOptionValue('I');
				if (sInputPath == null || sInputPath.length() == 0)
					throw new Exception("Required: -I <InputPath>");
				
				File fileInput = new File(sInputPath);
				if (!fileInput.exists())
					throw new Exception("InputPath is not exist.");
				
				new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.insertFamily(fileInput);
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use clean -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
		
	}

	
	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("insertfamily", options);
	}

}

class SentenceSegmentCaller{

	public SentenceSegmentCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				
				String B = commandLine.getOptionValue('B');
				int pipeBuffer = 0;
				if (!StringUtils.isEmpty(B)) {
					try {
						pipeBuffer = Integer.valueOf(B);
					}catch (Exception e) {
						e.printStackTrace();
						pipeBuffer = 0;
					}
				}
				new SentenceSegment(commandLine.getOptionValue('C'), commandLine.hasOption('V'), pipeBuffer)
					.Run(commandLine.getOptionValue('I'), commandLine.getOptionValue('O'));
				
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use segment -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("O", "output", true, "Output directory path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("B", "PipeBuffer", true, "Maximum pipe buffer.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("O", "output", true, "Output directory path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("B", "PipeBuffer", true, "Maximum pipe buffer.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("segment", options);
	}
	
}

class ProcessCleanCaller {

	public ProcessCleanCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				new ProcessClean(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.Start(commandLine.getOptionValue('I'), commandLine.getOptionValue('O'));
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use clean -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("O", "output", true, "Output directory path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("O", "output", true, "Output directory path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("clean", options);
	}
	
}

class ProcessUsptoCaller {

	public ProcessUsptoCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				new ProcessUSPTO(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.Start(commandLine.getOptionValue('I'), commandLine.getOptionValue('O'), commandLine.getOptionValue('T'));
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use processuspto -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("O", "output", true, "Output directory path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("T", "tempDirectory", true, "Temporary directory path.");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("O", "output", true, "Output directory path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("T", "tempDirectory", true, "Temporary directory path.");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("processuspto", options);
	}
	
}

class ProcessEPCaller {

	public ProcessEPCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				File fileInput = new File(commandLine.getOptionValue('I'));
				if (!fileInput.exists())
					throw new Exception("InputPath is not exist.");
				if (!fileInput.isDirectory()
						&& !"zip".equalsIgnoreCase(FilenameUtils.getExtension(fileInput.toString())))
					throw new Exception("InputPath must be a ZIP file.");
				
				String sOutputPath = commandLine.getOptionValue('O');
				File fileOutput = new File(sOutputPath);
				if (fileOutput.exists()) {
					fileOutput.mkdir();
				}
				
				String sYear = commandLine.getOptionValue('Y');
				String sNumberYear = commandLine.getOptionValue('N');

				if (StringUtils.isEmpty(sNumberYear)) {
					sNumberYear = "1";
				}
				
				new PatentUnzip(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.getDataCoverageOnDisk(fileInput, fileOutput, Integer.parseInt(sYear),  Integer.parseInt(sNumberYear));
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use processep -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("O", "output", true, "Output directory path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("Y", "year", true, "Start year.");
	    options.addOption("N", "numberYear", true, "Number of year process from start year.");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("O", "output", true, "Output directory path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addRequiredOption("Y", "year", true, "Start year.");
	    options.addOption("N", "numberYear", true, "Number of year process from start year.");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("processep", options);
	}
}

class ImportPatentToDBCaller {

	public ImportPatentToDBCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				String sTabFilePath = commandLine.getOptionValue('I');
				String sLang = commandLine.getOptionValue('L');
				if (!ConstantsVal.LIST_LANG.contains(sLang.toLowerCase()))
					throw new Exception("Required: -L <Language> or invalid the value <de, fr, en>.");
				
				String sSource = commandLine.getOptionValue('S');
				if (!ConstantsVal.LIST_SOURCE.contains(sSource.toLowerCase()))
					throw new Exception("Required: -S <Source> or invalid the value <us: USPTO, ep: EUROPAT>.");
				
				String sCategory = commandLine.getOptionValue('G');
				if (!ConstantsVal.LIST_CATEGORY.contains(sCategory.toLowerCase()))
					throw new Exception("Required: -G  <Category> or invalid the value <abstract, claim, dscp, metadata, title>.");
				
				new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.insertPatentDataToDB(sTabFilePath, sSource, sLang, sCategory);
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use insertpatent -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path (Tab file only).");
	    options.addOption("L", "Language", true, "Language: Posible value is <de, fr, en>.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("S", "SourcePatent", true, "Posible value is <us: USPTO, ep: EUROPAT>.");
	    options.addOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path (Tab file only).");
	    options.addRequiredOption("L", "Language", true, "Language: Posible value is <de, fr, en>.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addRequiredOption("S", "SourcePatent", true, "Posible value is <us: USPTO, ep: EUROPAT>.");
	    options.addRequiredOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("insertpatent", options);
	}
}

class ExportPatentPairCaller {

	public ExportPatentPairCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				String sCategory = commandLine.getOptionValue('G');
//				if (!ConstantsVal.LIST_CATEGORY.contains(sCategory.toLowerCase()))
//					throw new Exception("Required: -G  <Category> or invalid the value <abstract, claim, dscp, metadata, title>.");
				
				
				String sSourceLang = commandLine.getOptionValue("SL");
//				if (!ConstantsVal.LIST_LANG_SOURCE.contains(sSourceLang.toLowerCase()))
//					throw new Exception("Required: -SL <Source Language> or invalid the value <DE, FR>.");
				
				String sTargetLang = commandLine.getOptionValue("TL");
//				if (!ConstantsVal.LIST_LANG_TARGET.contains(sTargetLang.toLowerCase()))
//					throw new Exception("Required: -TL <Target Language> or invalid the value <EN>.");
				
				String sYear = commandLine.getOptionValue("Y");
				if (sYear == null || sYear.trim().length() == 0)
					throw new Exception("Required: -Y <Year> or invalid the value <YYYY>.");
				
				String sOutputPath = commandLine.getOptionValue("O");
				
				new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
				.ExportPairIDS(sSourceLang, sTargetLang, sYear, sCategory, sOutputPath);
					
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use exportpair -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("SL", "input", true, "Source language: Posible value is <de, fr>.");
	    options.addOption("TL", "Language", true, "Target language: Posible value is <en>.");
	    options.addOption("Y", "configFile", true, "Year of PublicationDate.");
	    options.addOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addOption("O", "configFile", true, "Output folder path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("SL", "input", true, "Source language: Posible value is <de, fr>.");
	    options.addRequiredOption("TL", "Language", true, "Target language: Posible value is <en>.");
	    options.addRequiredOption("Y", "configFile", true, "Year of PublicationDate.");
	    options.addRequiredOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addRequiredOption("O", "configFile", true, "Output folder path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("exportpair", options);
	}
	
}

class ProcessCleanAsciiCaller {

	public ProcessCleanAsciiCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				new ProcessCleanASCII(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.Start(commandLine.getOptionValue('I'), commandLine.getOptionValue('O'));
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use cleanascii -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("O", "output", true, "Output directory path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("O", "output", true, "Output directory path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("cleanascii", options);
	}
	
}

class SentenceSegmentSingleCaller{

	public SentenceSegmentSingleCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				
				String B = commandLine.getOptionValue('B');
				int pipeBuffer = 0;
				if (!StringUtils.isEmpty(B)) {
					try {
						pipeBuffer = Integer.valueOf(B);
					}catch (Exception e) {
						e.printStackTrace();
						pipeBuffer = 0;
					}
				}
				
				String sSourceLang = commandLine.getOptionValue("SL");

				new SentenceSegmentSingleLang(commandLine.getOptionValue('C'), commandLine.hasOption('V'), pipeBuffer)
					.Run(commandLine.getOptionValue('I'), commandLine.getOptionValue('O'), sSourceLang);
				
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use segmentSingle -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("O", "output", true, "Output directory path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("B", "PipeBuffer", true, "Maximum pipe buffer.");
	    options.addOption("SL", "Language", true, "Content Language.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("O", "output", true, "Output directory path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addRequiredOption("SL", "Language", true, "Content language.");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("B", "PipeBuffer", true, "Maximum pipe buffer.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("segmentSingle", options);
	}
	
}

class ExportPatentPairFamilyCaller {

	public ExportPatentPairFamilyCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				String sCategory = commandLine.getOptionValue('G');
				String sFamilyFilePath = commandLine.getOptionValue("FM");
				String sYear = commandLine.getOptionValue("Y");
				if (sYear == null || sYear.trim().length() == 0)
					throw new Exception("Required: -Y <Year> or invalid the value <YYYY>.");
				
				String sOutputPath = commandLine.getOptionValue("O");
				String targetPairFilePath = commandLine.getOptionValue("TPF"); ;
				
				new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
				.exportPairsByFamilyFile(sFamilyFilePath, sYear, sCategory, sOutputPath, "", targetPairFilePath);
					
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use exportpairfamily -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("FM", "familyfilepath", true, "Path of family file.");
	    options.addOption("TPF", "targetPairFilePath", true, "Path of pair file path.");
	    options.addOption("Y", "configFile", true, "Year of PublicationDate.");
	    options.addOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addOption("O", "configFile", true, "Output folder path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("FM", "familyfilepath", true, "Path of family file.");
	    options.addRequiredOption("TPF", "targetPairFilePath", true, "Path of pair file path.");
	    options.addRequiredOption("Y", "configFile", true, "Year of PublicationDate.");
	    options.addRequiredOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addOption("O", "configFile", true, "Output folder path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("exportpairfamily", options);
	}
	
}

class GetPatentContentsFromEPO {

	public GetPatentContentsFromEPO(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				String sFamilyFilePath = commandLine.getOptionValue("I");
			
				String sOutputPath = commandLine.getOptionValue("O");
				
				String contentType = commandLine.getOptionValue("CT");
				if (null != contentType && contentType.trim().length() > 0 && !ConstantsVal.LIST_CONTENTTYPE_EPO.contains(contentType)){
					throw new Exception("Invalid Content type.");
				}
				if (commandLine.hasOption("ND")) {
					new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.getPatentContents(sFamilyFilePath, sOutputPath, contentType);
				}else {
					new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.getPatentContentsByDotFile(sFamilyFilePath, sOutputPath);
				}
			}

		} catch (Exception e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use getepo -h for help.");
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "IntputTabFile", true, "Path of input data.");
	    options.addOption("CT", "ContentType", true, "Content Type: <description>, <claims>, <abstract,biblio>, <abstract> or <biblio>");
	    options.addOption("O", "Outputfile", true, "Output folder path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    options.addOption("ND", "help", false, "Not dot file process.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "Intput tab file", true, "Path of input data.");
	    options.addOption("CT", "ContentType", true, "Content Type: <description>, <claims>, <abstract,biblio>, <abstract> or <biblio>");
	    options.addOption("O", "Outputfile", true, "Output folder path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    options.addOption("ND", "NotDot", false, "Not dot file process.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("getepo", options);
	}
	
}

class ExportPatentPairCallerByDate {

	public ExportPatentPairCallerByDate(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				
				String sSourceLang = commandLine.getOptionValue("SL");
//				if (!ConstantsVal.LIST_LANG_SOURCE.contains(sSourceLang.toLowerCase()))
//					throw new Exception("Required: -SL <Source Language> or invalid the value <DE, FR>.");
				
				String sTargetLang = commandLine.getOptionValue("TL");
//				if (!ConstantsVal.LIST_LANG_TARGET.contains(sTargetLang.toLowerCase()))
//					throw new Exception("Required: -TL <Target Language> or invalid the value <EN>.");
				
				String sDateStart = commandLine.getOptionValue("Y");
				if (sDateStart == null || sDateStart.trim().length() == 0)
					throw new Exception("Required: -Y <Year> or invalid the value <YYYY-MM-DD>.");
				
				String sDateEnd = commandLine.getOptionValue("E");
				if (sDateEnd == null || sDateEnd.trim().length() == 0)
					throw new Exception("Required: -E <Year> or invalid the value <YYYY-MM-DD>.");
				
				String sOutputPath = commandLine.getOptionValue("O");
				
				new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
				.ExportPairOnlyIDSByDate(sSourceLang, sTargetLang, sDateStart, sOutputPath, sDateEnd);
					
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use exportpairbydate -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("SL", "input", true, "Source language: Posible value is <de, fr>.");
	    options.addOption("TL", "Language", true, "Target language: Posible value is <en>.");
	    options.addOption("Y", "configFile", true, "Start date of PublicationDate.");
	    options.addOption("E", "configFile", true, "End date of PublicationDate.");
	    options.addOption("O", "configFile", true, "Output folder path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("SL", "input", true, "Source language: Posible value is <de, fr>.");
	    options.addRequiredOption("TL", "Language", true, "Target language: Posible value is <en>.");
	    options.addRequiredOption("Y", "Start Date", true, "Start date of PublicationDate.");
	    options.addRequiredOption("E", "End Date", true, "End date of PublicationDate.");
	    options.addRequiredOption("O", "configFile", true, "Output folder path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("exportpairbydate", options);
	}
	
}

class ExportPatentPairFromTabCaller {

	public ExportPatentPairFromTabCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				String sCategory = commandLine.getOptionValue('G');
//				if (!ConstantsVal.LIST_CATEGORY.contains(sCategory.toLowerCase()))
//					throw new Exception("Required: -G  <Category> or invalid the value <abstract, claim, dscp, metadata, title>.");
				
				
				String sSourceLang = commandLine.getOptionValue("SL");
//				if (!ConstantsVal.LIST_LANG_SOURCE.contains(sSourceLang.toLowerCase()))
//					throw new Exception("Required: -SL <Source Language> or invalid the value <DE, FR>.");
				
				String sTargetLang = commandLine.getOptionValue("TL");
//				if (!ConstantsVal.LIST_LANG_TARGET.contains(sTargetLang.toLowerCase()))
//					throw new Exception("Required: -TL <Target Language> or invalid the value <EN>.");
				
				String sYear = commandLine.getOptionValue("Y");
				if (sYear == null || sYear.trim().length() == 0)
					throw new Exception("Required: -Y <Year> or invalid the value <YYYY>.");
				
				String sOutputPath = commandLine.getOptionValue("ODR");
				String sSourceDRPath = commandLine.getOptionValue("SDR");
				
				new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
				.ExportPairIDSRealeas2(sSourceLang, sTargetLang, sYear, sCategory, sOutputPath, sSourceDRPath);
//				new Patent(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
//				.ExportPairIDSRealeas3(sSourceLang, sTargetLang, sYear, sCategory, sOutputPath, sSourceDRPath);
					
			}

		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use exportpairfromtab -h for help.");
		} catch (IllegalArgumentException e) {
			System.out.println(e);
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("SL", "input", true, "Source language: Posible value is <de, fr>.");
	    options.addOption("TL", "Language", true, "Target language: Posible value is <en>.");
	    options.addOption("Y", "configFile", true, "Year of PublicationDate.");
	    options.addOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addOption("ODR", "configFile", true, "Output folder path.");
	    options.addOption("SDR", "configFile", true, "Source folder path.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("SL", "input", true, "Source language: Posible value is <de, fr>.");
	    options.addRequiredOption("TL", "Language", true, "Target language: Posible value is <en>.");
	    options.addRequiredOption("Y", "configFile", true, "Year of PublicationDate.");
	    options.addRequiredOption("G", "CategoryData", true, "Category of Data: Posible value is <abstract, claim, dscp, metadata, title>.");
	    options.addRequiredOption("ODR", "configFile", true, "Output folder path.");
	    options.addRequiredOption("SDR", "configFile", true, "Source folder path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("exportpairfromtab", options);
	}
	
}


class ProcessEPFrontCaller {

	public ProcessEPFrontCaller(String[] args) throws Exception {
		
		Options options = createOptions();
		HelpFormatter helpFormatter = new HelpFormatter();
		CommandLineParser parser = new PosixParser();
		CommandLine commandLine = null;

		try {
			commandLine = parser.parse(options, args);

			if (commandLine.hasOption('h')) {
				printHelp(options, helpFormatter);
			}else {
				
				options = createOptionsValidate();
				commandLine = parser.parse(options, args);
				File fileInput = new File(commandLine.getOptionValue('I'));
				if (!fileInput.exists())
					throw new Exception("InputPath is not exist.");
//				if (!fileInput.isDirectory()
//						&& !"zip".equalsIgnoreCase(FilenameUtils.getExtension(fileInput.toString())))
//					throw new Exception("InputPath must be a ZIP file.");
				
				String sOutputPath = commandLine.getOptionValue('O');
				File fileOutput = new File(sOutputPath);
				if (fileOutput.exists()) {
					fileOutput.mkdir();
				}
				
				String suffixAbstract = commandLine.getOptionValue("SA");
				String suffixClaim = commandLine.getOptionValue("SC");
				String suffixDscp = commandLine.getOptionValue("SD");
				
				
				
				new PatentUnzip(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
					.getDataCoverageFront(fileInput, fileOutput, suffixAbstract, suffixClaim, suffixDscp, commandLine.hasOption("IM"));
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
			System.out.println("Unknown command. Use processepfront -h for help.");
		}
	}

	private Options createOptions() {
	    Options options = new Options();
	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addOption("O", "output", true, "Output directory path.");
	    options.addOption("SA", "output", true, "Suffix bibio directory name.");
	    options.addOption("SD", "output", true, "Suffix description directory name.");
	    options.addOption("SC", "output", true, "Suffix claim directory name.");
	    options.addOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("V", "stdout", false, "Print thru Standard output.");
	    options.addOption("IM", "isMeatadataOnly", false, "Only extract metadata file.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private Options createOptionsValidate() {
	    Options options = new Options();
	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
	    options.addRequiredOption("O", "output", true, "Output directory path.");
	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
	    options.addOption("SA", "output", true, "Suffix bibio directory name.");
	    options.addOption("SD", "output", true, "Suffix description directory name.");
	    options.addOption("SC", "output", true, "Suffix claim directory name.");
	    options.addOption("V", "map", false, "Print thru Standard output.");
	    options.addOption("IM", "isMeatadataOnly", false, "Only process metadata.");
	    options.addOption("h", "help", false, "Print this help.");
	    return options;
	}
	
	private void printHelp(Options options, HelpFormatter helpFormatter) {
		helpFormatter.printHelp("processepfront", options);
	}
}
//class GettingISO8879Caller extends PatentManager {
//
//	public GettingISO8879Caller(String[] args) throws Exception {
//		
//		Options options = createOptions();
//		HelpFormatter helpFormatter = new HelpFormatter();
//		CommandLineParser parser = new PosixParser();
//		CommandLine commandLine = null;
//
//		try {
//			commandLine = parser.parse(options, args);
//
//			if (commandLine.hasOption('h')) {
//				printHelp(options, helpFormatter);
//			}else {
//				
//				options = createOptionsValidate();
//				commandLine = parser.parse(options, args);
//				new GettingISO8879(commandLine.getOptionValue('C'), commandLine.hasOption('V'))
//					.Start(commandLine.getOptionValue('I'), commandLine.getOptionValue('O'));
//			}
//
//		} catch (ParseException e) {
//			System.out.println(e.getMessage());
//			System.out.println("Unknown command. Use getiso8879 -h for help.");
//		} catch (IllegalArgumentException e) {
//			System.out.println(e);
//		}
//	}
//
//	private Options createOptions() {
//	    Options options = new Options();
//	    options.addOption("I", "input", true, "Input file path or directory (Tab file only).");
//	    options.addOption("O", "output", true, "Output directory path.");
//	    options.addOption("C", "configFile", true, "Config path (JSON only).");
//	    options.addOption("V", "stdout", false, "Print thru Standard output.");
//	    options.addOption("h", "help", false, "Print this help.");
//	    return options;
//	}
//	
//	private Options createOptionsValidate() {
//	    Options options = new Options();
//	    options.addRequiredOption("I", "input", true, "Input file path or directory (Tab file only).");
//	    options.addRequiredOption("O", "output", true, "Output directory path.");
//	    options.addRequiredOption("C", "configFile", true, "Config path (JSON only).");
//	    options.addOption("V", "map", false, "Print thru Standard output.");
//	    options.addOption("h", "help", false, "Print this help.");
//	    return options;
//	}
//	
//	private void printHelp(Options options, HelpFormatter helpFormatter) {
//		helpFormatter.printHelp("getiso8879", options);
//	}
//	
//}