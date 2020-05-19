package patentdata.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.Charsets;

import patentdata.utils.ConstantsVal;
import patentdata.utils.DocumentVO;
import patentdata.utils.Log;
import patentdata.utils.PatentDocumentVO;
import patentdata.utils.PatentOutputStream;




public class PatentExtractEPO {

	DocumentVO _oDocumentVO 					= null;
	protected HashMap<String, PatentOutputStream> _mapPatentOutputStream = new HashMap<>();
	protected Log log = null;
	
	PatentExtractEPO(Log log){
		this.log = log;
	}
	
	private void CloseBuffer() {
		  for (Entry<String, PatentOutputStream> entry : _mapPatentOutputStream.entrySet()) {
			  PatentOutputStream outputStream = entry.getValue();
			  outputStream.CloseBuffer();
	       }
	}
	
	public void readZipFileList(File oFolderInput, File oFolderOutPut, int weekNumber, List<PatentDocumentVO> lPatentDocumentVoList) throws Exception {
		int iSize = lPatentDocumentVoList.size();
		PatentDocumentVO oPatentDocumentVO = null;
		try {
			for (int i = 0; i < iSize; i++) {
				oPatentDocumentVO = lPatentDocumentVoList.get(i);
				
				try {
					readZipFile(oFolderInput, oFolderOutPut, oPatentDocumentVO, weekNumber);
				}catch (Exception e) {
					// TODO: handle exception
					log.printErr(e);
				}
			}
		}finally {
			CloseBuffer();
		}
		
	}
	
	public void readZipFile(File oFolderInput, File oFolderOutPut, PatentDocumentVO oPatentDocumentVO, int weekNumber) throws Exception {
		//define zip name and zip location 
		String sYear = oPatentDocumentVO.getPublDocumentVO().getsDatePubl().substring(0, 4);
		String sKind = oPatentDocumentVO.getPublDocumentVO().getSkind();
		String sCountry = oPatentDocumentVO.getApplDocumentVO().getsCountry();
		String sFoldername = "EPRTBJV" + sYear + String.format("%06d", weekNumber) + "001001";
		File folderSource = new File(oFolderInput, sYear+"/"+sFoldername);
		
		//set default
		String sKey = "NW";
		if ("B8".equals(sKind) || "B9".equals(sKind) || "A8".equals(sKind) ||  "A9".equals(sKind)) {
			sKey = "W1";
		}
		String sFilename = sCountry + oPatentDocumentVO.getApplDocumentVO().getsDocNumber() + sKey + sKind + ".zip";
		String sFileLocation = "DOC/" + sCountry + sKey + sKind;
		
		
		sFileLocation = sFileLocation.replace("\\", "/");
		folderSource = new File(folderSource, sFileLocation);
		File f = new File(folderSource, sFilename);

		if (null != oFolderOutPut && !oFolderOutPut.exists()) {
			oFolderOutPut.mkdirs();
		}
		
		ExtractZipFile(f.getPath(), oFolderOutPut, sFilename, sYear);
		
	}
	
