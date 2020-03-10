package patentdata.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.FilenameUtils;
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
		try {
			if (!folderSearch.exists())
				folderSearch.mkdirs();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Patent(String path) throws Exception {
		initial(path);
	}
	// -------------------------------------------------------------------------------

	public void getPatentByDate(String sDate) throws Exception {
		getPatentByDate(sDate, "yyyyMMdd");
	}

	public void getPatentByDate(String sDate, String pattern) throws Exception {
		DateFormat dateFormat = new SimpleDateFormat(pattern);
		String sFormat = "%d%02d%02d";
		if (sDate.matches("\\d{4}")) {
			int year = Integer.parseInt(sDate);
			for (int month = Calendar.JANUARY; month <= Calendar.DECEMBER; month++) {
				for (int day = 1; day <= getMonthDays(year, month); day++) {
					getPatentByDate(dateFormat.parse(String.format(sFormat, year, month + 1, day)), pattern,
							folderSearch);
				}
			}
		} else if (sDate.matches("\\d{6}")) {
			int year = Integer.parseInt(sDate.substring(0, 4));
			int month = Integer.parseInt(sDate.substring(4, 6)) - 1;
			if (month < 12) {
				for (int day = 1; day <= getMonthDays(year, month); day++) {
					getPatentByDate(dateFormat.parse(String.format(sFormat, year, month + 1, day)), pattern,
							folderSearch);
				}
			} else
				System.out.println("Month should between 01-12");
		} else if (sDate.matches("\\d{8}")) {
			int year = Integer.parseInt(sDate.substring(0, 4));
			int month = Integer.parseInt(sDate.substring(4, 6)) - 1;
			int day = Integer.parseInt(sDate.substring(6, 8));
			getPatentByDate(dateFormat.parse(String.format(sFormat, year, month + 1, day)), pattern, folderSearch);
		}
	}

	public void getPatentByDate(Date date, String pattern, File folderOutput) throws Exception {
		String resp = "", service = Service.PUBLISHED.getServiceName();
		Integer rangeBegin = 1;
		boolean isEnd = false;

		// first range 1-100
		resp = SearchPatents(service, new String[] {}, pattern, date, null, rangeBegin, (rangeBegin += 100) - 1, null);

		Integer totalResultCount = getTotalResultCount(resp);
		log.print(String.format("%s : total-result-count=%s", date, totalResultCount));
		if (totalResultCount > 2000) {
			// ignore first range
			for (String country : Arrays
					.asList(String.join(",", Files.readAllLines(Paths.get(configInfo.CCPath))).split(","))) {
				if (!StringUtils.isEmpty(country)) {
					totalResultCount = 0;
					rangeBegin = 1;
					isEnd = false;
					do {
						resp = SearchPatents(service, new String[] {}, pattern, date, null, rangeBegin,
								(rangeBegin += 100) - 1, country);

						if (totalResultCount == 0) {
							totalResultCount = getTotalResultCount(resp);
							log.print(String.format("publicationdate=%s : countrycode=%s : totalresultcount=%s",
									new SimpleDateFormat(pattern).format(date), country, totalResultCount));
						}

						if (resp.contains("<code>CLIENT.InvalidQuery</code>") || totalResultCount < rangeBegin) {
							isEnd = true;
						}
						writeSearchResult(date, pattern, folderOutput, resp, rangeBegin, country);
					} while (!isEnd);
				}
			}
		} else if (totalResultCount > 0) {
			// write the first range and move to next range
			writeSearchResult(date, pattern, folderOutput, resp, rangeBegin, null);
			do {
				resp = SearchPatents(service, new String[] {}, pattern, date, null, rangeBegin, (rangeBegin += 100) - 1,
						null);
				if (resp.contains("<code>CLIENT.InvalidQuery</code>") || totalResultCount < rangeBegin) {
					isEnd = true;
				}
				writeSearchResult(date, pattern, folderOutput, resp, rangeBegin, null);
			} while (!isEnd);
		} else {
			log.printErr("Something wrong! Please try again later");
			System.out.println(resp);
		}
	}

	public void getPatentIds(File filein, File fileout) throws Exception {
		try {
			try {
				File folderOutput = fileout.isDirectory() ? fileout : fileout.getParentFile();
				if (!folderOutput.exists())
					folderOutput.mkdirs();
			} catch (Exception e) {
				e.printStackTrace();
			}
			// list all subfolders. if there is no a subfolder, set input folder into array
			File[] files = filein.listFiles(File::isDirectory);
			if (files == null || files.length == 0)
				files = new File[] { filein };
			for (File file : files) {
				String[] arr = file.isFile() ? new String[] { file.toString() }
						: common.getFiles(file.toString(), new String[] { "*.xml" }, false);
				for (String inputPath : arr) {
					System.out.println(String.format("reading... %s", inputPath));
					File fileInput = new File(inputPath);
					File fileTarget = "txt".equalsIgnoreCase(FilenameUtils.getExtension(fileout.toString())) ? fileout
							: new File(fileout, "ids_"
									+ common.getBaseName(filein.isFile() ? fileInput.toString() : fileInput.getParent())
									+ ".txt");

					// extract docno from XML document
					String contents = String.join("\n", Files.readAllLines(fileInput.toPath()));
					PatentDocno oDocno = new PatentDocno();
					XMLEventReader eventReader = XMLInputFactory.newInstance()
							.createXMLEventReader(new ByteArrayInputStream(contents.getBytes()));
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
								// prepare document number
								StringBuffer sbDocNumber = new StringBuffer();
								if (oDocno.type.equalsIgnoreCase(INPUT_FORMAT.docdb.toString())) {
									sbDocNumber.append(oDocno.country).append(".").append(oDocno.number).append(".")
											.append(oDocno.kind);
								} else {
									sbDocNumber.append(oDocno.number);
								}

								// append docno into text file
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

	protected Integer getTotalResultCount(String content) {
		Integer result = 0;
		List<String> list = listStringPattern("total-result-count=\"([0-9]*)\"", content);
		if (list != null && list.size() > 0) {
			result = Integer.valueOf(list.get(0));
		}
		return result;
	}

	protected List<String> listStringPattern(String patternString, String text) {
		List<String> list = new ArrayList<String>();
		Matcher matcher = Pattern.compile(patternString).matcher(text);
		while (matcher.find()) {
			list.add(matcher.group(1));
		}
		return list;
	}

	protected void writeSearchResult(Date date, String pattern, File folderOutput, String content, Integer rangeBegin,
			String countryCode) throws Exception {
		String sDate = new SimpleDateFormat(pattern).format(date);
		StringBuilder filename = new StringBuilder().append(sDate).append("_");
		if (!StringUtils.isEmpty(countryCode))
			filename.append(countryCode).append("_");
		filename.append(formatRange(rangeBegin - 100, rangeBegin - 1)).append(".xml");

		// prepare sub folder
		File fileTarget = new File(new File(folderOutput, sDate), filename.toString());
		common.WriteFile(fileTarget.toString(), content, false);
		System.out.println(String.format("saved : %s", fileTarget));
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

	private int getMonthDays(int year, int month) throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, 1);
		return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	class PatentDocno {
		String type = "", country = "", number = "", kind = "", date = "", familyid = "";
	}

	private String TAB_DOCNO = "patent_docno";

	private Connection getDBConnection() throws Exception {
		Connection con = null;
		try {
			String sJdbc = configInfo.Jdbc;
			String sDriver = configInfo.DbDriver;;// ("com.mysql.cj.jdbc.Driver");
			StringBuilder sbUrl = new StringBuilder(sJdbc).append("://").append(configInfo.DbHost).append(":")
					.append(configInfo.DbPort).append("/").append(configInfo.DbSchema);
			Class.forName(sDriver);
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
