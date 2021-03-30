package patentdata.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;

public class USPTOXml {
	private Pattern patternDTD = null;
	private static String lineMarker = "<br/>";
	private HashMap<DTD, Pattern> hashPatternDocID = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternDocPublDate = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternAppID = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternAppPublDate = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternTitle = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternAbstract = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternClaims = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternBriefSum = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternGovInterest = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternDrawingDesp = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternAppType = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternDescription = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternMetadata = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPCs = new HashMap<DTD, Pattern>();
	//
	private HashMap<DTD, Pattern> hashPatternIPC = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPCV40DTD = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternFTIPCV40DTD = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPCSV40DTD = new HashMap<DTD, Pattern>();
//	private HashMap<DTD, Pattern> hashPatternIPCClass = new HashMap<DTD, Pattern>();
//	private HashMap<DTD, Pattern> hashPatternIPCSubClass = new HashMap<DTD, Pattern>();
//	private HashMap<DTD, Pattern> hashPatternIPCMainGroup = new HashMap<DTD, Pattern>();
//	private HashMap<DTD, Pattern> hashPatternIPCSubGroup = new HashMap<DTD, Pattern>();
//	private HashMap<DTD, Pattern> hashPatternPriority = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternApplication = new HashMap<DTD, Pattern>();
	//
	private DTD xmlDTD = DTD.Other;
	private String xmlString = "";
	private String docid = "";
	private String title = "";
	private String abstract_ = "";
	private String claims = "";
	private String description = "";
	private String publicationdate = "";
	private String metadata = "";
	private String year = "";
	private String appid = "";
	private String appPublicationdate = "";
	private String briefSummary = "";
	private String drawingDscp = "";
	private LogFourJ printlog = null;
	//

	private enum DTD {
		PATDOC, US_PATENT_GRANT, US_PATENT_APPLICATION, Unknown, Other
	}

	public USPTOXml(LogFourJ printLog) {
		this.printlog = printLog;
		this.preparePattern();
	}
	
	public USPTOXml(String logpath) {
		this.preparePattern();
	}

	public void setXmlString(String xml) throws Exception {
		this.xmlString = xml;
		this.getDTD();
		this.preparePattern();
		this.resetData();
		if (this.xmlDTD == DTD.Other) {
			this.extractDataASCII();
		}else {
			this.extractData();
		}
	}

	private void resetData() {
		this.title = "";
		this.abstract_ = "";
		this.claims = "";
		this.description = "";
		this.publicationdate = "";
		this.metadata = "";
		this.year = "";
	}

	private void preparePattern() {
		if (this.xmlDTD == DTD.Other) {
			this.patternDTD = Pattern.compile("\\<\\!DOCTYPE\\s(?<root>[^\\[\\s]+)\\s(?<system>[^\\[\\s]+)\\s(?<name>.+)\\[");
			this.hashPatternDocID.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "WKU[ ]*(?<docno>.+?)" + lineMarker  + ")"));
			this.hashPatternAppID.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "APN[ ]*(?<appno>.+?)" + lineMarker  + ")"));
			this.hashPatternAppPublDate.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "APD[ ]*(?<appdt>.+?)" + lineMarker  + ")"));
			this.hashPatternDocPublDate.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "ISD[ ]*(?<docdt>.+?)" + lineMarker  + ")"));