	private void ExtractZipFile(String sFilePath, File oFolderOutPut, String sFileName, String sYear) throws Exception {
		 log.print("Start Process : " + sFileName);
		 InputStream inputStream = null;
	     ZipArchiveInputStream zip = null;
	     StringBuffer stringBuffer = null; 
	     sFileName = sFileName.replace(".zip", ".xml");
			try {
				inputStream = new FileInputStream(sFilePath);
				zip = new ZipArchiveInputStream(inputStream);
			    ZipArchiveEntry entry = null;
			    stringBuffer = new StringBuffer();
			    while ((entry = zip.getNextZipEntry()) != null) {
			        if (sFileName.equalsIgnoreCase(entry.getName())) {
		                String UTF8 = "utf8";
		                int BUFFER_SIZE = 8192;
		                BufferedReader br = new BufferedReader(new InputStreamReader(zip, UTF8), BUFFER_SIZE);
		                String str;
		                while ((str = br.readLine()) != null) {
		                	stringBuffer.append(str);
		                }

		                String sDocSource = getDocSource(entry.getName(), stringBuffer.toString());
		                String sText = initializeText(sDocSource, stringBuffer.toString());
		                String sKey = _oDocumentVO.getsLang() + "-" + sYear;
		                PatentOutputStream oPatentOutputStream = (PatentOutputStream) _mapPatentOutputStream.get(sKey);
		                if (null == oPatentOutputStream) {
		                	oPatentOutputStream = new PatentOutputStream(oFolderOutPut.getPath(),   _oDocumentVO.getsLang(), sYear);
		                	_mapPatentOutputStream.put(sKey, oPatentOutputStream);
		                }
		                WriteToFile(sText, ConstantsVal.StartTitle, ConstantsVal.EndTitle, oPatentOutputStream.getOutputStreamTitle());
		                WriteToFile(sText, ConstantsVal.StartAbstract, ConstantsVal.EndAbstract, oPatentOutputStream.getOutputStreamAbstract());
		                WriteToFile(sText, ConstantsVal.StartDscp, ConstantsVal.EndDscp, oPatentOutputStream.getOutputStreamDescription());
		                WriteToFile(sText, ConstantsVal.StartClaims, ConstantsVal.EndClaims, oPatentOutputStream.getOutputStreamClaims());
		                break;
			        }
			    }
			} catch ( Exception e) {
				// TODO Auto-generated catch block
				log.printErr(e);
			} finally {
			    try {
					zip.close();
					inputStream.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					log.printErr(e);
				}
			}
			log.print("End Process : " + sFileName);
	}
	
