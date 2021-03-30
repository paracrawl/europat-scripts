package patentdata.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.logging.Log;

public class PatentOutputStream {
	OutputStream outputStreamTitle 				= null;
	OutputStream outputStreamAbstract 			= null;
	OutputStream outputStreamDescription		= null;
	OutputStream outputStreamClaims				= null;
	OutputStream outputStreamMetaData			= null;
	
	LogFourJ printlog = null;
	public PatentOutputStream(){
		
	}
	
	public PatentOutputStream(String sTargetPath, String sLang, String sYear, LogFourJ printlog) {
		creatBuffer(sTargetPath, sLang, sYear, printlog, true, true, true, true, true);
	}
	
	public PatentOutputStream(String sTargetPath, String sLang, String sYear, LogFourJ printlog, boolean isTitle, boolean ismetadata, boolean isAbstact, boolean isclaim, boolean isdesc) {
		creatBuffer(sTargetPath, sLang, sYear, printlog, isTitle, ismetadata, isAbstact, isclaim, isdesc);
	}
	
	public PatentOutputStream(String sTargetPath, String sLang, String sYear, LogFourJ printlog, List<String> filetype) {
		try {
			this.printlog = printlog;
			String sTitlePath 		= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Title.tab";
			String sAbstractPath 	= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Abstract.tab";
			String sDescriptionPath = sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Description.tab";
			String sClaimsPath 		= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Claim.tab";
			String sMetadataPath 	= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Metadata.tab";
			
			File file = new File(sTitlePath);
			if (!file.exists())
				file.getParentFile().mkdirs();
			
			file = new File(sAbstractPath);
			if (!file.exists())
				file.getParentFile().mkdirs();
			
			file = new File(sDescriptionPath);
			if (!file.exists())
				file.getParentFile().mkdirs();
			
			file = new File(sClaimsPath);
			if (!file.exists())
				file.getParentFile().mkdirs();
			
			file = new File(sMetadataPath);
			if (!file.exists())
				file.getParentFile().mkdirs();
			
			outputStreamTitle 		= new FileOutputStream(sTitlePath, true);
			outputStreamAbstract 	= new FileOutputStream(sAbstractPath, true);
			outputStreamDescription = new FileOutputStream(sDescriptionPath, true);
			outputStreamClaims 		= new FileOutputStream(sClaimsPath, true);
			outputStreamMetaData 	= new FileOutputStream(sMetadataPath, true);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			printlog.writeError("PatentOutputStream", e);
		}
	}
	
	private void creatBuffer(String sTargetPath, String sLang, String sYear, LogFourJ printlog, boolean isTitle, boolean ismetadata, boolean isAbstact, boolean isclaim, boolean isdesc) {
		try {
			this.printlog = printlog;
			String sTitlePath 		= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Title.tab";
			String sAbstractPath 	= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Abstract.tab";
			String sDescriptionPath = sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Description.tab";
			String sClaimsPath 		= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Claim.tab";
			String sMetadataPath 	= sTargetPath + "/" + sLang.toUpperCase() + "-" + sYear + "-Metadata.tab";
			
			File file = null;
			
			if (isTitle) {
				file = new File(sTitlePath);
				if (!file.exists())
					file.getParentFile().mkdirs();
				outputStreamTitle 		= new FileOutputStream(sTitlePath, true);
				
			}
			
			if (isAbstact) {
				file = new File(sAbstractPath);
				if (!file.exists())
					file.getParentFile().mkdirs();
				outputStreamAbstract 	= new FileOutputStream(sAbstractPath, true);
				
			}
			
			if (isdesc) {
				file = new File(sDescriptionPath);
				if (!file.exists())
					file.getParentFile().mkdirs();
				outputStreamDescription = new FileOutputStream(sDescriptionPath, true);
				
			}
			
			if (isclaim) {
				file = new File(sClaimsPath);
				if (!file.exists())
					file.getParentFile().mkdirs();
				outputStreamClaims 		= new FileOutputStream(sClaimsPath, true);
			}	
			
			if(ismetadata) {
				file = new File(sMetadataPath);
				if (!file.exists())
					file.getParentFile().mkdirs();
				outputStreamMetaData 	= new FileOutputStream(sMetadataPath, true);

			}
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			printlog.writeError("PatentOutputStream", e);
		}
	}
	
	public void CloseBuffer() {
		try {
			if (null != outputStreamTitle)
				outputStreamTitle.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printlog.writeError("PatentOutputStream:CloseBuffer", e);
		}
		
		try {
			if (null != outputStreamAbstract)
				outputStreamAbstract.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printlog.writeError("PatentOutputStream:CloseBuffer", e);
		}
		
		try {
			if (null != outputStreamDescription)
				outputStreamDescription.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printlog.writeError("PatentOutputStream:CloseBuffer", e);
		}
		
		try {
			if (null != outputStreamClaims)
			outputStreamClaims.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			printlog.writeError("PatentOutputStream:CloseBuffer", e);
		}
		
		outputStreamTitle 		= null;
		outputStreamAbstract 	= null;
		outputStreamDescription = null;
		outputStreamClaims 		= null;
		outputStreamMetaData 	= null;
	}

	public OutputStream getOutputStreamTitle() {
		return outputStreamTitle;
	}

	public void setOutputStreamTitle(OutputStream outputStreamTitle) {
		this.outputStreamTitle = outputStreamTitle;
	}

	public OutputStream getOutputStreamAbstract() {
		return outputStreamAbstract;
	}

	public void setOutputStreamAbstract(OutputStream outputStreamAbstract) {
		this.outputStreamAbstract = outputStreamAbstract;
	}

	public OutputStream getOutputStreamDescription() {
		return outputStreamDescription;
	}

	public void setOutputStreamDescription(OutputStream outputStreamDescription) {
		this.outputStreamDescription = outputStreamDescription;
	}

	public OutputStream getOutputStreamClaims() {
		return outputStreamClaims;
	}

	public void setOutputStreamClaims(OutputStream outputStreamClaims) {
		this.outputStreamClaims = outputStreamClaims;
	}

	public OutputStream getOutputStreamMetaData() {
		return outputStreamMetaData;
	}

	public void setOutputStreamMetaData(OutputStream outputStreamMetaData) {
		this.outputStreamMetaData = outputStreamMetaData;
	}
}
