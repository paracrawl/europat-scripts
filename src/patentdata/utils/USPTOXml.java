package patentdata.utils;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class USPTOXml {
	private Pattern patternDTD = null;
	private HashMap<DTD, Pattern> hashPatternDocID = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternTitle = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternAbstract = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternClaims = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternDescription = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternMetadata = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPC = new HashMap<DTD, Pattern>();
	//
	private HashMap<DTD, Pattern> hashPatternIPCSection = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPCClass = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPCSubClass = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPCMainGroup = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternIPCSubGroup = new HashMap<DTD, Pattern>();
	private HashMap<DTD, Pattern> hashPatternPriority = new HashMap<DTD, Pattern>();
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
	//

	private enum DTD {
		PATDOC, US_PATENT_GRANT, US_PATENT_APPLICATION, Unknown, Other
	}

	public USPTOXml() {
		this.preparePattern();
	}

	public void setXmlString(String xml) throws Exception {
		this.xmlString = xml;
		this.getDTD();
		this.preparePattern();
		this.resetData();
		this.extractData();
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
		} else if (hashPatternTitle.get(this.xmlDTD) == null) {
			if (this.xmlDTD == DTD.PATDOC) {
				hashPatternDocID.put(this.xmlDTD, Pattern.compile("<B100><B110><DNUM><PDAT>(?<docno>[^<>]*)<\\/PDAT><\\/DNUM><\\/B110><B130><PDAT>(?<kind>[^<>]*)<\\/PDAT><\\/B130><B140><DATE><PDAT>(?<date>[^<>]*)<\\/PDAT><\\/DATE><\\/B140><B190><PDAT>(?<country>[^<>]*)<\\/PDAT><\\/B190><\\/B100>"));
				hashPatternTitle.put(this.xmlDTD, Pattern.compile("<B540><STEXT><PDAT>(?<title>.*)<\\/PDAT><\\/STEXT><\\/B540>"));
				hashPatternAbstract.put(this.xmlDTD, Pattern.compile("<SDOAB><BTEXT>(?<abstract>.*)<\\/BTEXT><\\/SDOAB>"));
				hashPatternClaims.put(this.xmlDTD, Pattern.compile("<SDOCL>(?<claims>.*)<\\/SDOCL>"));
				hashPatternDescription.put(this.xmlDTD, Pattern.compile("<SDODE>(?<description>.*)<\\/SDODE>"));
				hashPatternMetadata.put(this.xmlDTD, Pattern.compile("<SDOBI>(?<metadata>.*)<\\/SDOBI>"));
				// hashPatternIPC.put(this.xmlDTD, Pattern.compile("<B51[12]><PDAT>(?<ipc>[^<]*)<\\/PDAT><\\/B51[12]>"));
				// hashPatternPriority.put(this.xmlDTD, Pattern.compile("<B51[12]><PDAT>(?<ipc>[^<]*)<\\/PDAT><\\/B51[12]>"));
				// hashPatternApplication.put(this.xmlDTD, Pattern.compile("<B200><B210><DNUM><PDAT>(?<docid>.*)<\\/PDAT><\\/DNUM><\\/B210><B130><PDAT>(?<kind>.*)<\\/PDAT><\\/B130><B140><DATE><PDAT>(?<date>.*)<\\/PDAT><\\/DATE><\\/B140><B190><PDAT>(?<country>.*)<\\/PDAT><\\/B190><\\/B100>"));
			} else {
				hashPatternDocID.put(this.xmlDTD, Pattern.compile("<publication-reference><document-id><country>(?<country>[^<>]*)<\\/country><doc-number>(?<docno>[^<>]*)<\\/doc-number><kind>(?<kind>[^<>]*)<\\/kind><date>(?<date>[^<>]*)<\\/date><\\/document-id><\\/publication-reference>"));
				hashPatternTitle.put(this.xmlDTD, Pattern.compile("<invention-title\\s[^>]*>(?<title>.*)<\\/invention-title>"));
				hashPatternAbstract.put(this.xmlDTD, Pattern.compile("<abstract\\s[^>]*>(?<abstract>.*)<\\/abstract>"));
				hashPatternClaims.put(this.xmlDTD, Pattern.compile("<claims\\s[^>]*>(?<claims>.*)<\\/claims>"));
				hashPatternDescription.put(this.xmlDTD, Pattern.compile("<description\\s[^>]*>(?<description>.*)<\\/description>"));
				hashPatternMetadata.put(this.xmlDTD, Pattern.compile("<us-bibliographic-data-grant>(?<metadata>.*)<\\/us-bibliographic-data-grant>"));
				// ipc 
				// hashPatternIPC.put(this.xmlDTD, Pattern.compile("<classification-ipcr>(?<ipc>.*)<\\/classification-ipcr>"));
				/* 
				 * hashPatternIPCSection.put(this.xmlDTD, Pattern.compile("<section>(?<section>[^<]*)<\\/section>"));
				 * hashPatternIPCClass.put(this.xmlDTD, Pattern.compile("<class>(?<class>[^<]*)<\\/class>"));
				 * hashPatternIPCSubClass.put(this.xmlDTD, Pattern.compile("<subclass>(?<subclass>[^<]*)<\\/subclass>"));
				 * hashPatternIPCMainGroup.put(this.xmlDTD, Pattern.compile("<main-group>(?<maingroup>[^<]*)<\\/main-group>"));
				 * hashPatternIPCSubGroup.put(this.xmlDTD, Pattern.compile("<subgroup>(?<subgroup>[^<]*)<\\/subgroup>"));
				 */
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
			this.xmlDTD = DTD.Unknown;
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
		this.metadata = getData(this.hashPatternMetadata.get(this.xmlDTD), "metadata");
	}
	
	private String getData(Pattern p, String data) {
		Matcher m = p.matcher(this.xmlString);
		if (m.find())
			return m.group(data);
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
		return this.metadata.trim();

		/*
		 * List<String> ipcList = new ArrayList<String>();
		 * 
		 * if (this.xmlDTD == DTD.PATDOC) { //get ipc Matcher m =
		 * this.hashPatternIPC.get(this.xmlDTD).matcher(this.metadata); while (m.find())
		 * { String ipc = m.group("ipc"); //A01K 6700 if (ipc.length() > 0 &&
		 * ipc.indexOf(" ") > 0) { ipc = ipc.substring(0, 7) + "/" + ipc.substring(7);
		 * ipcList.add(ipc); } } }else { //get ipc Matcher m =
		 * this.hashPatternIPC.get(this.xmlDTD).matcher(this.metadata); while (m.find())
		 * { String ipc = m.group("ipc");
		 * 
		 * String section = "", class_ = "", subclass = "", maingroup = "", subgroup =
		 * ""; Matcher m1 = this.hashPatternIPCSection.get(this.xmlDTD).matcher(ipc); if
		 * (m1.find()) section = m1.group("section");
		 * 
		 * m1 = this.hashPatternIPCClass.get(this.xmlDTD).matcher(ipc); if (m1.find())
		 * class_ = m1.group("class");
		 * 
		 * m1 = this.hashPatternIPCSubClass.get(this.xmlDTD).matcher(ipc); if
		 * (m1.find()) subclass = m1.group("subclass");
		 * 
		 * m1 = this.hashPatternIPCMainGroup.get(this.xmlDTD).matcher(ipc); if
		 * (m1.find()) maingroup = m1.group("maingroup");
		 * 
		 * m1 = this.hashPatternIPCSubGroup.get(this.xmlDTD).matcher(ipc); if
		 * (m1.find()) subgroup = m1.group("subgroup");
		 * 
		 * ipcList.add(String.format("%s%s%s %s/%s", section, class_, subclass,
		 * maingroup, subgroup)); }
		 * 
		 * //get priority
		 * 
		 * //get application
		 * 
		 * //get related publication
		 * 
		 * } return String.format("%s\t%s", String.join(",", ipcList), this.metadata);
		 */
	}

	public String getYear() {
		return this.publicationdate.trim().substring(0, 4);
	}

	public String getDate() {
		return this.publicationdate.trim();
	}

}
