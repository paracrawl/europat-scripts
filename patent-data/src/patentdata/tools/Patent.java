package patentdata.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import patentdata.utils.Config;
import patentdata.utils.Log;
import patentdata.utils.PatentData;

public class Patent extends PatentData {

	private File folderSearch = null;

	private void initial(String path) throws Exception {
		if (StringUtils.isEmpty(path))
			path = common.getConfigPath();
		config = new Config(path);
		configInfo = config._config;

		log = new Log(configInfo.WorkingDir);
		folderSearch = new File(configInfo.WorkingDir, "search");
		folderQuota = new File(configInfo.WorkingDir, "quota");
		try {
			if (!folderSearch.exists())
				folderSearch.mkdirs();
			if (!folderQuota.exists())
				folderQuota.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Patent(String path) throws Exception {
		initial(path);
	}
	// -------------------------------------------------------------------------------

	public void getPatentByDate(String sDate) throws Exception {
		getPatentByDate(sDate, "yyyyMMdd", folderSearch);
	}

	public void getPatentByDate(String sDate, String pattern, File folderOutput) throws Exception {
		String resp = "", service = Service.PUBLISHED.getServiceName();
		Date dateCrit = new SimpleDateFormat(pattern).parse(sDate);
		Integer rangeBegin = 1;
		boolean isErr = false;
		do {
			resp = SearchPatentsByDate(service, new String[] {}, pattern, dateCrit, null, rangeBegin,
					(rangeBegin += 100) - 1);
			if (resp.contains("<code>CLIENT.InvalidQuery</code>")) {
				isErr = true;
			}

			StringBuilder filename = new StringBuilder().append(sDate).append("_")
					.append(formatRange(rangeBegin - 100, rangeBegin - 1)).append(".xml");

			// prepare sub folder
			File folderTarget = new File(folderOutput, sDate);
			writeFile(folderTarget, filename.toString(), resp);
		} while (!isErr);
	}

	public void getPatentIds(File folderInput, File folderOutput) throws Exception {
		try {
			try {
				if (!folderOutput.exists())
					folderOutput.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
			}
			// list all subfolders. if there is no a subfolder, set input folder into array
			File[] files = folderInput.listFiles(File::isDirectory);
			if (files.length == 0)
				files = new File[] { folderInput };
			for (File folderSub : files) {
				for (String inputPath : common.getFiles(folderSub.toString(), new String[] { "*.xml" }, false)) {
					System.out.println(String.format("reading... %s", inputPath));
					File fileInput = new File(inputPath);
					File fileTarget = new File(folderOutput,
							"ids_" + common.getBaseName(fileInput.getParent()) + ".txt");
					String contents = String.join("\n", Files.readAllLines(fileInput.toPath()));

					PatentDocno oDocno = new PatentDocno();
					XMLEventReader eventReader = XMLInputFactory.newInstance()
							.createXMLEventReader(new ByteArrayInputStream(contents.getBytes()));
					// read the XML document
					while (eventReader.hasNext()) {
						XMLEvent event = eventReader.nextEvent();
						if (event.isStartElement()) {
							String localPart = event.asStartElement().getName().getLocalPart();
							switch (localPart) {
							case "document-id":
								oDocno.type = getAttribute(event, "document-id-type");
								break;
							case "country":
								oDocno.country = getCharacterData(event, eventReader);
								break;
							case "doc-number":
								oDocno.number = getCharacterData(event, eventReader);
								break;
							case "kind":
								oDocno.kind = getCharacterData(event, eventReader);
								break;
							}
						} else if (event.isEndElement()) {
							String localPart = event.asEndElement().getName().getLocalPart();
							if (localPart == ("publication-reference")) {
								// set document number
								StringBuffer sbDocNumber = new StringBuffer();
								if (oDocno.type.equalsIgnoreCase(INPUT_FORMAT.docdb.toString())) {
									sbDocNumber.append(oDocno.country).append(".").append(oDocno.number).append(".")
											.append(oDocno.kind);
								} else {
									sbDocNumber.append(oDocno.number);
								}

								// save id as text file
								common.WriteFile(fileTarget.toString(), sbDocNumber.append("\n").toString(), true);

								// reset variable
								oDocno = new PatentDocno();
							}
						}
					}
					System.out.println(String.format("saved : %s", fileTarget));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void getFamily(File fileInput) throws Exception {
		Connection con = null;
		try {
			con = getDBConnection();
			getFamily(fileInput, con);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(con);
		}

	}

	public void getFamily(File fileInput, Connection con) throws Exception {
		// if input is a directory then get all txt files
		// if input is a file then set it into array
		// if input is not exists then do nothing
		String[] files = fileInput.isDirectory()
				? common.getFiles(fileInput.toString(), new String[] { "*.txt" }, false)
				: fileInput.exists() ? new String[] { fileInput.toString() } : new String[] {};
		if (files.length > 0) {
			for (String idPath : files) {
				log.print(idPath);
				List<String> idList = common.readLines(idPath);
				for (String docno : idList) {
					getFamily(docno, con);
				}
			}
		}
	}

	public void getFamily(String docno) throws Exception {
		Connection con = null;
		try {
			con = getDBConnection();
			getFamily(docno, con);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close(con);
		}
	}

	public void getFamily(String docno, Connection con) throws Exception {
		// get family by doc no from APIs
		String contents = ListFamily(REF_TYPE.publication.toString(), INPUT_FORMAT.docdb.toString(), docno,
				new String[] {});
		try {
			PatentDocno oDocno = new PatentDocno();
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
						oDocno.familyid = getAttribute(event, "family-id");
						isMember = true;
						break;
					case "document-id":
						if (isMember)
							oDocno.type = getAttribute(event, "document-id-type");
						break;
					case "country":
						if (isMember)
							oDocno.country = getCharacterData(event, eventReader);
						break;
					case "doc-number":
						if (isMember)
							oDocno.number = getCharacterData(event, eventReader);
						break;
					case "kind":
						if (isMember)
							oDocno.kind = getCharacterData(event, eventReader);
						break;
					case "date":
						if (isMember)
							oDocno.date = getCharacterData(event, eventReader);
						break;
					}
				} else if (event.isEndElement()) {
					String localPart = event.asEndElement().getName().getLocalPart();
					if (localPart == ("publication-reference")) {
						// set document number
						StringBuffer sbDocNumber = new StringBuffer();
						if (oDocno.type.equalsIgnoreCase(INPUT_FORMAT.docdb.toString())) {
							sbDocNumber.append(oDocno.country).append(".").append(oDocno.number).append(".")
									.append(oDocno.kind);
						} else {
							sbDocNumber.append(oDocno.number);
						}
						if (!StringUtils.isEmpty(oDocno.date)) {
							sbDocNumber.append(".").append(oDocno.date);
						}

						try {// not break when Exception
							insertPatentHeader(oDocno, con);
						} catch (Exception e) {
							e.printStackTrace();
						}

						// reset variable
						oDocno = new PatentDocno();
					} else if (localPart == ("family-member"))
						isMember = false;
				}
			}
		} catch (Exception e) {
			log.printErr(new StringBuilder(e.getMessage()).append("\n").append(contents).toString());
		}
	}

	public void getPair(String sourcelang, String targetlang, File fileOutput) throws Exception {
		// list doc no from db
		JSONArray jarr = listDocnoByPair(sourcelang, targetlang);

		// pairing between sources and targets
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

	public void insertPatentHeader(PatentDocno oDocno, Connection con) throws Exception {
		// if duplicate familyid+countrycode+docno+kindcode then not insert
		if (!(StringUtils.isEmpty(oDocno.familyid) || StringUtils.isEmpty(oDocno.number)
				|| StringUtils.isEmpty(oDocno.country))) {
			if (null == con) {
				System.out.println(String.format("no connection : %s.%s.%s will be not added!", oDocno.country,
						oDocno.number, oDocno.kind));
			} else {
				StringBuilder sbQuery = new StringBuilder("INSERT INTO ").append(TAB_DOCNO)
						.append(" (family_id, country_code, doc_no, kind_code) select '").append(oDocno.familyid)
						.append("', '").append(oDocno.country).append("', '").append(oDocno.number).append("', '")
						.append(oDocno.kind).append("' from dual WHERE NOT EXISTS (select 1 from ").append(TAB_DOCNO)
						.append(" where family_id='").append(oDocno.familyid).append("'").append(" and country_code='")
						.append(oDocno.country).append("' and doc_no='").append(oDocno.number)
						.append("' and kind_code='").append(oDocno.kind).append("');");
				PreparedStatement pstmt = null;
				try {
					pstmt = con.prepareStatement(sbQuery.toString());
					pstmt.executeUpdate(sbQuery.toString());
					System.out.println(String.format("Family Id %s added : %s.%s.%s", oDocno.familyid, oDocno.country,
							oDocno.number, oDocno.kind));
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					close(pstmt);
				}
			}
		}
	}

	public JSONArray listDocnoByPair(String sourcelang, String targetlang) throws Exception {
		JSONArray jarr = new JSONArray();
		StringBuilder sbQuery = new StringBuilder("select * from ").append(TAB_DOCNO)
				.append(" where family_id in (select family_id from ").append(TAB_DOCNO)
				.append(" where country_code in (?,?) GROUP BY family_id HAVING COUNT(family_id) > 1)")
				.append(" and country_code in (?,?) order by family_id;");
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

				// add result set object into source/target members
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
			close(rs);
			close(pstmt);
			close(con);
		}
		return jarr;
	}

	public String getPatentData(String id, String endpoint, String[] constituents) throws Exception {
		return GetPatentsData(Service.PUBLISHED.getServiceName(), REF_TYPE.publication.toString(),
				INPUT_FORMAT.epodoc.toString(), new String[] { id }, endpoint, constituents);
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

	class PatentDocno {
		String type = "", country = "", number = "", kind = "", date = "", familyid = "";
	}

	private String TAB_DOCNO = "patent_docno";

	private Connection getDBConnection() throws Exception {
		Connection con = null;
		try {
			StringBuilder sbUrl = new StringBuilder(configInfo.Jdbc).append("://").append(configInfo.DbHost).append(":")
					.append(configInfo.DbPort).append("/").append(configInfo.DbSchema);
			Class.forName("org.mariadb.jdbc.Driver");// ("com.mysql.cj.jdbc.Driver");
			con = DriverManager.getConnection(sbUrl.toString(), configInfo.DbUser, configInfo.DbPassword);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return con;
	}

	private void close(ResultSet rs) throws Exception {
		if (rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
			}
		}
	}

	private void close(PreparedStatement pstmt) throws Exception {
		if (pstmt != null) {
			try {
				pstmt.close();
			} catch (SQLException e) {
			}
		}
	}

	private void close(Connection con) throws Exception {
		if (con != null) {
			try {
				con.close();
			} catch (SQLException e) {
			}
		}
	}
}
