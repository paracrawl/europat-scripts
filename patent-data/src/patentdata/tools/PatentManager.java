package patentdata.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class PatentManager {

	public static void main(String[] args) throws Exception {

		PatentManager oTool = new PatentManager();
		if (oTool.validateOption(args)) {
			String line = String.join(" ", args);
			List<String> list = oTool.listStringPattern("(?i)-(search|extractid|getfamily|getpair)", line);
			if (list.size() > 1)
				throw new Exception("Select method: -search or -extractid or -getfamily or -getpair");
			String method = (String) list.get(0);
			line = line.replace("-" + method, "").trim();
			args = line.split(" ");
			if ("search".equalsIgnoreCase(method)) {
				new Search(args);
			} else if ("extractid".equalsIgnoreCase(method)) {
				new ExtractId(args);
			} else if ("getfamily".equalsIgnoreCase(method)) {
				new GetFamily(args);
			} else if ("getpair".equalsIgnoreCase(method)) {
				new GetPair(args);
			} else {
				throw new Exception("Option should be \"-search\" or \"-extractid\" or \"-getfamily\" or \"-getpair\"");
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
			System.exit(0);
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
				System.out.println("2. -C <ConfigPath> (Optional): Config path");
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
			System.exit(0);
			return false;
		}
	}

}

class ExtractId extends PatentManager {

	public ExtractId(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String line = String.join(" ", args);

				String sInputDir = getOptionValue("I", line);
				if (sInputDir == null || sInputDir.length() == 0)
					throw new Exception("Required: -I <InputDir>");

				String sOutputDir = getOptionValue("O", line);
				if (sOutputDir == null || sOutputDir.length() == 0)
					throw new Exception("Required: -O <OutputDir>");

				File folderInput = new File(sInputDir);
				if (!folderInput.exists() || !folderInput.isDirectory())
					throw new Exception("InputDir is not exist or not a directory.");

				new Patent(getOptionConfig(line)).getPatentIds(folderInput, new File(sOutputDir));

			}
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private boolean validateMandatory(String[] args) {
		try {
			if (args == null || args.length == 0 || args.length < 4 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println("1. -I <InputDir> (Required): Input folder");
				System.out.println("2. -O <OutputDir> (Required): Output folder");
				System.out.println("3. -C <ConfigPath> (Optional): Config path");
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

				if (line.matches(".*(\\-I).*")) {
					File fileInput = new File(getOptionValue("I", line));
					if (!fileInput.exists())
						throw new Exception("InputPath is not exist.");

					new Patent(sConfigPath).getFamily(fileInput);
				} else if (line.matches(".*(\\-D).*")) {
					new Patent(sConfigPath).getFamily(getOptionValue("D", line));
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
				System.out.println("2. -C <ConfigPath> (Optional): Config path");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println(
						"java -jar patent.jar -getfamily -I \"input/path/ids_201912.txt\" -C \"config/path/patent.json\"");
				System.out
						.println("java -jar patent.jar -getfamily -D \"JP.H07196059.A\" -C \"config/path/patent.json\"");
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

class GetPair extends PatentManager {

	public GetPair(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String line = String.join(" ", args);
				String sConfigPath = getOptionConfig(line);
				line = line.replace("-C " + sConfigPath, "").trim();

				String sOutputPath = getOptionValue("O", line);
				if (sOutputPath == null || sOutputPath.length() == 0)
					throw new Exception("Required: -O <OutputPath>");
				File fileOutput = new File(sOutputPath);
				if (!"txt".equalsIgnoreCase(FilenameUtils.getExtension(fileOutput.toString())))
					throw new Exception("OutputPath is not a text file.");
				line = line.replace("-O " + sOutputPath, "").trim();

				args = line.split(" ");
				String sSourceCountry = args[0];
				if (sSourceCountry == null || sSourceCountry.length() == 0)
					throw new Exception("SourceCountry is required.");
				String sTargetCountry = args[1];
				if (sTargetCountry == null || sTargetCountry.length() == 0)
					throw new Exception("TargetCountry is required.");

				new Patent(sConfigPath).getPair(sSourceCountry, sTargetCountry, fileOutput);
			}
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private boolean validateMandatory(String[] args) {
		try {
			if (args == null || args.length == 0 || args.length < 4 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println("1. <SourceCountry> (Required): Source Country");
				System.out.println("2. <TargetCountry> (Required): Target Country");
				System.out.println("3. -O <OutputPath> (Required): Output file path (TXT only)");
				System.out.println("4. -C <ConfigPath> (Optional): Config path");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println(
						"java -jar patent.jar -getpair \"TW\" \"US\" -O \"input/path/pair_result.txt\" -C \"config/path/conf.json\"");
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
