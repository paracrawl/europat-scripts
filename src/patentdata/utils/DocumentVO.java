package patentdata.utils;

import java.util.HashMap;

public class DocumentVO {
	String sLang;
	String sCountry;
	String sDocNumber;
	String skind;
	String sDatePubl;
	String sPatentId;
	String sId;
	String sFileName;
	String sDtdVersion;
	String sApplicationId;
	String sCorectionCode;
	String sDateApplication;
	String sIPC;
	HashMap<String, String> title;
	HashMap<String, String> abstracttext;
	HashMap<String, String> claim;
	HashMap<String, String> description;
	
	public String getsLang() {
		return sLang;
	}
	public void setsLang(String sLang) {
		this.sLang = sLang;
	}
	public String getsCountry() {
		return sCountry;
	}
	public void setsCountry(String sCountry) {
		this.sCountry = sCountry;
	}
	public String getsDocNumber() {
		return sDocNumber;
	}
	public void setsDocNumber(String sDocNumber) {
		this.sDocNumber = sDocNumber;
	}
	public String getSkind() {
		return skind;
	}
	public void setSkind(String skind) {
		this.skind = skind;
	}
	public String getsDatePubl() {
		return sDatePubl;
	}
	public void setsDatePubl(String sDatePubl) {
		this.sDatePubl = sDatePubl;
	}
	
	public String getsPatentId() {
		if (null != sCountry && null != sDocNumber && null != skind) {
			sPatentId = sCountry + "-" + sDocNumber + "-" + skind;
		}else if (null != sCountry && null != sDocNumber) {
			sPatentId = sCountry + "-" + sDocNumber;
			
		}
		return sPatentId;
	}
	
	public String getsId() {
		return sId;
	}
	public void setsId(String sId) {
		this.sId = sId;
	}
	public String getsFileName() {
		return sFileName;
	}
	public void setsFileName(String sFileName) {
		this.sFileName = sFileName;
	}
	public String getsDtdVersion() {
		return sDtdVersion;
	}
	public void setsDtdVersion(String sDtdVersion) {
		this.sDtdVersion = sDtdVersion;
	}
	public String getsApplicationId() {
		return sApplicationId;
	}
	public void setsApplicationId(String sApplicationId) {
		this.sApplicationId = sApplicationId;
	}
	public void setsPatentId(String sPatentId) {
		this.sPatentId = sPatentId;
	}
	public String getsCorectionCode() {
		return sCorectionCode;
	}
	public void setsCorectionCode(String sCorectionCode) {
		this.sCorectionCode = sCorectionCode;
	}
	public HashMap<String, String> getTitle() {
		return title;
	}
	public void setTitle(HashMap<String, String> title) {
		this.title = title;
	}
	public HashMap<String, String> getAbstracttext() {
		return abstracttext;
	}
	public void setAbstracttext(HashMap<String, String> abstracttext) {
		this.abstracttext = abstracttext;
	}
	public HashMap<String, String> getClaim() {
		return claim;
	}
	public void setClaim(HashMap<String, String> claim) {
		this.claim = claim;
	}
	public HashMap<String, String> getDescription() {
		return description;
	}
	public void setDescription(HashMap<String, String> description) {
		this.description = description;
	}
	public String getsDateApplication() {
		return sDateApplication;
	}
	public void setsDateApplication(String sDateApplication) {
		this.sDateApplication = sDateApplication;
	}
	public String getsIPC() {
		return sIPC;
	}
	public void setsIPC(String sIPC) {
		this.sIPC = sIPC;
	}
	
	
}
