package patentdata.tools;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

public class PatentManager {

	public static void main(String[] args) throws Exception {

	}

}

class Search {

	public static void main(String[] args) throws Exception {
		Search oTool = new Search();
		try {
			if (oTool.validateMandatory(args)) {
				String sYearMonth = args[0];
				if (sYearMonth == null || sYearMonth.length() == 0)
					throw new Exception("YearMonth is required.");
				if (!sYearMonth.matches("\\d{6}"))
					throw new Exception("YearMonth is should be in YYYYMM format.");

				new Patent().getPatentByMonth(sYearMonth);

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
				System.out.println("1. <YearMonth> (Required): Year and Month to search patents (YYYYMM)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent-search.jar \"201912\"");
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

	public static void main(String[] args) throws Exception {
		ExtractId oTool = new ExtractId();
		try {
			if (oTool.validateMandatory(args)) {
				String sIcmd = args[0];
				if (sIcmd == null || sIcmd.length() == 0 || !"-I".equals(sIcmd))
					throw new Exception("Option should be \"-I\"");
				String sInputPath = args[1];
				if (sInputPath == null || sInputPath.length() == 0)
					throw new Exception("InputDir is required.");
				String sOcmd = args[2];
				if (sOcmd == null || sOcmd.length() == 0 || !"-O".equals(sOcmd))
					throw new Exception("Option should be \"-O\"");
				String sOutputDir = args[3];
				if (sOutputDir == null || sOutputDir.length() == 0)
					throw new Exception("OutputDir is required.");

				File folderInput = new File(sInputPath);
				if (!folderInput.exists() || !folderInput.isDirectory())
					throw new Exception("InputDir is not exist or not a directory.");
				File folderOutput = new File(sOutputDir);
				if (!folderOutput.exists() || !folderOutput.isDirectory())
					throw new Exception("OutputDir is not exist or not a directory.");

				new Patent().getPatentIds(folderInput, folderOutput);

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
				System.out.println("1. -I <InputDir> (Required): Input folder");
				System.out.println("2. -O <OutputDir> (Required): Output folder");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println(
						"java -jar patent-extractids.jar -I \"input/directory\" -O \"output/directory\"");
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

	public static void main(String[] args) throws Exception {
		GetFamily oTool = new GetFamily();
		try {
			if (oTool.validateMandatory(args)) {
				String sCmd = args[0];
				if (sCmd == null || sCmd.length() == 0)
					throw new Exception("Option should be \"-I\" or \"-D\"");
				String str = args[1];
				if (str == null || str.length() == 0)
					throw new Exception("InputPath or DocId is required.");

				if ("-I".equals(sCmd)) {
					File fileInput = new File(str);
					if (!fileInput.exists())
						throw new Exception("InputPath is not exist.");

					new Patent().getFamily(fileInput);
				} else if ("-D".equals(sCmd)) {

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
			if (args == null || args.length == 0 || args.length < 1 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println(
						"1. -I <InputPath> or -D <DocId> (Required): Input file path or folder (TXT only) or Document Id (CountryCode.DocNo.KindCode)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent-getfamily.jar -I \"input/path/ids_201912.txt\"");
				System.out.println("java -jar patent-getfamily.jar -D \"JP.H07196059.A\"");
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

	public static void main(String[] args) throws Exception {
		GetPair oTool = new GetPair();
		try {
			if (oTool.validateMandatory(args)) {
				String sSourceCountry = args[0];
				if (sSourceCountry == null || sSourceCountry.length() == 0)
					throw new Exception("SourceCountry is required.");
				String sTargetCountry = args[1];
				if (sTargetCountry == null || sTargetCountry.length() == 0)
					throw new Exception("TargetCountry is required.");
				String sCmd = args[2];
				if (sCmd == null || sCmd.length() == 0 || !"-O".equals(sCmd))
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
			if (args == null || args.length == 0 || args.length < 1 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println("1. <SourceCountry> (Required): Source Country");
				System.out.println("2. <TargetCountry> (Required): Target Country");
				System.out.println("3. -O <OutputPath> (Required): Output file path (TXT only)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println("java -jar patent-getpair.jar \"TW\" \"US\" -O \"input/path/pair_result.txt\"");
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
