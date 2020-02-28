package patentdata.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

public class PatentManager {

	public static void main(String[] args) throws Exception {

		PatentManager oTool = new PatentManager();
		if (oTool.validateOption(args)) {
			String sOption = args[0];

			List<String> list = new ArrayList<String>(Arrays.asList(args));
			if (list.size() > 1) {
				list.remove(0);
				args = list.stream().toArray(String[]::new);
			}
			if ("-search".equalsIgnoreCase(sOption)) {
				new Search(args);
			} else if ("-extractid".equalsIgnoreCase(sOption)) {
				new ExtractId(args);
			} else if ("-getfamily".equalsIgnoreCase(sOption)) {
				new GetFamily(args);
			} else if ("-getpair".equalsIgnoreCase(sOption)) {
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
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println("1. -search or -extractid or -getfamily or -getpair (Required): Select method");
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

class Search {

	public Search(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String sDate = args[0];
				if (sDate == null || sDate.length() == 0)
					throw new Exception("YearMonth is required.");
				if (!sDate.matches("\\d{8}"))
					throw new Exception("YearMonth is should be in YYYYMMDD format.");

				new Patent().getPatentByDate(sDate);

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
				System.out.println("1. <Date> (Required): Year and Month to search patents (YYYYMMDD)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent.jar -search \"201912\"");
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

class ExtractId {

	public ExtractId(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String sOptionI = args[0];
				if (sOptionI == null || sOptionI.length() == 0 || !"-I".equals(sOptionI))
					throw new Exception("Option should be \"-I\"");
				String sInputDir = args[1];
				if (sInputDir == null || sInputDir.length() == 0)
					throw new Exception("InputDir is required.");
				String sOptionO = args[2];
				if (sOptionO == null || sOptionO.length() == 0 || !"-O".equals(sOptionO))
					throw new Exception("Option should be \"-O\"");
				String sOutputDir = args[3];
				if (sOutputDir == null || sOutputDir.length() == 0)
					throw new Exception("OutputDir is required.");

				File folderInput = new File(sInputDir);
				if (!folderInput.exists() || !folderInput.isDirectory())
					throw new Exception("InputDir is not exist or not a directory.");

				new Patent().getPatentIds(folderInput, new File(sOutputDir));

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
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent.jar -extractid -I \"input/directory\" -O \"output/directory\"");
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

class GetFamily {

	public GetFamily(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String sOption = args[0];
				if (sOption == null || sOption.length() == 0)
					throw new Exception("Option should be \"-I\" or \"-D\"");
				String str = args[1];
				if (str == null || str.length() == 0)
					throw new Exception("InputPath or DocId is required.");

				if ("-I".equals(sOption)) {
					File fileInput = new File(str);
					if (!fileInput.exists())
						throw new Exception("InputPath is not exist.");

					new Patent().getFamily(fileInput);
				} else if ("-D".equals(sOption)) {

					new Patent().getFamily(str);
				} else {
					throw new Exception("Option should be \"-I\" or \"-D\"");
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
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent.jar -getfamily -I \"input/path/ids_201912.txt\"");
				System.out.println("java -jar patent.jar -getfamily -D \"JP.H07196059.A\"");
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

class GetPair {

	public GetPair(String[] args) throws Exception {
		try {
			if (validateMandatory(args)) {
				String sSourceCountry = args[0];
				if (sSourceCountry == null || sSourceCountry.length() == 0)
					throw new Exception("SourceCountry is required.");
				String sTargetCountry = args[1];
				if (sTargetCountry == null || sTargetCountry.length() == 0)
					throw new Exception("TargetCountry is required.");
				String sOption = args[2];
				if (sOption == null || sOption.length() == 0 || !"-O".equals(sOption))
					throw new Exception("Option should be \"-O\"");
				String sOutputPath = args[3];
				if (sOutputPath == null || sOutputPath.length() == 0)
					throw new Exception("OutputPath is required.");

				File fileOutput = new File(sOutputPath);
				if (!"txt".equalsIgnoreCase(FilenameUtils.getExtension(fileOutput.toString())))
					throw new Exception("OutputPath is not a text file.");

				new Patent().getPair(sSourceCountry, sTargetCountry, fileOutput);

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
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent.jar -getpair \"TW\" \"US\" -O \"input/path/pair_result.txt\"");
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
