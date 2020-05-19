package patentdata.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PatentExtract extends Patent {

	public PatentExtract() throws Exception {
	}

	public PatentExtract(String path) throws Exception {
		super(path);
	}

	public static void main(String[] args) throws Exception {
		String line = String.join(" ", args);
		String sConfigPath = new PatentExtract().getOptionConfig(line);
		PatentExtract oTool = new PatentExtract(sConfigPath);
		try {
			if (oTool.validateMandatory(args)) {
				String sInputPath = oTool.getOptionValue("I", line);
				if (sInputPath == null || sInputPath.length() == 0)
					throw new Exception("Required: -I <InputPath>");

				String sOutputPath = oTool.getOptionValue("O", line);
				if (sOutputPath == null || sOutputPath.length() == 0)
					throw new Exception("Required: -O <OutputPath>");

				File fileInput = new File(sInputPath);
				if (!fileInput.exists())
					throw new Exception("InputPath is not exist.");

				File fileOutput = new File(sOutputPath);

				oTool.extract(fileInput, fileOutput);
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
				System.out.println("1. -I <InputPath> (Required): Input path or folder (TAB only)");
				System.out.println("2. -O <OutputFolder> (Required): Output folder");
				System.out.println("3. -C <ConfigPath> (Optional): Config path (JSON only)");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println(
						"java -jar extract-patent.jar -I \"input/directory\" -O \"output/path/result.txt\" -C \"config/path/patent.json\"");
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

	private void extract(File filein, File fileout) {
		String line = null;
		try {
			File folderOutput = fileout.isDirectory() ? fileout : fileout.getParentFile();
			if (null != folderOutput && !folderOutput.exists())
				folderOutput.mkdirs();
		} catch (Exception e) {
		}

		File[] files = filein.listFiles(File::isDirectory);
		if (files == null || files.length == 0)
			files = new File[] { filein };
		for (File file : files) {
			String[] sources = file.isFile() ? new String[] { file.toString() }
					: common.getFiles(file.toString(), new String[] { "*.txt" }, false);
			for (String inputPath : sources) {
				System.out.println(String.format("reading... %s", inputPath));
				File fileInput = new File(inputPath);
				try (BufferedReader br = new BufferedReader(new FileReader(fileInput))) {
					while ((line = br.readLine()) != null) {
						String[] arr = line.split("\t");
						if (arr.length > 0) {
							StringBuilder content = new StringBuilder().append(arr[0]).append("\t").append(arr[1])
									.append("\t").append(arr[2]).append("\t").append(arr[3]).append("\t").append(arr[4])
									.append("\t").append(arr[5])
									.append(("TITLE".equalsIgnoreCase(arr[5]) ? "\t" + arr[6] : "")).append("\n");
							// System.out.println(content);
							common.WriteFile(fileout.toString(), content.toString(), true);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}
