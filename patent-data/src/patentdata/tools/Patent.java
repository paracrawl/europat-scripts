package patentdata.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import patentdata.Common;
import patentdata.Config;
import patentdata.Config.ConfigInfo;
import patentdata.PatentData;
import patentdata.PatentData.INPUT_FORMAT;
import patentdata.PatentData.REF_TYPE;
import patentdata.PatentData.Service;

public class Patent {

	private Common common = new Common();
	private PatentData patentData = new PatentData();
	private ConfigInfo configInfo = null;

	private void initial() throws Exception {
		configInfo = new Config(common.getConfigPath())._config;
	}

	public Patent() throws Exception {
		initial();
	}
	// -------------------------------------------------------------------------------

	public void getPatentByMonth(String yearMonth) throws Exception {
		getPatentByMonth(yearMonth, new File(configInfo.WorkingDir, "search"));
	}

	public void getPatentByMonth(String yearMonth, File folderOutput) throws Exception {
		String resp = "", datePattern = "yyyyMM", service = Service.PUBLISHED.getServiceName();
		Date dateCrit = new SimpleDateFormat(datePattern).parse(yearMonth);
		Integer rangeBegin = 1;
		boolean isErr = false;
		do {
			resp = patentData.SearchPatentsByDate(service, new String[] {}, datePattern, dateCrit, null, rangeBegin,
					(rangeBegin += 100) - 1);
			if (resp.contains("<code>CLIENT.InvalidQuery</code>")) {
				isErr = true;
			} else {
				StringBuilder filename = new StringBuilder().append(yearMonth).append("_")
						.append(patentData.formatRange(rangeBegin - 100, rangeBegin - 1)).append(".xml");

				// prepare sub folder
				File folderTarget = new File(folderOutput, yearMonth);
				writeFile(folderTarget, filename.toString(), resp);
			}
		} while (!isErr);
	}