	protected String getDocSource(String sFileName, String sText) {
		String sDocSource = "UNKNOWN"; //Returned when no DTD name matches or invalid file type
	    if (sFileName.toLowerCase().lastIndexOf(".txt") > 0) {
	        //If txt, then assume that the format is the old USPTO Ansi format.
	        sDocSource = "us-patent-grant-ansi";
	    }
	    else {
	        if (sFileName.toLowerCase().lastIndexOf(".xml") > 0 ) {
	            //Check to see if it is a known DTD
	    		Pattern pDtd = Pattern.compile("ep-patent-document-v1-1.dtd|ep-patent-document-v1-5.dtd",
	    				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	        	Matcher oMatches = pDtd.matcher(sText);
	            if (oMatches.find()) {
	                String  sDocMatch = oMatches.group(0);
	                sDocSource = sDocMatch.replace(".dtd", "").toLowerCase();
	            }
	        }
	    }
	    return sDocSource;
	}
	
	private String GetEpPtuDocment(String sText) {
		String sLang = "";
		Pattern pLang = Pattern.compile(	"(<ep-patent-document [^<>]*>)",
				Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	
		Matcher mTarget = pLang.matcher(sText);
		while (mTarget.find()) {
			sLang = mTarget.group(0) == null ? "" : mTarget.group(0);
			break;
		}				
			
		return sLang;
	}
	
	private String initializeText(String sDocSource, String sText) throws Exception {
	    sText = sText.replace("/\\r\\n|\\n/gm", "EPBREP");
	    
	    if ("ep-patent-document-v1-1".equals(sDocSource) || "ep-patent-document-v1-5".equals(sDocSource) ){
	        	
	        	_oDocumentVO = GetPatentInformationXML(sText);

	        	sText = sText.replaceAll("(<B541[^<>]*>" +_oDocumentVO.getsLang()+"<\\/B541[^<>]*><B542[^<>]*>)", "\n" + ConstantsVal.StartTitle);
	            sText = sText.replaceAll("(<\\/B542[^<>]*>)", ConstantsVal.EndTitle+"\n");
	            
	            sText = sText.replaceAll("(<abstract [^<>]* lang=\""+_oDocumentVO.getsLang()+"\">)", "\n" + ConstantsVal.StartAbstract);
	            sText = sText.replaceAll("(<\\/abstract[^<>]*>)", ConstantsVal.EndAbstract + "\n");
	            
	            sText = sText.replaceAll("(<description [^<>]* lang=\""+_oDocumentVO.getsLang()+"\">)", "\n" + ConstantsVal.StartDscp);
	            sText = sText.replaceAll("(<\\/description[^<>]*>)", ConstantsVal.EndDscp + "\n");
	            
	            sText = sText.replaceAll("(<claims [^<>]* lang=\""+_oDocumentVO.getsLang()+"\">)", "\n" + ConstantsVal.StartClaims);
	            sText = sText.replaceAll("(<\\/claims[^<>]*>)", ConstantsVal.EndClaims + "\n");

	    }
	    return sText;
	}
	
	private void WriteToFile(String sText, String sStart, String sEnd, OutputStream oOutputStream) throws Exception {
		String sWrite = "";
		int iStartIndex = sText.indexOf(sStart);
		int iEndIndex = 0;
		if (iStartIndex > 0 ) {
			iEndIndex = sText.substring(iStartIndex).indexOf(sEnd);
			sWrite = sText.substring(iStartIndex + sStart.length(), iEndIndex + iStartIndex);
		}
		sWrite = _oDocumentVO.getsPatentId() + "\t" + _oDocumentVO.getsDatePubl() + "\t"  + sWrite +"\n";
		
		try {
			oOutputStream.write(sWrite.getBytes(Charsets.toCharset("utf-8")));
		} catch (IOException e) {
			log.printErr(e);
		}
	}
	
	public DocumentVO GetPatentInformationXML(String sText) throws Exception {
		DocumentVO oDocInfo = new DocumentVO();
		XMLEventReader eventReader = null;
		try {
			String sEpPatenDoc = GetEpPtuDocment(sText);
			if (null != sEpPatenDoc && sEpPatenDoc.trim().length() > 0) {
				sEpPatenDoc = sEpPatenDoc + "</ep-patent-document>";
			}

			eventReader = XMLInputFactory.newInstance()
					.createXMLEventReader(new ByteArrayInputStream(sEpPatenDoc.getBytes()));
			// read the XML document
			XMLEvent event = null;
			while (eventReader.hasNext()) {
					event = eventReader.nextEvent();
					if (event.isStartElement()) {
						String localPart = event.asStartElement().getName().getLocalPart();
						StartElement startElement = event.asStartElement();
	
						switch (localPart) {
						// case there is tag "ep-patent-document", read attribute
						case "ep-patent-document":
							Iterator<Attribute> attributes = startElement.getAttributes();
							while (attributes.hasNext()) {
								Attribute attribute = attributes.next();
								if (attribute.getName().toString().equals("id")) {
									oDocInfo.setsId(attribute.getValue());
								}else if (attribute.getName().toString().equals("file")) {
									oDocInfo.setsFileName(attribute.getValue());
								}else if (attribute.getName().toString().equals("lang")) {
									oDocInfo.setsLang(attribute.getValue());
								}else if (attribute.getName().toString().equals("country")) {
									oDocInfo.setsCountry(attribute.getValue());
								}else if (attribute.getName().toString().equals("doc-number")) {
									oDocInfo.setsDocNumber(attribute.getValue());
								}else if (attribute.getName().toString().equals("kind")) {
									oDocInfo.setSkind(attribute.getValue());
								}else if (attribute.getName().toString().equals("date-publ")) {
									oDocInfo.setsDatePubl(attribute.getValue());
								}else if (attribute.getName().toString().equals("dtd-version")) {
									oDocInfo.setsDtdVersion(attribute.getValue());
								}
							}
	
							break;
						}
					}
				}
		}catch (Exception e) {
			log.printErr(e);
		}finally {
			try {
				eventReader.close();
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				log.printErr(e);
			}
		}
		return oDocInfo;
	}
	
	
}