//			this.hashPatternDocPublDate.put(this.xmlDTD, Pattern.compile("(ISSUE[ ]\\-([ ]*[0-9]+[ ]+)(?<docdt>[0-9]+?)[ ]*" + lineMarker  + ")"));
			this.hashPatternTitle.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "TTL[ ]*(?<title>.+?)" + lineMarker  + "[A-Z].+?([ ]|" + lineMarker  + "))"));
			this.hashPatternAbstract.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "ABST[ ]*" + lineMarker  + "[ ]*(?<abstract>.+?)" + lineMarker  + "(([A-Z]{4}([ ]|" + lineMarker  + "))|$))"));
			this.hashPatternDescription.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "DETD[ ]*" + lineMarker  + "[ ]*(?<description>.+?)" + lineMarker  + "(([A-Z]{4}([ ]|" + lineMarker  + "))|$))"));
			this.hashPatternClaims.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "CLMS[ ]*" + lineMarker  + "[ ]*(?<claims>.+?)" + lineMarker  + "(([A-Z]{4}([ ]|" + lineMarker  + "))|$))"));
			this.hashPatternBriefSum.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "BSUM[ ]*" + lineMarker  + "[ ]*(?<bsum>.+?)" + lineMarker  + "(([A-Z]{4}([ ]|" + lineMarker  + "))|$))"));
			this.hashPatternGovInterest.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "GOVT[ ]*" + lineMarker  + "[ ]*(?<govt>.+?)" + lineMarker  + "(([A-Z]{4}([ ]|" + lineMarker  + "))|$))"));
			this.hashPatternDrawingDesp.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "DRWD[ ]*" + lineMarker  + "[ ]*(?<drwd>.+?)" + lineMarker  + "(([A-Z]{4}([ ]|" + lineMarker  + "))|$))"));
			this.hashPatternIPCs.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "ICL[ ]*(?<ipc>.+?)" + lineMarker  + "(([A-Z]{3,4}([ ]|" + lineMarker  + "))|$))"));
			this.hashPatternAppType.put(this.xmlDTD, Pattern.compile("(" + lineMarker  + "APT[ ]*(?<apt>[0-9]{1}))" + lineMarker));

		} else if (hashPatternTitle.get(this.xmlDTD) == null) {
			if (this.xmlDTD == DTD.PATDOC) {
				hashPatternDocID.put(this.xmlDTD, Pattern.compile("<B100><B110><DNUM><PDAT>(?<docno>[^<>]*)<\\/PDAT><\\/DNUM><\\/B110><B130><PDAT>(?<kind>[^<>]*)<\\/PDAT><\\/B130><B140><DATE><PDAT>(?<date>[^<>]*)<\\/PDAT><\\/DATE><\\/B140><B190><PDAT>(?<country>[^<>]*)<\\/PDAT><\\/B190><\\/B100>"));
				hashPatternTitle.put(this.xmlDTD, Pattern.compile("<B540><STEXT><PDAT>(?<title>.*)<\\/PDAT><\\/STEXT><\\/B540>"));
				hashPatternAbstract.put(this.xmlDTD, Pattern.compile("<SDOAB><BTEXT>(?<abstract>.*)<\\/BTEXT><\\/SDOAB>"));
				hashPatternClaims.put(this.xmlDTD, Pattern.compile("<SDOCL>(?<claims>.*)<\\/SDOCL>"));
				hashPatternDescription.put(this.xmlDTD, Pattern.compile("<SDODE>(?<description>.*)<\\/SDODE>"));
				hashPatternMetadata.put(this.xmlDTD, Pattern.compile("<SDOBI>(?<metadata>.*)<\\/SDOBI>"));
				hashPatternIPCs.put(this.xmlDTD, Pattern.compile("<B51[12]><PDAT>(?<ipc>[^<>]*)</PDAT></B51[12]>"));
				// hashPatternPriority.put(this.xmlDTD, Pattern.compile("<B51[12]><PDAT>(?<ipc>[^<]*)<\\/PDAT><\\/B51[12]>"));
				hashPatternApplication.put(this.xmlDTD, Pattern.compile("<B210><DNUM><PDAT>(?<docno>.[^<>]*)</PDAT></DNUM></B210>(<B211US><PDAT>(?<ustime>[^<>]*)</PDAT></B211US>)*(<B220><DATE><PDAT>(?<date>[^<>]*)</PDAT></DATE></B220>)*"));
			} else {
				hashPatternDocID.put(this.xmlDTD, Pattern.compile("<publication-reference><document-id><country>(?<country>[^<>]*)<\\/country><doc-number>(?<docno>[^<>]*)<\\/doc-number><kind>(?<kind>[^<>]*)<\\/kind><date>(?<date>[^<>]*)<\\/date><\\/document-id><\\/publication-reference>"));
				hashPatternTitle.put(this.xmlDTD, Pattern.compile("<invention-title\\s[^>]*>(?<title>.*)<\\/invention-title>"));
				hashPatternAbstract.put(this.xmlDTD, Pattern.compile("<abstract\\s[^>]*>(?<abstract>.*)<\\/abstract>"));
				hashPatternClaims.put(this.xmlDTD, Pattern.compile("<claims\\s[^>]*>(?<claims>.*)<\\/claims>"));
				hashPatternDescription.put(this.xmlDTD, Pattern.compile("<description\\s[^>]*>(?<description>.*)<\\/description>"));
				hashPatternMetadata.put(this.xmlDTD, Pattern.compile("<us-bibliographic-data-grant>(?<metadata>.*)<\\/us-bibliographic-data-grant>"));
				// ipc 
				 hashPatternIPCs.put(this.xmlDTD, Pattern.compile("<classifications-ipcr>(?<ipc>.*)<\\/classifications-ipcr>"));
				 hashPatternIPC.put(this.xmlDTD, Pattern.compile("<classification-ipcr><ipc-version-indicator><date>(?<date>[^<>]*)</date></ipc-version-indicator><classification-level>(?<level>[^<>]*)</classification-level><section>(?<section>[^<>]*)</section><class>(?<class>[^<>]*)</class><subclass>(?<sclass>[^<>]*)</subclass><main-group>(?<group>[^<>]*)</main-group><subgroup>(?<sgroup>[^<>]*)</subgroup>"));
				 hashPatternIPCSV40DTD.put(this.xmlDTD, Pattern.compile("<classification-ipc>(?<ipc>.+?)</classification-ipc>"));
				 hashPatternIPCV40DTD.put(this.xmlDTD, Pattern.compile("<main-classification>(?<ipc>[^<>]*)</main-classification>"));
				 hashPatternFTIPCV40DTD.put(this.xmlDTD, Pattern.compile("<further-classification>(?<ipc>[^<>]*)</further-classification>"));
				 hashPatternApplication.put(this.xmlDTD, Pattern.compile("<application-reference [^<>]*><document-id><country>(?<ctry>[^<>]*)</country><doc-number>(?<docno>[^<>]*)</doc-number><date>(?<date>[^<>]*)</date></document-id></application-reference>"));
//				  hashPatternIPCClass.put(this.xmlDTD, Pattern.compile("<class>(?<class>[^<]*)<\\/class>"));
//				  hashPatternIPCSubClass.put(this.xmlDTD, Pattern.compile("<subclass>(?<subclass>[^<]*)<\\/subclass>"));
//				  hashPatternIPCMainGroup.put(this.xmlDTD, Pattern.compile("<main-group>(?<maingroup>[^<]*)<\\/main-group>"));
//				  hashPatternIPCSubGroup.put(this.xmlDTD, Pattern.compile("<subgroup>(?<subgroup>[^<]*)<\\/subgroup>"));
//				 
				// priority
			}
		}
	}

	private void getDTD() throws Exception {
		String rootEle = "";

		Matcher m = this.patternDTD.matcher(this.xmlString);
		if (m.find()) {
			rootEle = m.group("root");
		}
		rootEle = rootEle.toLowerCase();

		if (rootEle.equals("patdoc")) {
			this.xmlDTD = DTD.PATDOC;
		} else if (rootEle.equals("us-patent-grant")) {
			this.xmlDTD = DTD.US_PATENT_GRANT;
		} else if (rootEle.equals("us-patent-application")) {
			this.xmlDTD = DTD.US_PATENT_APPLICATION;
		} else {
			this.xmlDTD = DTD.Other;
		}
		
	}

	private void extractData() {

		Matcher m = this.hashPatternDocID.get(this.xmlDTD).matcher(this.xmlString);
		if (m.find()) {
			this.docid = String.format("%s-%s-%s", m.group("country"), m.group("docno"), m.group("kind"));
			this.publicationdate = m.group("date");
		}
		
		this.title = getData(this.hashPatternTitle.get(this.xmlDTD), "title");
		this.abstract_ = getData(this.hashPatternAbstract.get(this.xmlDTD), "abstract");
		this.claims = getData(this.hashPatternClaims.get(this.xmlDTD), "claims");
		this.description = getData(this.hashPatternDescription.get(this.xmlDTD), "description");
		this.metadata = getDataMetaData();
	}
	
	private void extractDataASCII() {

		Matcher m = this.hashPatternDocID.get(this.xmlDTD).matcher(this.xmlString);
		if (m.find()) {
			String sTmpDocId = m.group("docno");
			if (sTmpDocId.length() > 8)
				sTmpDocId = sTmpDocId.substring(0, 8);
			this.docid = String.format("%s-%s", "US", sTmpDocId);
		}
		
		m = this.hashPatternDocPublDate.get(this.xmlDTD).matcher(this.xmlString);
		if (m.find()) {
			this.publicationdate = m.group("docdt");
		}
		
		m = this.hashPatternAppID.get(this.xmlDTD).matcher(this.xmlString);
		if (m.find()) {
			String sTempAppId = m.group("appno");
			if (sTempAppId.length() > 6)
				sTempAppId = sTempAppId.substring(0, 6);
			this.appid = String.format("%s-%s", "US", sTempAppId);
		}
		
		String sAppType = "";
		m = this.hashPatternAppType.get(this.xmlDTD).matcher(this.xmlString);
		if (m.find()) {
			sAppType = m.group("apt");
		}
		
		if (StringUtils.isNotEmpty(sAppType)) {
			this.appid =  this.appid + "-" + sAppType;
		}
		m = this.hashPatternAppPublDate.get(this.xmlDTD).matcher(this.xmlString);
		if (m.find()) {
			this.appPublicationdate = m.group("appdt").trim();
		}
		
		this.title = getData(this.hashPatternTitle.get(this.xmlDTD), "title");
		
		this.abstract_ = getData(this.hashPatternAbstract.get(this.xmlDTD), "abstract");
		this.claims = getData(this.hashPatternClaims.get(this.xmlDTD), "claims");
		this.description = getData(this.hashPatternDescription.get(this.xmlDTD), "description");
		this.briefSummary = getData(this.hashPatternBriefSum.get(this.xmlDTD), "bsum");
		this.drawingDscp = getData(this.hashPatternDrawingDesp.get(this.xmlDTD), "drwd");
		this.description = this.briefSummary + lineMarker + this.drawingDscp + lineMarker + this.description;
		this.metadata = getDataMetaData();

		if ("US-05705118".equals(this.docid)) {
			System.out.println("docid: " + docid);
			System.out.println("publicationdate: " + publicationdate);
			System.out.println("appid: " + appid);
			System.out.println("appPublicationdate: " + appPublicationdate);
			System.out.println("title: " + title);
			System.out.println("abstract_: " + abstract_);
			System.out.println("claims: " + claims);
			System.out.println("description: " + description);
			System.out.println("briefSummary: " + briefSummary);
			System.out.println("xml: " + this.xmlString);
		}
		
//		System.out.println("drawingDscp: " + drawingDscp);
//		System.out.println("metadata: " + metadata);
	}
	
	private String getData(Pattern p, String data) {
		Matcher m = p.matcher(this.xmlString);
		try {
			if (m.find())
				return m.group(data);
		}catch(Exception e) {
			printlog.writeError("getData", e);
		}
		return "";
	}

	public String getDocID() {
		return this.docid.trim();
	}

	public String getTitle() {
		return this.title.trim();
	}

	public String getAbstract() {
		return this.abstract_.trim();
	}

	public String getClaims() {
		return this.claims.trim();
	}

	public String getDescription() {
		return this.description.trim();
	}
	
	public String getMetaData() {
		return this.metadata;
	}

	public String getDataMetaData() {
		 
		 String sMetadata = "";
		 String sIPC = "";
		 String sApplication = "";
		 String sDateApplicaton = "";
		 
//		 if (this.docid.equals("US-06243980-B1")) {
//				System.out.println("Test: " + this.xmlString);
//			}
		 if (null != this.xmlDTD && this.xmlDTD != DTD.Unknown) {
			 
			 if (this.xmlDTD == DTD.PATDOC) { 
				 // get ipc 
				 Matcher m = this.hashPatternIPCs.get(this.xmlDTD).matcher(this.xmlString); 
				 while (m.find()) {
					 String ipc = m.group("ipc"); 
					 //A01K 6700 
					 if (ipc.length() > 0 && ipc.indexOf(" ") > 0) { 
						 ipc = ipc.substring(0, 7) + "/" + ipc.substring(7);
						 
					 } 
					 
					 sIPC = ipc + ",";
				 }
				 //get application
				 m = this.hashPatternApplication.get(this.xmlDTD).matcher(this.xmlString);
				 if (m.find()) {
					 sApplication = String.format("%s-%s", "US", m.group("docno") );
					 sDateApplicaton = m.group("date");
				 }
				 
			 }else if (this.xmlDTD == DTD.US_PATENT_GRANT || this.xmlDTD == DTD.US_PATENT_APPLICATION){ 
				 
				 //get ipc 
				 Matcher m = this.hashPatternIPCs.get(this.xmlDTD).matcher(this.xmlString); 
				 if (m.find()) {
					 String sIPCs = m.group("ipc");
					 Matcher m1 = this.hashPatternIPC.get(this.xmlDTD).matcher(sIPCs); 
					 
					 sIPC = "";
					 while(m1.find()) {
						 sIPC = sIPC + m1.group("section") + m1.group("class") + m1.group("sclass") + m1.group("group") + "/" + m1.group("sgroup") + "," ;
					 }
					 
				 }else {
					 m = this.hashPatternIPCSV40DTD.get(this.xmlDTD).matcher(this.xmlString);
					 if (m.find()) {
						 String sIPCs = m.group("ipc");
//						 if (sIPCs.indexOf("<classification-national>") > 0) {
//							 sIPCs = sIPCs.substring(0, sIPCs.indexOf("<classification-national>")+1);
//						 }
						 Matcher m1 = this.hashPatternIPCV40DTD.get(this.xmlDTD).matcher(sIPCs); 
						 
						 sIPC = "";
						 while(m1.find()) {
							 sIPC = sIPC + m1.group("ipc") + "," ;
						 }
						 
						 m1 = this.hashPatternFTIPCV40DTD.get(this.xmlDTD).matcher(sIPCs); 
						 while(m1.find()) {
							 sIPC = sIPC + m1.group("ipc") + "," ;
						 }
					 }
				 }
				 
				 //get application
				 m = this.hashPatternApplication.get(this.xmlDTD).matcher(this.xmlString);
				 if (m.find()) {
					 sApplication = String.format("%s-%s", m.group("ctry"), m.group("docno") );
					 sDateApplicaton = m.group("date");
				 }
				 
				 
			}else if (this.xmlDTD == DTD.Other){ 
				 
				 //get ipc 
				 sApplication = this.appid;
				 sDateApplicaton = this.appPublicationdate;
				 Matcher m = this.hashPatternIPCs.get(this.xmlDTD).matcher(this.xmlString);
				 
				 sIPC = "";
				 while(m.find()) {
					 String sTempIPC = m.group("ipc");
					 if (sTempIPC.length() >= 7)
						 sTempIPC = sTempIPC.substring(0, 4) + sTempIPC.substring(4,7).trim() + "/" + sTempIPC.substring(7);
					 sIPC = sIPC + sTempIPC + "," ;
				 }
				 
			} 
			 
			 if (sIPC.length() > 1) {
				 sIPC = sIPC.substring(0,sIPC.length() - 1);
			 }
			 sMetadata = sApplication +"\t" + sDateApplicaton +"\t" + "EN" + "\t" + sIPC;
		 }
		 return sMetadata;
	}

	public String getYear() {
		return this.publicationdate.trim().substring(0, 4);
	}

	public String getDate() {
		return this.publicationdate.trim();
	}

}
