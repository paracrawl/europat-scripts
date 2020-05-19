package patentdata.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PatentDocno {
	public String type = "";
	public String country = "";
	public String  number = "";
	public String  kind = "";
	public String  date = "";
	public String  familyid = "";
	
	public List<String> ipc = new ArrayList<String>();
	public List<String> ipcr = new ArrayList<String>();
	public List<String> applicants = new ArrayList<String>();

	public List<Map<String, String>> _title = new ArrayList<Map<String, String>>();
	public List<Map<String, String>> _claims = new ArrayList<Map<String, String>>();
	public List<Map<String, String>> _abstract = new ArrayList<Map<String, String>>();
	public List<Map<String, String>> _description = new ArrayList<Map<String, String>>();
	
	public String getDocno() {
		return country+"."+number+"."+kind;
	}


}