	public void getPatentIds(File folderInput, File folderOutput) throws Exception {
		try {
			File[] files = folderInput.listFiles(File::isDirectory);
			if (files.length == 0)
				files = new File[] { folderInput };
			for (File folderSub : files) {
				for (String inputPath : common.getFiles(folderSub.toString(), new String[] { "*.xml" }, false)) {
					System.out.println(String.format("reading... %s", inputPath));
					File fileInput = new File(inputPath);
					File folderTarget = new File(folderOutput,
							"ids_" + common.getBaseName(fileInput.getParent()) + ".txt");
					String contents = String.join("\n", Files.readAllLines(fileInput.toPath()));

					PatentHeader header = new PatentHeader();
					XMLEventReader eventReader = XMLInputFactory.newInstance()
							.createXMLEventReader(new ByteArrayInputStream(contents.getBytes()));
					// read the XML document
					while (eventReader.hasNext()) {
						XMLEvent event = eventReader.nextEvent();
						if (event.isStartElement()) {
							String localPart = event.asStartElement().getName().getLocalPart();
							switch (localPart) {
							case "document-id":
								header.type = getAttribute(event, "document-id-type");
								break;
							case "country":
								header.country = getCharacterData(event, eventReader);
								break;
							case "doc-number":
								header.number = getCharacterData(event, eventReader);
								break;
							case "kind":
								header.kind = getCharacterData(event, eventReader);
								break;
							}
						} else if (event.isEndElement()) {
							String localPart = event.asEndElement().getName().getLocalPart();
							if (localPart == ("publication-reference")) {
								// set document number
								StringBuffer sbDocNumber = new StringBuffer();
								if (header.type.equalsIgnoreCase(INPUT_FORMAT.docdb.toString())) {
									sbDocNumber.append(header.country).append(".").append(header.number).append(".")
											.append(header.kind);
								} else {
									sbDocNumber.append(header.number);
								}

								// save id as text file
								common.WriteFile(folderTarget.toString(), sbDocNumber.append("\n").toString(), true);

								// reset variable
								header = new PatentHeader();
							}
						}
					}
					System.out.println(String.format("saved : %s", folderTarget));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void getFamily(File fileInput) throws Exception {
		String[] files = fileInput.isDirectory()
				? common.getFiles(fileInput.toString(), new String[] { "*.txt" }, false)
				: fileInput.exists() ? new String[] { fileInput.toString() } : new String[] {};
		if (files.length > 0) {
			for (String idPath : files) {
				List<String> idList = common.readLines(idPath);
				for (String docno_info : idList) {
					getFamily(docno_info);
				}
			}
		}
	}

	public void getFamily(String docno) throws Exception {
		// get family by doc no
		String contents = patentData.ListFamily(REF_TYPE.publication.toString(), INPUT_FORMAT.docdb.toString(), docno,
				new String[] {});
		PatentHeader header = new PatentHeader();
		XMLEventReader eventReader = XMLInputFactory.newInstance()
				.createXMLEventReader(new ByteArrayInputStream(contents.getBytes()));
		// read the XML document
		boolean isMember = false;
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			if (event.isStartElement()) {
				String localPart = event.asStartElement().getName().getLocalPart();
				switch (localPart) {
				case "family-member":
					header.familyid = getAttribute(event, "family-id");
					isMember = true;
					break;
				case "document-id":
					if (isMember)
						header.type = getAttribute(event, "document-id-type");
					break;
				case "country":
					if (isMember)
						header.country = getCharacterData(event, eventReader);
					break;
				case "doc-number":
					if (isMember)
						header.number = getCharacterData(event, eventReader);
					break;
				case "kind":
					if (isMember)
						header.kind = getCharacterData(event, eventReader);
					break;
				case "date":
					if (isMember)
						header.date = getCharacterData(event, eventReader);
					break;
				}
			} else if (event.isEndElement()) {
				String localPart = event.asEndElement().getName().getLocalPart();
				if (localPart == ("publication-reference")) {
					// set document number
					StringBuffer sbDocNumber = new StringBuffer();
					if (header.type.equalsIgnoreCase(INPUT_FORMAT.docdb.toString())) {
						sbDocNumber.append(header.country).append(".").append(header.number).append(".")
								.append(header.kind);
					} else {
						sbDocNumber.append(header.number);
					}
					if (!StringUtils.isEmpty(header.date)) {
						sbDocNumber.append(".").append(header.date);
					}

					insertPatentHeader(header);

					// reset variable
					header = new PatentHeader();
				} else if (localPart == ("family-member"))
					isMember = false;
			}
		}
	}

	public void getPair(String sourcelang, String targetlang, File fileOutput) throws Exception {
		JSONArray jarr = listDocnoByPair(sourcelang, targetlang);
		StringBuilder content = new StringBuilder();
		for (int i = 0; i < jarr.length(); i++) {
			JSONArray jsources = jarr.getJSONObject(i).getJSONObject("source").getJSONArray("members");
			JSONArray jtargets = jarr.getJSONObject(i).getJSONObject("target").getJSONArray("members");
			for (int s = 0; s < jsources.length(); s++) {
				JSONObject smember = jsources.getJSONObject(s);
				for (int t = 0; t < jtargets.length(); t++) {
					JSONObject tmember = jtargets.getJSONObject(t);
					content.append(smember.getString("country_code")).append(".").append(smember.getString("doc_no"))
							.append(".").append(smember.getString("kind_code")).append("\t")
							.append(tmember.getString("country_code")).append(".").append(tmember.getString("doc_no"))
							.append(".").append(tmember.getString("kind_code")).append("\n");
				}
			}
		}
		common.WriteFile(fileOutput.toString(), content.toString());
		System.out.println(String.format("saved : %s", fileOutput));
	}

	public void insertPatentHeader(PatentHeader header) throws Exception {
		String tabname = "patent_header";
		if (!(StringUtils.isEmpty(header.familyid) || StringUtils.isEmpty(header.number)
				|| StringUtils.isEmpty(header.country))) {
			StringBuilder sbQuery = new StringBuilder("INSERT INTO ").append(tabname)
					.append(" (family_id, country_code, doc_no, kind_code)").append(" select '").append(header.familyid)
					.append("', '").append(header.country).append("', '").append(header.number).append("', '")
					.append(header.kind).append("' from dual").append(" WHERE NOT EXISTS (select 1 from ")
					.append(tabname).append(" where family_id='").append(header.familyid).append("'")
					.append(" and country_code='").append(header.country).append("'").append(" and doc_no='")
					.append(header.number).append("'").append(" and kind_code='").append(header.kind).append("');");// */
			System.out.println(sbQuery);
			Connection con = null;
			PreparedStatement pstmt = null;
			try {
				con = getDBConnection();
				pstmt = con.prepareStatement(sbQuery.toString());
				pstmt.executeUpdate(sbQuery.toString());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				pstmt.close();
				con.close();
			}
		}
	}

	public JSONArray listDocnoByPair(String sourcelang, String targetlang) throws Exception {
		JSONArray jarr = new JSONArray();
		String tabname = "patent_header";
		StringBuilder sbQuery = new StringBuilder("select * from ").append(tabname)
				.append(" where family_id in (select family_id from ").append(tabname)
				.append(" where country_code in (?,?)").append(" GROUP BY family_id HAVING COUNT(family_id) > 1)")
				.append(" and country_code in (?,?)").append(" order by family_id;");
		Connection con = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = getDBConnection();
			pstmt = con.prepareStatement(sbQuery.toString());
			int n = 1;
			pstmt.setString(n++, sourcelang);
			pstmt.setString(n++, targetlang);
			pstmt.setString(n++, sourcelang);
			pstmt.setString(n++, targetlang);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				// convert result set into object
				JSONObject jtmp = new JSONObject();
				jtmp.put("family_id", rs.getString("family_id"));
				jtmp.put("country_code", rs.getString("country_code"));
				jtmp.put("doc_no", rs.getString("doc_no"));
				jtmp.put("kind_code", rs.getString("kind_code"));

				boolean isNew = true;
				JSONObject jobj = new JSONObject();
				// find matched family_id
				for (int i = 0; i < jarr.length(); i++) {
					JSONObject jo = jarr.getJSONObject(i);
					if (jtmp.getString("family_id").equals(jo.getString("family_id"))) {
						isNew = false;
						jobj = jo;
						break;
					}
				}

				// initial list of source and target
				JSONObject jsource = jobj.has("source") ? jobj.getJSONObject("source") : new JSONObject() {
					{
						put("country", sourcelang);
						put("members", new JSONArray());
					}
				};
				JSONObject jtarget = jobj.has("target") ? jobj.getJSONObject("target") : new JSONObject() {
					{
						put("country", targetlang);
						put("members", new JSONArray());
					}
				};
				if (jtmp.getString("country_code").equals(jsource.getString("country"))) {
					jsource.getJSONArray("members").put(jtmp);
				} else if (jtmp.getString("country_code").equals(jtarget.getString("country"))) {
					jtarget.getJSONArray("members").put(jtmp);
				}
				jobj.put("family_id", jtmp.getString("family_id"));
				jobj.put("source", jsource);
				jobj.put("target", jtarget);
				if (isNew) {
					jarr.put(jobj);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			rs.close();
			pstmt.close();
			con.close();
		}
		return jarr;
	}

	public String getPatentData(String id, String endpoint, String[] constituents) throws Exception {
		return patentData.GetPatentsData(Service.PUBLISHED.getServiceName(), REF_TYPE.publication.toString(),
				INPUT_FORMAT.epodoc.toString(), new String[] { id }, endpoint, constituents);
	}

	class PatentHeader {
		String type = "", country = "", number = "", kind = "", date = "", familyid = "";
	}
	// -------------------------------------------------------------------------------

	private String getAttribute(XMLEvent event, String attName) throws XMLStreamException {
		String value = "";
		Iterator<Attribute> iter = event.asStartElement().getAttributes();
		while (iter.hasNext()) {
			Attribute attribute = iter.next();
			if (attribute.getName().toString().equals(attName)) {
				value = attribute.getValue();
				break;
			}
		}
		return value;
	}

	private String getCharacterData(XMLEvent event, XMLEventReader eventReader) throws XMLStreamException {
		event = eventReader.nextEvent();
		return (event instanceof Characters) ? event.asCharacters().getData() : "";
	}

	private void writeFile(File folder, String filename, String content) throws Exception {
		writeFile(folder, filename, content, false);
	}

	private void writeFile(File folder, String filename, String content, boolean isAppend) throws Exception {
		File file = new File(folder, filename);
		common.WriteFile(file.toString(), content, isAppend);
		System.out.println(String.format("saved : %s", file));
	}

	private Connection getDBConnection() throws Exception {
		Connection con = null;
		try {
			StringBuilder sbUrl = new StringBuilder("jdbc:mysql://").append(configInfo.DbHost).append(":")
					.append(configInfo.DbPort).append("/").append(configInfo.DbSchema);
			Class.forName("com.mysql.cj.jdbc.Driver");
			con = DriverManager.getConnection(sbUrl.toString(), configInfo.DbUser, configInfo.DbPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return con;
	}
}
