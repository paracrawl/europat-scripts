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
import java.util.Map;
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
import org.apache.commons.lang3.StringUtils;

import patentdata.utils.ConstantsVal;
import patentdata.utils.DocumentVO;
import patentdata.utils.LogFourJ;
import patentdata.utils.PatentOutputStream;




public class PatentExtractEPO {

	DocumentVO _oDocumentVO 					= null;
	protected HashMap<String, PatentOutputStream> _mapPatentOutputStream = new HashMap<>();
	protected LogFourJ printLog = null;
	
	private PatentExtractEPO(){
	}
	
	PatentExtractEPO(LogFourJ printLog){
		this.printLog = printLog;
	}
	
	private void CloseBuffer() {
		  for (Entry<String, PatentOutputStream> entry : _mapPatentOutputStream.entrySet()) {
//			  printLog.writeDebugLog("CloseBuffer:" + entry.getKey());
			  PatentOutputStream outputStream = entry.getValue();
			  outputStream.CloseBuffer();
	       }
		  _mapPatentOutputStream = new HashMap<>();
	}
	
	public void readZipFileList(List<File> files, File oFolderOutPut, String sYear) throws Exception {
		int iSize = files.size();
		try {
			for (int i = 0; i < iSize; i++) {
				try {
					File tempFile = files.get(i);
					ExtractZipFile(tempFile.getPath(), oFolderOutPut, tempFile.getName(), sYear);
					
					if (i%10000 == 0) {
						CloseBuffer();
					}
				}catch (Exception e) {
					printLog.writeError("readZipFileList", e);
				}
			}
		}finally {
			CloseBuffer();
		}
		
	}

