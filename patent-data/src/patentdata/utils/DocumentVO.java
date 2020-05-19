package patentdata.utils;

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
	
	
}
