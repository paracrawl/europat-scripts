package patentdata.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PatentOutputStream {
	OutputStream outputStreamTitle 				= null;
	OutputStream outputStreamAbstract 			= null;
	OutputStream outputStreamDescription		= null;
	OutputStream outputStreamClaims				= null;
	OutputStream outputStreamMetaData			= null;
	
	public PatentOutputStream(){
		
	}
	
	public PatentOutputStream(String sTargetPath, String sLang, String sYear) {
		try {
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
			e.printStackTrace();
		}
	}
	
	public void CloseBuffer() {
		try {
			outputStreamTitle.close();
			outputStreamAbstract.close();
			outputStreamDescription.close();
			outputStreamClaims.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