	private void ExtractZipFile(String sFilePath, File oFolderOutPut, String sFileName, String sYear) throws Exception {
		 printLog.writeDebugLog("\tStart Process : " + sFileName);
		 InputStream inputStream = null;
	     ZipArchiveInputStream zip = null;
	     StringBuffer stringBuffer = null; 
	     BufferedReader br = null;
	     sFileName = sFileName.replace(".zip", ".xml");
			try {
				inputStream = new FileInputStream(sFilePath);
				zip = new ZipArchiveInputStream(inputStream);
			    ZipArchiveEntry entry = null;
			    stringBuffer = new StringBuffer();
			    while ((entry = zip.getNextZipEntry()) != null) {
			        if (sFileName.equalsIgnoreCase(entry.getName())) {
		                String UTF8 = "utf-8";
		                br = new BufferedReader(new InputStreamReader(zip, UTF8));
		                String str;
		                while ((str = br.readLine()) != null) {
		                	stringBuffer.append(str);
		                }

		                String sDocSource = getDocSource(entry.getName(), stringBuffer.toString());
		                printLog.writeDebugLog("\tDTD Version::" + sDocSource);
		                String sText = initializeText(sDocSource, stringBuffer.toString());
		                String sKey = _oDocumentVO.getsLang() + "-" + sYear;
		                PatentOutputStream oPatentOutputStream = (PatentOutputStream) _mapPatentOutputStream.get(sKey);
		                if (null == oPatentOutputStream) {
		                	oPatentOutputStream = new PatentOutputStream(oFolderOutPut.getPath(),   _oDocumentVO.getsLang(), sYear, printLog);
		                	_mapPatentOutputStream.put(sKey, oPatentOutputStream);
		                }
		                WriteToFile(sText, ConstantsVal.StartTitle, ConstantsVal.EndTitle, oPatentOutputStream.getOutputStreamTitle());
		                WriteToFile(sText, ConstantsVal.StartAbstract, ConstantsVal.EndAbstract, oPatentOutputStream.getOutputStreamAbstract());
		                WriteToFile(sText, ConstantsVal.StartDscp, ConstantsVal.EndDscp, oPatentOutputStream.getOutputStreamDescription());
		                WriteToFile(sText, ConstantsVal.StartClaims, ConstantsVal.EndClaims, oPatentOutputStream.getOutputStreamClaims());
		                WriteMetaData(sText, ConstantsVal.StartMetadata, ConstantsVal.EndMetadata, oPatentOutputStream.getOutputStreamMetaData());
		                break;
			        }
			    }
			} catch ( Exception e) {
				// TODO Auto-generated catch block
				printLog.writeError("ExtractZipFile", e);
			} finally {
			    try {
					zip.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					printLog.writeError("ExtractZipFile", e);
				}
			    
			    try {
			    	inputStream.close();
			    }catch (Exception e) {
			    	printLog.writeError("ExtractZipFile", e);
				}
			    
			    try {
			    	br.close();
			    }catch (Exception e) {
			    	printLog.writeError("ExtractZipFile", e);
				}
			}
			printLog.writeDebugLog("\tEnd Process : " + sFileName);
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
	    		Pattern pDtd = Pattern.compile("ep-patent-document-v1-0.dtd|ep-patent-document-v1-1.dtd|ep-patent-document-v1-2.dtd|ep-patent-document-v1-3.dtd|ep-patent-document-v1-4.dtd|ep-patent-document-v1-5.dtd|ops:world-patent-data",
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
	    sText = sText.replaceAll("/\\r\\n|\\n/gm", "EPBREP");
	    
	    if ("ep-patent-document-v1-0".equals(sDocSource) || "ep-patent-document-v1-1".equals(sDocSource) || "ep-patent-document-v1-2".equals(sDocSource)
	    		|| "ep-patent-document-v1-3".equals(sDocSource) || "ep-patent-document-v1-4".equals(sDocSource)
	    		|| "ep-patent-document-v1-5".equals(sDocSource)){
	        	
	        	_oDocumentVO = GetPatentInformationXML(sText);

	        	sText = sText.replaceAll("(<SDOBI[^<>]*>)", "\n" + ConstantsVal.StartMetadata);
	            sText = sText.replaceAll("(<\\/SDOBI[^<>]*>)", ConstantsVal.EndMetadata+"\n");
	            
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
	
	private void WriteMetaData(String sText, String sStart, String sEnd, OutputStream oOutputStream) throws Exception {
		String sWrite = "";
		String sMetadata = "";
		String sApplications = "";
		String sInventors = "";
		String sIPC = "";
		String sPriority = "";
		String sTemp = "";
		String sDateApplication = "";
		int iStartIndex = sText.indexOf(sStart);
		int iEndIndex = 0;
		
		if (iStartIndex > 0 ) {
			iEndIndex = sText.substring(iStartIndex).indexOf(sEnd);
			sMetadata = sText.substring(iStartIndex + sStart.length(), iEndIndex + iStartIndex);
			
			if (!StringUtils.isEmpty(sMetadata)) {
				// Applications
				Pattern pt = Pattern.compile("<B710>(?<application>.*)</B710>");
				Matcher m = pt.matcher(sMetadata);
				if (m.find()) {
					sTemp = m.group("application");
					String[] aApplication = sTemp.split("</B711>");
					
					for (int i = 0; i < aApplication.length; i++) {
						Pattern pAppDet = Pattern.compile("<B711>.*<snm>(?<name>[^<>]*)</snm>.*<ctry>(?<ctry>[^<>]*)</ctry>.*</B711>");
						Matcher mAppDet = pAppDet.matcher(aApplication[i].concat("</B711>"));
						
						if (mAppDet.find()) {
							sApplications = sApplications + mAppDet.group("name") + " [" + mAppDet.group("ctry") + "]" + "@|@";
						}
					}
				}
				
				// Inventor
				pt = Pattern.compile("<B720>(?<inventors>.*)</B720>");
				m = pt.matcher(sMetadata);
				if (m.find()) {
					sTemp = m.group("inventors");
					String[] aApplication = sTemp.split("</B721>");
					
					for (int i = 0; i < aApplication.length; i++) {
						Pattern pTemp = Pattern.compile("<B721>.*<snm>(?<name>[^<>]*)</snm>.*<ctry>(?<ctry>[^<>]*)</ctry>.*</B721>");
						Matcher mTemp = pTemp.matcher(aApplication[i].concat("</B721>"));
						
						if (mTemp.find()) {
							sInventors = sInventors + mTemp.group("name") + " [" + mTemp.group("ctry") + "]" + "@|@";
						}
					}
				}
				
				// IPC
				pt = Pattern.compile("<B510EP>(?<IPC>.*)</B510EP>");
				m = pt.matcher(sMetadata);
				if (m.find()) {
					sTemp = m.group("IPC");
					
					Pattern pAppDet = Pattern.compile("<classification-ipcr [^<>]*><text>(?<ipc>[^<>]*)</text></classification-ipcr>");
					Matcher mTemp = pAppDet.matcher(sTemp);
					
					while(mTemp.find()) {
						sIPC = sIPC + mTemp.group("ipc") + ",";
					}
				}else {
					pt = Pattern.compile("<B510>(?<IPC>.+?)</B510>");
					m = pt.matcher(sMetadata);
					if (m.find()) {
						sTemp = m.group("IPC");
						
						Pattern pIPC = Pattern.compile("<B511>(?<ipc>[^<>]*)</B511>");
						Matcher mTemp = pIPC.matcher(sTemp);
						while(mTemp.find()) {
							sIPC = sIPC + mTemp.group("ipc") + ",";
						}
						
						pIPC = Pattern.compile("<B512>(?<ipc>[^<>]*)</B512>");
						mTemp = pIPC.matcher(sTemp);
						while(mTemp.find()) {
							sIPC = sIPC + mTemp.group("ipc") + ",";
						}
					}
				}
				
				// Priority
				pt = Pattern.compile("<B310>(?<docid>[^<>]*)</B310><B320><date>(?<date>[^<>]*)</date></B320><B330><ctry>(?<ctry>[^<>]*)</ctry></B330>");
				m = pt.matcher(sMetadata);
				if (m.find()) {
					sPriority = m.group("ctry") + m.group("docid") + " " + m.group("date");
				}
				
				// Applcation Date
				pt = Pattern.compile("<B210>(?<appid>[^<>]*)</B210><B220><date>(?<date>[^<>]*)</date></B220>");
				m = pt.matcher(sMetadata);
				if (m.find()) {
					sDateApplication = m.group("date");
				}
			}
		}
		
		if (sIPC.length() > 1) {
			sIPC = sIPC.substring(0, sIPC.length() - 1 );
		}
		
		sWrite = _oDocumentVO.getsPatentId() + "\t" + _oDocumentVO.getsDatePubl()
				+ "\t" + _oDocumentVO.getsApplicationId() 
				+ "\t" + sDateApplication
				+ "\t" + _oDocumentVO.getsLang()
				+ "\t" + sIPC
				+ "\n"
				;
		try {
			oOutputStream.write(sWrite.getBytes(Charsets.toCharset("utf-8")));
		} catch (IOException e) {
			printLog.writeError("WriteMetaData", e);
		}
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
			printLog.writeError("WriteToFile", e);
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
									if (null != oDocInfo.getsId() && oDocInfo.getsId().length() > 5) {
										oDocInfo.setsApplicationId(String.format("%s-%s-%s", oDocInfo.getsId().substring(0,2), oDocInfo.getsId().substring(2, oDocInfo.getsId().length()-2), oDocInfo.getsId().substring(oDocInfo.getsId().length()-2)));
									}
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
			printLog.writeError("GetPatentInformationXML", e);
		}finally {
			try {
				eventReader.close();
			} catch (XMLStreamException e) {
				// TODO Auto-generated catch block
				printLog.writeError("GetPatentInformationXML", e);
			}
		}
		return oDocInfo;
	}
	
	public void readFrontXML(File folderInput, List<File> files, File oFolderOutPut, String suffixAbstract, String suffixClaim, String suffixDscp, boolean isMetadata) throws Exception {
		int iSize = files.size();
		System.out.println("Size: " + iSize);
		try {
			for (int i = 0; i < iSize; i++) {
//			for (int i = 0; i < 2; i++) {
				try {
					File tempFile = files.get(i);
//					ExtractZipFile(tempFile.getPath(), oFolderOutPut, tempFile.getName(), sYear);
					if (!isMetadata)
						ExtractFrontFile(i, tempFile.getParent(), oFolderOutPut, tempFile.getName(), suffixAbstract, suffixClaim, suffixDscp);
					else
						ExtractFrontFile(i, tempFile.getParent(), oFolderOutPut, tempFile.getName(), suffixAbstract, suffixClaim, suffixDscp, false, true, false, false, false);
					
					if (i%1000 == 0 || (null != _mapPatentOutputStream && _mapPatentOutputStream.size() > 4)) {
						printLog.writeDebugLog("readFrontXML: CloseBuffer:" + _mapPatentOutputStream.size());
						CloseBuffer();
					}
					tempFile = null;
				}catch (Exception e) {
					printLog.writeError("readFrontXML", e);
				}
			}
		}finally {
			CloseBuffer();
		}
	}
	
	private void ExtractFrontFile(int i, String sFilePath, File oFolderOutPut, String sFileName, String suffixAbstract, String suffixClaim, String suffixDscp) throws Exception {
		ExtractFrontFile(i, sFilePath, oFolderOutPut, sFileName, suffixAbstract, suffixClaim, suffixDscp, true, true, true, true, true);
	}
	
	private void ExtractFrontFile(int i, String sFilePath, File oFolderOutPut, String sFileName, String suffixAbstract, String suffixClaim, String suffixDscp, boolean isTitle, boolean ismetadata, boolean isAbstact, boolean isclaim, boolean isdesc) throws Exception {
		 printLog.writeDebugLog("\t" + i + " Start Process : " + sFileName);
	     StringBuffer stringBufferBiblio = null; 
	     StringBuffer stringBufferClaim = null; 
	     StringBuffer stringBufferDscp = null; 
	     String claimDirectoryPath = null;
	     String dscpDirectoryPath = null;
		 try {
            
		 	//Biblio
		 	stringBufferBiblio = getFileContent(sFilePath + "/" + sFileName);
            String sText = stringBufferBiblio.toString();
            DocumentVO documentVO =  getPatentInformationXMLFront(sText);
            stringBufferBiblio = null;
            
            if (isclaim) {
	            //Claim
            	claimDirectoryPath = sFilePath.replaceAll(suffixAbstract, suffixClaim);
	            stringBufferClaim = getFileContent(claimDirectoryPath + "/" + sFileName.replaceAll(suffixAbstract, suffixClaim));
	            sText = stringBufferClaim.toString();
	            documentVO =  getPatentClaimXMLFront(sText, documentVO);
	            stringBufferClaim = null;
            }
            
            if (isdesc) {
	            //Description
            	dscpDirectoryPath = sFilePath.replaceAll(suffixAbstract, suffixDscp);
	            stringBufferDscp = getFileContent(dscpDirectoryPath + "/" + sFileName.replaceAll(suffixAbstract, suffixDscp));
	            sText = stringBufferDscp.toString();
	            documentVO =  getPatentDescriptionXMLFront(sText, documentVO);
	            stringBufferDscp = null;
            }
            
            writeFrontFilePerYear(documentVO, oFolderOutPut, isTitle, ismetadata, isAbstact, isclaim, isdesc);
		} catch ( Exception e) {
			// TODO Auto-generated catch block
			printLog.writeError("ExtractFrontFile", e);
		} finally {
		    
		}
		printLog.writeDebugLog("\tEnd Process : " + sFileName);
	}
	
	
	private StringBuffer getFileContent(String filePath) {
		StringBuffer strBuffer = new StringBuffer();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), "UTF-8"));
			String line;
			while ((line = br.readLine()) != null) {
				strBuffer.append(line).append("\n");
			}
		}catch (Exception e) {
			// TODO: handle exception
		}finally {
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					
				}
				br = null;
			}
		}
		
		
		return strBuffer;
	}
	
	public DocumentVO getPatentInformationXMLFront(String sText) throws Exception {
		DocumentVO oDocInfo = new DocumentVO();
		try {
			Pattern newLine = Pattern.compile("(?<begin>>)(\\r\\n|\\n)([ ]|\\t)*(?<end><)");
			sText = newLine.matcher(sText).replaceAll("${begin}${end}");
			newLine = Pattern.compile("(\\r\\n|\\n)([ ]|\\t)*");
			sText = newLine.matcher(sText).replaceAll("</p><p>");
			Pattern pAppDet = Pattern.compile(
					"<document-id document-id-type=\"docdb\"><country>(?<ctrcd>[a-z]+)</country><doc-number>(?<ptid>[0-9]+)</doc-number><kind>(?<kindcd>[a-z0-9]+)</kind><date>(?<date>[0-9]+)</date></document-id>"
					, Pattern.CASE_INSENSITIVE);
			Matcher mAppDet = pAppDet.matcher(sText);
			
			if (mAppDet.find()) {
				String coutrycode = mAppDet.group("ctrcd");
				String docNumber = mAppDet.group("ptid");
				String kindcode = mAppDet.group("kindcd");
				String publicationDate = mAppDet.group("date");
				
				oDocInfo.setsId(coutrycode+docNumber+kindcode);
				oDocInfo.setsCountry(coutrycode);
				oDocInfo.setsDocNumber(docNumber);
				oDocInfo.setSkind(kindcode);
				oDocInfo.setsDatePubl(publicationDate);
//				oDocInfo.setsApplicationId(String.format("%s-%s-%s", coutrycode, docNumber, kindcode));
				oDocInfo.setsPatentId(String.format("%s-%s-%s", coutrycode, docNumber, kindcode));

			}
			
			String titlePattern = "<invention-title lang=\"(?<lang>[a-z]+)\">(?<content>.+?)</invention-title>";
			oDocInfo.setTitle(getContent(titlePattern, sText));
			
			String abstractPattern = "<abstract lang=\"(?<lang>[a-z]+)\">(?<content>.+?)</abstract>";
			oDocInfo.setAbstracttext(getContent(abstractPattern, sText));
			
			oDocInfo = getMetaDataFrontFile(sText, oDocInfo);
							
		}catch (Exception e) {
			printLog.writeError("GetPatentInformationXMLFront", e);
		}finally {
			
		}
		return oDocInfo;
	}
	
	
	private HashMap<String, String> getContent(String pattern, String sText){
		HashMap<String, String> tmpmap = new HashMap<String, String>();
		Pattern pAppDet  = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		Matcher mAppDet = pAppDet.matcher(sText);
		while (mAppDet.find()) {
			String languageId = mAppDet.group("lang");
			String content = mAppDet.group("content");
			tmpmap.put(languageId, content);
		}
		
		return tmpmap;
	}
	
	public DocumentVO getPatentClaimXMLFront(String sText, DocumentVO oDocInfo) throws Exception {
		try {
			Pattern newLine = Pattern.compile("(?<begin>>)(\\r\\n|\\n)([ ]|\\t)*(?<end><)");
			sText = newLine.matcher(sText).replaceAll("${begin}${end}");
			newLine = Pattern.compile("(\\r\\n|\\n)([ ]|\\t)*");
			sText = newLine.matcher(sText).replaceAll("</p><p>");
			String titlePattern = "<claims lang=\\\"(?<lang>[a-z]+)\\\">(?<content>.+?)</claims>";
			oDocInfo.setClaim(getContent(titlePattern, sText));
			
		}catch (Exception e) {
			printLog.writeError("GetPatentClaimXMLFront", e);
		}finally {
			
		}
		return oDocInfo;
	}
	
	public DocumentVO getPatentDescriptionXMLFront(String sText, DocumentVO oDocInfo) throws Exception {
		try {
			Pattern newLine = Pattern.compile("(?<begin>>)(\\r\\n|\\n)([ ]|\\t)*(?<end><)");
			sText = newLine.matcher(sText).replaceAll("${begin}${end}");
			newLine = Pattern.compile("(\\r\\n|\\n)([ ]|\\t)*");
			sText = newLine.matcher(sText).replaceAll("</p><p>");
//			printLog.writeDebugLog("sText: " + sText);
			String titlePattern = "<description lang=\\\"(?<lang>[a-z]+)\\\">(?<content>.+?)</description>";
			oDocInfo.setDescription(getContent(titlePattern, sText));
			
		}catch (Exception e) {
			printLog.writeError("GetPatentClaimXMLFront", e);
		}finally {
			
		}
		return oDocInfo;
	}
	
	public void writeFrontFilePerYear(DocumentVO documentVO, File oFolderOutPut, boolean isTitle, boolean ismetadata, boolean isAbstact, boolean isclaim, boolean isdesc) {
		 String sYear = "";
		 if (null != documentVO.getsDatePubl() && documentVO.getsDatePubl().trim().length() > 4) {
			 sYear = documentVO.getsDatePubl().substring(0, 4);
		 }
		 String sKey = documentVO.getsLang() + "-" + sYear;
         
         PatentOutputStream oPatentOutputStream = null;
         for(Map.Entry<String, String> m: documentVO.getTitle().entrySet()) {
			 sKey = m.getKey().toUpperCase() + "-" + sYear;
			 oPatentOutputStream = (PatentOutputStream) _mapPatentOutputStream.get(sKey);
	         if (null == oPatentOutputStream) {
//	         	oPatentOutputStream = new PatentOutputStream(oFolderOutPut.getPath(),    m.getKey(), sYear, printLog);
	         	oPatentOutputStream = new PatentOutputStream(oFolderOutPut.getPath(),    m.getKey(), sYear, printLog, isTitle, ismetadata, isAbstact, isclaim, isdesc);
	         	_mapPatentOutputStream.put(sKey, oPatentOutputStream);
	         }
	         if (isTitle)
	        	 WriteToFile(oPatentOutputStream.getOutputStreamTitle(), m.getValue(), documentVO.getsPatentId(), documentVO.getsDatePubl());
	         if (ismetadata) {
		         String metadata = documentVO.getsApplicationId() 
					+ "\t" + documentVO.getsDateApplication()
					+ "\t" + m.getKey().toUpperCase()
					+ "\t" + documentVO.getsIPC();
		         WriteToFile(oPatentOutputStream.getOutputStreamMetaData(), metadata, documentVO.getsPatentId(), documentVO.getsDatePubl());
	         }
	         
		 }
		 
         if (isAbstact) {
			 for(Map.Entry<String, String> m: documentVO.getAbstracttext().entrySet()) {
				 sKey = m.getKey().toUpperCase() + "-" + sYear;
				 oPatentOutputStream = (PatentOutputStream) _mapPatentOutputStream.get(sKey);
		         if (null == oPatentOutputStream) {
		         	oPatentOutputStream = new PatentOutputStream(oFolderOutPut.getPath(),   m.getKey(), sYear, printLog);
		         	_mapPatentOutputStream.put(sKey, oPatentOutputStream);
		         }
		         WriteToFile(oPatentOutputStream.getOutputStreamAbstract(), m.getValue(), documentVO.getsPatentId(), documentVO.getsDatePubl());
			 }
         }
         
         if (isclaim) {
			 for(Map.Entry<String, String> m: documentVO.getClaim().entrySet()) {
				 sKey = m.getKey().toUpperCase() + "-" + sYear;
				 oPatentOutputStream = (PatentOutputStream) _mapPatentOutputStream.get(sKey);
		         if (null == oPatentOutputStream) {
		         	oPatentOutputStream = new PatentOutputStream(oFolderOutPut.getPath(),   m.getKey(), sYear, printLog);
		         	_mapPatentOutputStream.put(sKey, oPatentOutputStream);
		         }
		         WriteToFile(oPatentOutputStream.getOutputStreamClaims(), m.getValue(), documentVO.getsPatentId(), documentVO.getsDatePubl());
			 }
         }
         
         if (isdesc) {
			 for(Map.Entry<String, String> m: documentVO.getDescription().entrySet()) {
				 sKey = m.getKey().toUpperCase() + "-" + sYear;
				 oPatentOutputStream = (PatentOutputStream) _mapPatentOutputStream.get(sKey);
		         if (null == oPatentOutputStream) {
		         	oPatentOutputStream = new PatentOutputStream(oFolderOutPut.getPath(),   m.getKey(), sYear, printLog);
		         	_mapPatentOutputStream.put(sKey, oPatentOutputStream);
		         }
		         WriteToFile(oPatentOutputStream.getOutputStreamDescription(), m.getValue(), documentVO.getsPatentId(), documentVO.getsDatePubl());
			 }
         }
		 
//		 CloseBuffer();
	}
	
	private void WriteToFile(OutputStream oOutputStream, String sText, String patentId, String datePulb) {
		String sWrite = patentId + "\t" + datePulb + "\t"  + sText +"\n";
		try {
			oOutputStream.write(sWrite.getBytes(Charsets.toCharset("utf-8")));
		} catch (IOException e) {
			printLog.writeError("WriteToFile", e);
		}
	}
	
	private DocumentVO getMetaDataFrontFile(String sText, DocumentVO documentVO) throws Exception {
		String sMetadata = "";
		String sApplicationid = "";
		String sIPC = "";
		String sTemp = "";
		String sDateApplication = "";
		
		sMetadata = sText;
		
		if (!StringUtils.isEmpty(sMetadata)) {
			// Applications
			Pattern pt = Pattern.compile("<application-reference doc-id=\"[0-9]+\">(?<content>.+?)</application-reference>", Pattern.CASE_INSENSITIVE);
			Matcher m = pt.matcher(sMetadata);
			if (m.find()) {
				sTemp = m.group("content");
				
				pt = Pattern.compile("<document-id document-id-type=\\\"docdb\\\"><country>(?<ctrcd>[a-z]+)</country><doc-number>(?<ptid>[0-9]+)</doc-number><kind>(?<kindcd>[a-z0-9]+)</kind></document-id>", Pattern.CASE_INSENSITIVE);
				Matcher mAppDet = pt.matcher(sTemp);
				if (mAppDet.find()) {
					sApplicationid = mAppDet.group("ctrcd") + "-" + mAppDet.group("ptid") + "-" + mAppDet.group("kindcd");
				}
				
				pt = Pattern.compile("<document-id document-id-type=\\\"epodoc\\\"><doc-number>(?<ptid>[a-z0-9]+)</doc-number><date>(?<date>[0-9]+)</date></document-id>", Pattern.CASE_INSENSITIVE);
				mAppDet = pt.matcher(sTemp);
				if (mAppDet.find()) {
					sDateApplication = mAppDet.group("date");
				}
			}
			
			// IPC
			pt = Pattern.compile("<classification-ipcr sequence=\"[0-9]+\"><text>(?<content>.+?)</text></classification-ipcr>", Pattern.CASE_INSENSITIVE);
			m = pt.matcher(sMetadata);
			while(m.find()) {
				sIPC = sIPC + m.group("content").trim().replaceAll("( )+", " ") + ",";
			}
		}
		
		if (sIPC.length() > 1) {
			sIPC = sIPC.substring(0, sIPC.length() - 1 );
		}
		
		documentVO.setsApplicationId(sApplicationid);
		documentVO.setsDateApplication(sDateApplication);
		documentVO.setsIPC(sIPC);
		
		return documentVO;
		
	}
	
	
}

