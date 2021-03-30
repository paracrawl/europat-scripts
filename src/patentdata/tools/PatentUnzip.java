package patentdata.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

public class PatentUnzip extends Patent {


	public PatentUnzip() throws Exception {
		
	}

	public PatentUnzip(String path) throws Exception {
		super(path);
	}
	
	public PatentUnzip(String path, boolean bVerbose) throws Exception {
		super(path, bVerbose);
	}

    public void getDataCoverageOnDisk(File folderInput, File folderOutput, int year, int numberYears) throws Exception {
		if (0 == numberYears) {
			numberYears = 1;
		}

        PatentExtractEPO extractEPO = new PatentExtractEPO(printLog);

		for (int i = year; i < (year + numberYears); i++) {
			String sYearFilePath = folderInput + "/" + i;
			List<File> files = new ArrayList<File>();
			listf(sYearFilePath, files);
			try {
				extractEPO.readZipFileList(files, folderOutput, i+"");
			}catch (Exception e) {
				printLog.writeError("getDataCoverageOnDisk", e);
			}
		}
	}

	public void listf(String directoryName, List<File> files) {
		File directory = new File(directoryName);

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if (fList != null)
			for (File file : fList) {
				if (file.isFile()) {
					String tempName = file.getName();
					String tempExtension = "";
					if (tempName.lastIndexOf(".") > 0)
						tempExtension = tempName.substring(tempName.lastIndexOf(".")+1);
					if (tempName.length() > 4 && "EP".equalsIgnoreCase(tempName.substring(0,2)) && "zip".equalsIgnoreCase(tempExtension)) {
						files.add(file);
//						printLog.writeDebugLog(file.getPath());
					}
				} else if (file.isDirectory()) {
					listf(file.getAbsolutePath(), files);
				}
			}
	}
	
	
	public void getDataCoverageFront(File folderInput, File folderOutput, String suffixAbstract, String suffixClaim, String suffixDscp, boolean isMetadata ) throws Exception {

        PatentExtractEPO extractEPO = new PatentExtractEPO(printLog);

		String processingFilePath = folderInput.getAbsolutePath();
		if (!isMetadata) {
			processingFilePath = processingFilePath + "/" + "abstract_biblio/";
		}
		List<File> files = new ArrayList<File>();
		listfile(processingFilePath, files, "xml", false);
		try {
//			extractEPO.readZipFileList(files, folderOutput, i+"");
//			extractEPO.readFrontXML(folderInput, files, folderOutput, "abstract_biblio", "claims", "description");
			extractEPO.readFrontXML(folderInput, files, folderOutput, suffixAbstract, suffixClaim, suffixDscp, isMetadata);
		}catch (Exception e) {
			printLog.writeError("getDataCoverageFront", e);
		}
	}
	
	public void listfile(String directoryName, List<File> files, String extension, Boolean subdirectory) {
		File directory = new File(directoryName);

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if (fList != null)
			for (File file : fList) {
				if (file.isFile()) {
					String tempName = file.getName();
					String tempExtension = FilenameUtils.getExtension(tempName);
					if (tempName.length() > 4  // && ("EP".equalsIgnoreCase(tempName.substring(0, 2)) || "FR".equalsIgnoreCase(tempName.substring(0, 2))) 
							&& extension.equalsIgnoreCase(tempExtension)) {
						files.add(file);
//						printLog.writeDebugLog(file.getPath());
					}
				} else if ( subdirectory & file.isDirectory()) {
					listf(file.getAbsolutePath(), files);
				}
		}
		Pattern pt = Pattern.compile("[a-z0-9]+-([0-9]{4})-(.+?).xml", Pattern.CASE_INSENSITIVE);
		Collections.sort(files, (d1, d2) -> {
			String year1 = pt.matcher(d1.getName()).replaceAll("$1");
			String year2 = pt.matcher(d2.getName()).replaceAll("$1");
			int i1 = 0;
			int i2 = 0;
			if (null != year1)
				i1 = Integer.valueOf(year1);
			if (null != year2)
				i2 = Integer.valueOf(year2);
			return i2-i1;
		});
	}


}
