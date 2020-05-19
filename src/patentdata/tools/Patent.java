package patentdata.tools;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

import patentdata.utils.Config;
import patentdata.utils.Connector;
import patentdata.utils.Log;
import patentdata.utils.PatentData;
import patentdata.utils.PatentDocno;

public class Patent extends PatentData {

	Connection con = null;

	private void initial(String path) throws Exception {
		if (StringUtils.isEmpty(path))
			path = common.getConfigPath();
		config = new Config(path);
		configInfo = config._config;

		log = new Log(configInfo.WorkingDir);
		connector = new Connector(config);
	}

	public Patent() throws Exception {
		
	}

	public Patent(String path) throws Exception {
		initial(path);
	}
	// -------------------------------------------------------------------------------

	public void getPatentByDate(String sDate) throws Exception {
		getPatentByDate(sDate, "yyyyMMdd");
	}

	public void getPatentByDate(String sDate, String pattern) throws Exception {
		File folderSearch = new File(configInfo.WorkingDir, "search");
		try {
			if (!folderSearch.exists())
				folderSearch.mkdirs();
		} catch (Exception e) {
			log.printErr(e);
		}
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
				log.print("Month should between 01-12");
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
			log.printErr(resp);
		}
	}

	public void getPatentIds(File filein, File fileout) throws Exception {
		File folderOutput = fileout.isDirectory() ? fileout : fileout.getParentFile();
		if (null != folderOutput && !folderOutput.exists())
			folderOutput.mkdirs();

		// list all subfolders. if there is no a subfolder, set input folder into array
		File[] files = filein.listFiles(File::isDirectory);
		if (files == null || files.length == 0)
			files = new File[] { filein };
		for (File file : files) {
			String[] sources = file.isFile() ? new String[] { file.toString() }
					: common.getFiles(file.toString(), new String[] { "*.xml" }, false);
			for (String inputPath : sources) {
				System.out.println(String.format("reading... %s", inputPath));
				File fileInput = new File(inputPath);
				File fileTarget = "txt".equalsIgnoreCase(FilenameUtils.getExtension(fileout.toString())) ? fileout
						: new File(fileout, "ids_"
								+ common.getBaseName(filein.isFile() ? fileInput.toString() : fileInput.getParent())
								+ ".txt");

				try {
					// extract docno from XML document
					String contents = String.join("\n", Files.readAllLines(fileInput.toPath()));
					if (!StringUtils.isEmpty(contents)) {
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
									sbDocNumber.append(oDocno.country).append(".").append(oDocno.number).append(".")
											.append(oDocno.kind);

									// append docno into text file
									common.WriteFile(fileTarget.toString(), sbDocNumber.append("\n").toString(), true);

									// reset variable
									oDocno = new PatentDocno();
								}
							}
						}
					} else {
						log.print(String.format("empty file : %s", inputPath));
					}
				} catch (Exception e) {
					log.printErr(e, fileInput.toString());
				}
				System.out.println(String.format("saved : %s", fileTarget));
			}
		}
	}

	public void getFamily(File fileInput, File folderOutput) throws Exception {
		try {
			if (null != folderOutput && !folderOutput.exists())
				folderOutput.mkdirs();
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
						try {
							String[] arr = docno.split("\\.");
//							docno = arr[0]+"."+arr[1].replaceFirst("^0+(?!$)", "")+"."+arr[2];
							if (arr.length > 1 && "EP".equalsIgnoreCase(arr[0]) && arr[1].length() > 7)
								docno = arr[0] + "." + arr[1].substring(arr[1].length() - 7) + "." + arr[2];
							getFamily(docno, folderOutput);
						} catch (Exception e) {
							log.printErr(e, docno);
						}
					}
				}
			}
		} catch (Exception e) {
			log.printErr(e);
		}
	}

	public String getFamily(String docno, File folderOutput) throws Exception {
		String familyid = "";
		try {
			if (StringUtils.isEmpty(docno)) {
				log.print(String.format("No document number"));
				return "";
			} else {
				log.print(String.format("======================= START %s =======================", docno));
				// get family by doc no from APIs
				String contents = ListFamily(REF_TYPE.publication.toString(), INPUT_FORMAT.docdb.toString(), docno,
						new String[] {});

				String filename = "family_" + docno.replaceAll("\\.", "");

				File fileXml = new File(new File(folderOutput, "xml"), filename + ".xml");
				common.WriteFile(fileXml.toString(), contents);
				System.out.println(String.format("XML saved : %s", fileXml));

				// extract family members
				List<String> members = readFamily(contents);

				// append family members into text file
				File fileTxt = new File(new File(folderOutput, "txt"), filename + ".txt");
				common.WriteFile(fileTxt.toString(), String.join("\n", members), true);
				System.out.println(String.format("TXT saved : %s", fileTxt));
				log.print(String.format("======================= END %s =======================", docno));
			}
		} catch (Exception e) {
			log.printErr(e);
		} finally {
			connector.close(con);
		}
		return familyid;
	}

	public List<String> readFamily(String contents) throws Exception {
		List<String> members = new ArrayList<String>();
		String familyid = "";
		log.print(String.format("reading family members"));
		PatentDocno oDocno = new PatentDocno();
		XMLEventReader eventReader = XMLInputFactory.newInstance()
				.createXMLEventReader(new ByteArrayInputStream(contents.getBytes()));
		// read the XML document
		boolean isMember = false;
		String doctype = "";

		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			if (event.isStartElement()) {
				String localPart = event.asStartElement().getName().getLocalPart();
				switch (localPart) {
				case "family-member":
					oDocno.familyid = getAttribute(event, "family-id");
					if (StringUtils.isEmpty(familyid))
						familyid = oDocno.familyid;
					isMember = true;
					break;
				case "document-id":
					if (isMember)
						doctype = getAttribute(event, "document-id-type");
					break;
				case "country":
					if (isMember && INPUT_FORMAT.docdb.toString().equalsIgnoreCase(doctype))
						oDocno.country = getCharacterData(event, eventReader);
					break;
				case "doc-number":
					if (isMember && INPUT_FORMAT.docdb.toString().equalsIgnoreCase(doctype))
						oDocno.number = getCharacterData(event, eventReader);
					break;
				case "kind":
					if (isMember && INPUT_FORMAT.docdb.toString().equalsIgnoreCase(doctype))
						oDocno.kind = getCharacterData(event, eventReader);
					break;
				case "date":
					if (isMember && INPUT_FORMAT.docdb.toString().equalsIgnoreCase(doctype))
						oDocno.date = getCharacterData(event, eventReader);
					break;
				}
			} else if (event.isEndElement()) {
				String localPart = event.asEndElement().getName().getLocalPart();
				if (localPart == ("publication-reference")) {
					if (!StringUtils.isEmpty(oDocno.number)) {
						// set document number
						StringBuffer sbDocNumber = new StringBuffer(oDocno.familyid);
						sbDocNumber.append(".").append(oDocno.country).append(".").append(oDocno.number).append(".")
								.append(oDocno.kind);
						if (!StringUtils.isEmpty(oDocno.date)) {
							sbDocNumber.append(".").append(oDocno.date);
						}

						members.add(sbDocNumber.toString());
					}

					// reset variable
					oDocno = new PatentDocno();
				} else if (localPart == ("family-member")) {
					isMember = false;
				}
			}
		}
		return members;
	}

	public void insertFamily(File fileInput) throws Exception {
		try {
			// if input is a directory then get all txt & xml files
			// if input is a file then set it into array
			// if input is not exists then do nothing
			String[] files = fileInput.isDirectory()
					? common.getFiles(fileInput.toString(), new String[] { "*.txt", "*.xml" }, false)
					: fileInput.exists() ? new String[] { fileInput.toString() } : new String[] {};
			if (files.length > 0) {
				for (String familyPath : files) {
					log.print(String.format("reading : %s", familyPath));
					for (String docno : FilenameUtils.getExtension(familyPath).equalsIgnoreCase("xml")
							? readFamily(String.join("\n", Files.readAllLines(new File(familyPath).toPath())))
							: common.readLines(familyPath)) {
						try {
							String[] arr = docno.split("\\.");
							PatentDocno oDocno = new PatentDocno();
							oDocno.familyid = arr[0];
							oDocno.country = arr[1];
							oDocno.number = arr[2];
							oDocno.kind = arr[3];
							insertPatentHeader(oDocno);
						} catch (Exception e) {
							log.printErr(e, docno);
						}
					}
				}
			}
		} catch (Exception e) {
			log.printErr(e);
		} finally {
			connector.close(con);
		}
	}

	public void getPair(String sourcelang, String targetlang, File fileOutput) throws Exception {
		log.print(String.format("Pairing : %s, %s", sourcelang, targetlang));
		// list doc no from db
		List<Map<String, PatentDocno>> list = listDocnoByPair(sourcelang, targetlang);

		Instant start = Instant.now();
		log.print("WRITING :::::::::::::::::::::::::::: ");
		// pairing between sources and targets
		StringBuilder content = new StringBuilder();
		for (Map<String, PatentDocno> map : list) {
			PatentDocno oSource = map.get("source");
			PatentDocno oTarget = map.get("target");
			content.append(oSource.country).append(".").append(oSource.number).append(".").append(oSource.kind)
					.append("\t").append(oTarget.country).append(".").append(oTarget.number).append(".")
					.append(oTarget.kind).append("\n");
		}

		common.WriteFile(fileOutput.toString(), content.toString());

		Instant finish = Instant.now();
		log.print(String.format("Writing time %s Millis", Duration.between(start, finish).toMillis()));
		log.print((StringUtils.isEmpty(content)
				? String.format("No saved file : Not found %s match %s", sourcelang, targetlang)
				: String.format("saved : %s", fileOutput)));
	}

	public boolean findDocno(PatentDocno oDocno) throws Exception {
		boolean isFound = false;
		StringBuilder sbQuery = new StringBuilder("select * from ").append(TAB_DOCNO)
				.append(" where doc_no=? and country_code=? and kind_code=?;");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			con = connector.get(con);
			pstmt = con.prepareStatement(sbQuery.toString());
			pstmt.setString(1, oDocno.number);
			pstmt.setString(2, oDocno.country);
			pstmt.setString(3, oDocno.kind);
			rs = pstmt.executeQuery();
			if (rs.next()) {
				isFound = true;
			}
		} catch (Exception e) {
			log.printErr(e);
		} finally {
			connector.close(rs);
			connector.close(pstmt);
		}
		return isFound;
	}

	public void insertPatentHeader(PatentDocno oDocno) throws Exception {
		// if duplicate familyid+countrycode+docno+kindcode then not insert
		if (!(StringUtils.isEmpty(oDocno.familyid) || StringUtils.isEmpty(oDocno.number)
				|| StringUtils.isEmpty(oDocno.country))) {
			con = connector.get(con);
			StringBuilder sbQuery = new StringBuilder("INSERT INTO ").append(TAB_DOCNO).append(
					" (family_id, country_code, doc_no, kind_code, dt_created, dt_updated) values (?, ?, ?, ?, sysdate(), sysdate());");
			PreparedStatement pstmt = null;
			try {
				con = connector.get(con);
				pstmt = con.prepareStatement(sbQuery.toString());
				int n = 1;
				pstmt.setString(n++, oDocno.familyid);
				pstmt.setString(n++, oDocno.country);
				pstmt.setString(n++, oDocno.number);
				pstmt.setString(n++, oDocno.kind);
				pstmt.executeUpdate();
				log.print(String.format("Family Id %s added : %s.%s.%s", oDocno.familyid, oDocno.country, oDocno.number,
						oDocno.kind));
			} catch (Exception e) {
				log.writeErr(e);
			} finally {
				connector.close(pstmt);
			}
		}
	}

	public List<Map<String, PatentDocno>> listDocnoByPair(String sourcelang, String targetlang) throws Exception {
		List<Map<String, PatentDocno>> list = new ArrayList<Map<String, PatentDocno>>();
		StringBuilder sbQuery = new StringBuilder(" SELECT DISTINCT").append(
				" s.country_code source_country_code ,s.doc_no source_doc_no ,s.kind_code source_kind_code ,s.family_id source_family_id")
				.append(" ,t.country_code target_country_code ,t.doc_no target_doc_no ,t.kind_code target_kind_code ,t.family_id target_family_id")
				.append(" FROM ")//.append(TAB_DOCNO).append(" t inner join")
				.append(" (SELECT source.country_code ,source.doc_no ,source.kind_code ,source.family_id")
				.append(" FROM ").append(TAB_DOCNO).append(" source")
				.append(" where source.country_code = ? ORDER by source.family_id ) s")
				.append(" , (SELECT target.country_code , target.doc_no , target.kind_code , target.family_id")
				.append(" FROM ").append(TAB_DOCNO).append(" target")
				.append(" where target.country_code = ? ORDER by target.family_id ) t")
				.append(" where s.family_id = t.family_id;");
//				.append(" where t.country_code = ? ORDER by t.family_id;");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			log.print("RETRIEVING ::::::::::::::::::::::::: ");
			Instant start = Instant.now();

			con = connector.get(con);
			pstmt = con.prepareStatement(sbQuery.toString());
			int n = 1;
			pstmt.setString(n++, sourcelang);
			pstmt.setString(n++, targetlang);
			rs = pstmt.executeQuery();

			Instant finish = Instant.now();
			int nRow = 0;
			if (rs != null) {
				rs.last(); // moves cursor to the last row
				nRow = rs.getRow(); // get row id
				rs.beforeFirst();
			}
			log.print(String.format("Retrieving time %s Millis with %s rows",
					Duration.between(start, finish).toMillis(), nRow));
			log.print("MAPPING :::::::::::::::::::::::::::: ");
			start = Instant.now();

			while (rs.next()) {
				// convert result set into object
				PatentDocno oSource = new PatentDocno();
				PatentDocno oTarget = new PatentDocno();
				oSource.familyid = rs.getString("source_family_id");
				oSource.country = rs.getString("source_country_code");
				oSource.number = rs.getString("source_doc_no");
				oSource.kind = rs.getString("source_kind_code");
				oTarget.familyid = rs.getString("target_family_id");
				oTarget.country = rs.getString("target_country_code");
				oTarget.number = rs.getString("target_doc_no");
				oTarget.kind = rs.getString("target_kind_code");
				Map<String, PatentDocno> map = new HashMap<String, PatentDocno>();
				map.put("source", oSource);
				map.put("target", oTarget);
				list.add(map);
			}

			finish = Instant.now();
			log.print(String.format("Mapping time %s Millis", Duration.between(start, finish).toMillis()));
		} catch (Exception e) {
			log.printErr(e);
		} finally {
			connector.close(rs);
			connector.close(pstmt);
			connector.close(con);
		}
		return list;
	}

	protected Integer getTotalResultCount(String content) {
		Integer result = 0;
		List<String> list = listStringPattern("total-result-count=\"([0-9]*)\"", content);
		if (list != null && list.size() > 0) {
			result = Integer.valueOf(list.get(0));
		}
		return result;
	}

	public List<String> listStringPattern(String patternString, String text) {
		List<String> list = new ArrayList<String>();
		Matcher matcher = Pattern.compile(patternString).matcher(text);
		while (matcher.find()) {
			list.add(matcher.group(1));
		}
		return list;
	}

	public String getOptionValue(String option, String line) {
		String[] arr = line.trim().split("-" + option);
		return arr.length > 1 ? arr[1].trim().split(" ")[0].trim() : "";
	}

	protected String getOptionConfig(String line) throws Exception {
		String sConfigPath = "";
		if (line.matches(".*(\\-C).*")) {
			sConfigPath = getOptionValue("C", line);
			if (StringUtils.isEmpty(sConfigPath) || !sConfigPath.matches("(?i).*\\.json")) {
				throw new Exception("Expect a JSON file: -C \"config/path/patent.json\"");
			}
		}
		return sConfigPath;
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

	protected String getAttribute(XMLEvent event, String attName) throws XMLStreamException {
		String value = "";
		Iterator<?> iter = event.asStartElement().getAttributes();
		while (iter.hasNext()) {
			Attribute attribute = (Attribute) iter.next();
			if (attribute.getName().toString().equals(attName)) {
				value = attribute.getValue();
				break;
			}
		}
		return value;
	}

	protected String getCharacterData(XMLEvent event, XMLEventReader eventReader) throws XMLStreamException {
		event = eventReader.nextEvent();
		return (event instanceof Characters) ? event.asCharacters().getData() : "";
	}

	private int getMonthDays(int year, int month) throws Exception {
		Calendar calendar = Calendar.getInstance();
		calendar.set(year, month, 1);
		return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
	}

	protected String TAB_DOCNO = "patent_docno";
}
