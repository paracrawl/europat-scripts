package patentdata.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Array;
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

import org.apache.commons.io.Charsets;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import patentdata.utils.ConstantsVal;
import patentdata.utils.PatentData;
import patentdata.utils.PatentDocno;

public class Patent extends PatentData {

	Connection con = null;

	

	public Patent() throws Exception {
		
	}

	public Patent(String path) throws Exception {
		super(path);
	}
	
	public Patent(String path, boolean bVerbose) throws Exception {
		super(path, bVerbose);
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
			printLog.writeError("getPatentByDate", e);
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
				printLog.writeDebugLog("Month should between 01-12");
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
		printLog.writeDebugLog(String.format("%s : total-result-count=%s", date, totalResultCount));
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
							printLog.writeDebugLog(String.format("publicationdate=%s : countrycode=%s : totalresultcount=%s",
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
			printLog.writeError("Something wrong! Please try again later");
			printLog.writeError(resp);
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
				XMLEventReader eventReader = null;
				try {
					// extract docno from XML document
					String contents = String.join("\n", Files.readAllLines(fileInput.toPath()));
					if (!StringUtils.isEmpty(contents)) {
						PatentDocno oDocno = new PatentDocno();
						eventReader = XMLInputFactory.newInstance()
								.createXMLEventReader(new ByteArrayInputStream(contents.getBytes()));
						while (eventReader.hasNext()) {
							XMLEvent event = eventReader.nextEvent();
							if (event.isStartElement()) {
								String localPart = event.asStartElement().getName().getLocalPart();
								switch (localPart) {
								case "document-id":
									oDocno.type = getAttribute(event, "document-id-type");
									oDocno.lang = getAttribute(event, "lang");
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
											.append(oDocno.kind).append(".").append(oDocno.lang);

									// append docno into text file
//									if ("de".equals(oDocno.lang) || "fr".equals(oDocno.lang)) {
									if ("en".equalsIgnoreCase(oDocno.lang)) {
										common.WriteFile(fileTarget.toString(), sbDocNumber.append("\n").toString(), true);
									}
									// reset variable
									oDocno = new PatentDocno();
								}
							}
						}
					} else {
						printLog.writeDebugLog(String.format("empty file : %s", inputPath));
					}
				} catch (Exception e) {
					printLog.writeError("getPatentids:" + fileInput.toString(), e);
				}finally {
					if (null != eventReader) {
						try {
							eventReader.close();
						}catch (Exception e) {

						}
					}
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
					printLog.writeDebugLog(idPath);
					List<String> idList = common.readLines(idPath);
					for (String docno : idList) {
						try {
							String[] arr = docno.split("\\.");
//							docno = arr[0]+"."+arr[1].replaceFirst("^0+(?!$)", "")+"."+arr[2];
							if (arr.length > 1 && "EP".equalsIgnoreCase(arr[0]) && arr[1].length() > 7)
								docno = arr[0] + "." + arr[1].substring(arr[1].length() - 7) + "." + arr[2];
							getFamily(docno, folderOutput);
						} catch (Exception e) {
							printLog.writeError("getFamily:" + docno, e);
						}
					}
				}
			}
		} catch (Exception e) {
			printLog.writeError("getFamily", e);
		}
	}
	
	public void getFamilyDotFile(File fileInput, File folderOutput) throws Exception {
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
					printLog.writeDebugLog(idPath);
					List<String> idList = common.readLines(idPath);
					for (String docno : idList) {
						try {
							String[] arr = docno.split("\\.");
//							docno = arr[0]+"."+arr[1].replaceFirst("^0+(?!$)", "")+"."+arr[2];
							if (arr.length > 1 && "EP".equalsIgnoreCase(arr[0]) && arr[1].length() > 7)
								docno = arr[0] + "." + arr[1].substring(arr[1].length() - 7) + "." + arr[2];
							getFamily(docno, folderOutput);
						} catch (Exception e) {
							printLog.writeError("getFamily:" + docno, e);
						}
					}
				}
			}
		} catch (Exception e) {
			printLog.writeError("getFamily", e);
		}
	}

	public String getFamily(String docno, File folderOutput) throws Exception {
		String familyid = "";
	
		
		String filename =  "";
		File filetemp = null;
		if (!StringUtils.isEmpty(docno)) {
			filename = "family_" + docno.replaceAll("\\.", "");
			filetemp = new File(new File(folderOutput, "txt"), filename + ".txt");
		}else {
			return "";
		}

		try {
			if (common.IsExist(filetemp)){
				printLog.writeDebugLog("Existing file " + filetemp.getPath());
				return "";
			}else {
				printLog.writeDebugLog(String.format("======================= START %s =======================", docno));
				// get family by doc no from APIs
				String contents = ListFamily(REF_TYPE.publication.toString(), INPUT_FORMAT.docdb.toString(), docno,
						new String[] {});

				filename = "family_" + docno.replaceAll("\\.", "");

				File fileXml = new File(new File(folderOutput, "xml"), filename + ".xml");
				common.WriteFile(fileXml.toString(), contents);
				printLog.writeDebugLog(String.format("XML saved : %s", fileXml));

				// extract family members
				List<String> members = readFamily(contents);

				// append family members into text file
				File fileTxt = new File(new File(folderOutput, "txt"), filename + ".txt");
				common.WriteFile(fileTxt.toString(), String.join("\n", members), true);
				printLog.writeDebugLog(String.format("TXT saved : %s", fileTxt));
				printLog.writeDebugLog(String.format("======================= END %s =======================", docno));
			}
		} catch (Exception e) {
			printLog.writeError("getFamily", e);
		} finally {
			connector.close(con);
		}
		return familyid;
	}
	
	public String getContents(DocNoYear docNoYear, File folderOutput, String cContentTypes ) throws Exception {
		String familyid = "";
	
		
		String filename =  "";
		File filetemp = null;
		String sTempContentype = cContentTypes.replaceAll(",", "_");
		if (!StringUtils.isEmpty(docNoYear.getDocno())) {
			filename = docNoYear.getDocno().replaceAll("\\.", "") + "-" + docNoYear.getYear() + "-" + sTempContentype;
			filetemp = new File(new File(folderOutput, "xml/" + sTempContentype), filename + ".xml");
		}else {
			return "";
		}

		try {
			Long filesize = new Long(400);
			if (common.IsExist(filetemp) && 0 > filesize.compareTo(Files.size(Paths.get(filetemp.getPath())))){
				printLog.writeDebugLog("Existing file " + filetemp.getPath());
				return "";
			}else {
				printLog.writeDebugLog(String.format("======================= START %s =======================", docNoYear.getDocno()));
				// get family by doc no from APIs
				String contents = getPatentData(docNoYear.getDocno(), cContentTypes, new String[] {});

				filename = docNoYear.getDocno().replaceAll("\\.", "") + "-" + docNoYear.getYear() + "-" + sTempContentype ;

				File fileXml = new File(new File(folderOutput, "xml/" + sTempContentype), filename + ".xml");
				common.WriteFile(fileXml.toString(), contents);
				printLog.writeDebugLog(String.format("XML saved : %s", fileXml));

				if (contents.contains("<code>SERVER.EntityNotFound</code>")){
					filename = "ids-missing-" + docNoYear.getYear() + "-" + sTempContentype ;
					File idsMissingFile = new File(new File(folderOutput, "xml/" + sTempContentype), filename + ".tab");
					common.WriteFile(idsMissingFile.toString(), String.format("%s\t%s\n", docNoYear.getDocno(), docNoYear.getDatePublication()), true);
				}
//				// extract family members
//				List<String> members = readFamily(contents);
//
//				// append family members into text file
//				File fileTxt = new File(new File(folderOutput, "txt"), filename + ".txt");
//				common.WriteFile(fileTxt.toString(), String.join("\n", members), true);
//				printLog.writeDebugLog(String.format("TXT saved : %s", fileTxt));
				printLog.writeDebugLog(String.format("======================= END %s ======================= \n", docNoYear.getDocno()));
			}
		} catch (Exception e) {
			printLog.writeError("getContents", e);
		} finally {
			connector.close(con);
		}
		return docNoYear.getDocno();
	}

	public List<String> readFamily(String contents) throws Exception {
		List<String> members = new ArrayList<String>();
		String familyid = "";
		printLog.writeDebugLog(String.format("reading family members"));
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
			printLog.writeDebugLog(String.format("Start Inserted Family ID..."));

			String[] files = fileInput.isDirectory()
					? common.getFiles(fileInput.toString(), new String[] { "*.txt", "*.xml" }, false)
					: fileInput.exists() ? new String[] { fileInput.toString() } : new String[] {};
			if (files.length > 0) {
				int i = 0;
				boolean bExecuteFlag = false;
				List<PatentDocno> oDocnoList = new ArrayList<PatentDocno>();
				for (String familyPath : files) {
					i++;
//					log.print(String.format("reading : %s", familyPath));
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
							if (arr.length > 4) {
								oDocno.date = arr[4].substring(0, 8);
							}
							oDocnoList.add(oDocno);
							bExecuteFlag = true;
//							insertPatentHeader(oDocno);
						} catch (Exception e) {
							printLog.writeError("insertFamily", docno);
						}
					}
					
					if (i%200 == 0) {
						try {
							insertPatentHeader(oDocnoList);
						} catch (Exception e) {
							printLog.writeError("insertFamily : Number" + i + ": " + familyPath, e);
						}finally {
							bExecuteFlag = false;
							oDocnoList = new ArrayList<PatentDocno>();
							printLog.writeDebugLog(String.format("Done Imported : %s femilies.", i));
						}
					}
				}
				
				if (bExecuteFlag) {
					try {
						insertPatentHeader(oDocnoList);
					} catch (Exception e) {
						printLog.writeError("insertFamily", e);
					}finally {
						bExecuteFlag = false;
						oDocnoList = new ArrayList<PatentDocno>();
						printLog.writeDebugLog(String.format("Done Imported : %s femilies.", i));
					}
				}
				
			}
		} catch (Exception e) {
			printLog.writeError("insertFamily", e);
		} finally {
			connector.close(con);
		}
	}


	public void insertPatentHeader(PatentDocno oDocno) throws Exception {
		// if duplicate familyid+countrycode+docno+kindcode then not insert
		if (!(StringUtils.isEmpty(oDocno.familyid) || StringUtils.isEmpty(oDocno.number)
				|| StringUtils.isEmpty(oDocno.country))) {
			StringBuilder sbQuery = new StringBuilder("INSERT INTO ").append(TAB_DOCNO).append(
					" (family_id, country_code, doc_no, kind_code, dt_created, dt_updated, dt_publ, doc_id) values (?, ?, ?, ?, sysdate(), sysdate(), STR_TO_DATE(?,'%Y%m%d'), ?)\n");
			sbQuery.append("ON DUPLICATE KEY UPDATE ");
			sbQuery.append("dt_publ = STR_TO_DATE(?,'%Y%m%d'), \n");
			sbQuery.append("doc_id = ?, \n");
			sbQuery.append("dt_updated = sysdate() \n");
			sbQuery.append(";");
			PreparedStatement pstmt = null;
			try {
				con = connector.get(con);
				pstmt = con.prepareStatement(sbQuery.toString());
				int n = 1;
				pstmt.setString(n++, oDocno.familyid);
				pstmt.setString(n++, oDocno.country);
				pstmt.setString(n++, oDocno.number);
				pstmt.setString(n++, oDocno.kind);
				pstmt.setString(n++, oDocno.date);
				pstmt.setString(n++, String.format("%s-%s-%s", oDocno.country, oDocno.number, oDocno.kind));
				pstmt.setString(n++, oDocno.date);
				pstmt.setString(n++, String.format("%s-%s-%s", oDocno.country, oDocno.number, oDocno.kind));
				pstmt.executeUpdate();
				printLog.writeDebugLog(String.format("Family Id %s added : %s.%s.%s", oDocno.familyid, oDocno.country, oDocno.number,
						oDocno.kind));
			} catch (Exception e) {
				printLog.writeError("insertPatentHeader", e);
			} finally {
				connector.close(pstmt);
				connector.close(con);
			}
		}
	}
	
	public void insertPatentHeader(List<PatentDocno> oDocnoList) throws Exception {

		if (null != oDocnoList && !oDocnoList.isEmpty()) {
			StringBuilder sbQuery = new StringBuilder("INSERT INTO ").append(TAB_DOCNO).append(
					" (family_id, country_code, doc_no, kind_code, dt_created, dt_updated, dt_publ, doc_id) values (?, ?, ?, ?, sysdate(), sysdate(), STR_TO_DATE(?,'%Y%m%d'), ?)\n");
			sbQuery.append("ON DUPLICATE KEY UPDATE ");
			sbQuery.append("dt_publ = STR_TO_DATE(?,'%Y%m%d'), \n");
			sbQuery.append("doc_id = ?, \n");
			sbQuery.append("dt_updated = sysdate() \n");
			sbQuery.append(";");
			PreparedStatement pstmt = null;
			try {
				con = connector.get(con);
				pstmt = con.prepareStatement(sbQuery.toString());
				boolean bExecuteFlag = false;
				PatentDocno oDocno = null;
				for (int i = 0; i < oDocnoList.size(); i++) {
					oDocno = new PatentDocno();
					oDocno = oDocnoList.get(i);
					int n = 1;
					pstmt.setString(n++, oDocno.familyid);
					pstmt.setString(n++, oDocno.country);
					pstmt.setString(n++, oDocno.number);
					pstmt.setString(n++, oDocno.kind);
					pstmt.setString(n++, oDocno.date);
					pstmt.setString(n++, String.format("%s-%s-%s", oDocno.country, oDocno.number, oDocno.kind));
					pstmt.setString(n++, oDocno.date);
					pstmt.setString(n++, String.format("%s-%s-%s", oDocno.country, oDocno.number, oDocno.kind));
//					pstmt.executeUpdate();
					pstmt.addBatch();
					bExecuteFlag = true;
					
					try {
						if (i%50 == 0) {
							printLog.writeDebugLog("Start i:" + i);
							pstmt.executeBatch();
							con.commit();
							bExecuteFlag = false;
							printLog.writeDebugLog("End i:" + i);
						}
					}catch (Exception e) {
						printLog.writeError("insertPatentHeader", e);
					}finally {
						bExecuteFlag = false;
					}
				}
				if (bExecuteFlag) {
					pstmt.executeBatch();
					con.commit();
				}
				
			} catch (Exception e) {
				printLog.writeError("insertPatentHeader", e);
			} finally {
				connector.close(pstmt);
				connector.close(con);
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
			printLog.writeDebugLog("RETRIEVING ::::::::::::::::::::::::: ");
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
			printLog.writeDebugLog(String.format("Retrieving time %s Millis with %s rows",
					Duration.between(start, finish).toMillis(), nRow));
			printLog.writeDebugLog("MAPPING :::::::::::::::::::::::::::: ");
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
			printLog.writeDebugLog(String.format("Mapping time %s Millis", Duration.between(start, finish).toMillis()));
		} catch (Exception e) {
			printLog.writeError("listDocnoByPair", e);
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
		String[] arr = line.trim().split("-" + option + " ");
		return arr.length > 1 ? arr[1].trim().split(" ")[0].trim() : "";
	}
	
	public String getMethodeValue(String option, String line) {
		String[] arr = line.trim().split("--" + option + " ");
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
		printLog.writeDebugLog(String.format("saved : %s", fileTarget));
	}

	public String getPatentData(String id, String endpoint, String[] constituents) throws Exception {
		return GetPatentsData(Service.PUBLISHED.getServiceName(), REF_TYPE.publication.toString(),
				INPUT_FORMAT.docdb.toString(), new String[] { id }, endpoint, constituents);
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

//	protected String TAB_DOCNO = "patent_docno_sample";
	protected String TAB_DOCNO = "patent_docno";
	
	private String GetTableName(String sSource, String sLang, String sCategory) {
		String sTblname = "";
		sLang = sLang.toLowerCase();
		sSource = sSource.toLowerCase();
		sCategory = sCategory.toLowerCase();
		
		if (ConstantsVal.LIST_SOURCE.contains(sSource) && ConstantsVal.LIST_LANG.contains(sLang)
				&& ConstantsVal.LIST_CATEGORY.contains(sCategory)) {
			
			if (ConstantsVal.ABSTRACT.equals(sCategory)) {
				sTblname = ConstantsVal.TABLE_ABSTRACT.replace("@source@", sSource).replace("@lang@", sLang);
			}else if (ConstantsVal.CLAIM.equals(sCategory)) {
				sTblname = ConstantsVal.TABLE_CLAIM.replace("@source@", sSource).replace("@lang@", sLang);
			}else if (ConstantsVal.DSCP.equals(sCategory)) {
				sTblname = ConstantsVal.TABLE_dscp.replace("@source@", sSource).replace("@lang@", sLang);
//			}else if (ConstantsVal.METADATA.equals(sCategory)) {
//				sTblname = ConstantsVal.TABLE_METADATA.replace("@source@", sSource).replace("@lang@", sLang);
			}else if (ConstantsVal.TITLE.equals(sCategory) || ConstantsVal.METADATA.equals(sCategory) || ConstantsVal.PATENT_ID.equals(sCategory)) {
				sTblname = ConstantsVal.TABLE_TITLE.replace("@source@", sSource).replace("@lang@", sLang);
			}
		}
		
		return sTblname;
	}
	
	public void insertPatentDataToDB(String sTabFilePath, String sSource, String sLang, String sCategory) throws Exception {
		insertPatentDataToDB(sTabFilePath, sSource, sLang, sCategory, 0, 0);
	}
	
	public void insertPatentDataToDB(String sTabFilePath, String sSource, String sLang, String sCategory, int iStart, int iEnd) throws Exception {
		printLog.writeDebugLog("Start insertPatentDataToDB...");
		String sTblName = GetTableName(sSource, sLang, sCategory);
		if (!StringUtils.isEmpty(sTabFilePath) && !StringUtils.isEmpty(sTblName)) {
			printLog.writeDebugLog(String.format("Start load from file %s into %s.", sTblName, sTabFilePath));
			
			
			PreparedStatement pstmt = null;
			FileReader reader = null;
			BufferedReader bufferedReader = null;
			int i = 0;
			StringBuilder sbQuery = new StringBuilder();
			try {
				System.out.println(sTabFilePath);
				reader = new FileReader(sTabFilePath);
		        bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(sTabFilePath), "UTF-8" ));

				String sLine = "";
				sbQuery = new StringBuilder();
				
				sbQuery.append("\n insert into ").append(sTblName).append("  ");
				sbQuery.append("\n (id, country_cd, doc_no , kind_cd, doc_id, publ_dt, dt_publ, dt_create,  dt_update)  ");
				sbQuery.append("\n select tmp.*   ");
				sbQuery.append("\n from ").append(sTblName).append(" rtb  ");
				sbQuery.append("\n right join (  ");
				sbQuery.append("\n select ");
				sbQuery.append("\n    uuid() id");
				sbQuery.append("\n    , SUBSTRING_INDEX(?,'-',1) country ");
				sbQuery.append("\n    , SUBSTRING_INDEX(SUBSTRING_INDEX(?,'-',2),'-',-1) doc_no ");
				sbQuery.append("\n    , case " + 
						"\n			when length(SUBSTRING_INDEX(?,'-',-1)) > 2 then 'A'\n" + 
						"\n		    else SUBSTRING_INDEX(?,'-',-1)\n" + 
						"\n		end kind ");
				sbQuery.append("\n    , ? doc_id");
				sbQuery.append("\n    , ? publ_dt");
				sbQuery.append("\n    , STR_TO_DATE( ?,'%Y%m%d') dt_publ ");
				sbQuery.append("\n 	  , sysdate() dt_create");
				sbQuery.append("\n    , sysdate() dt_update");
				sbQuery.append("\n from dual) tmp   ");
				sbQuery.append("\n 	on rtb.doc_id = tmp.doc_id  ");
				sbQuery.append("\n 	and rtb.publ_dt  = tmp.publ_dt  ");
				sbQuery.append("\n where rtb.doc_id is null;  ");
				
				printLog.writeDebugLog(sbQuery.toString());
				con = connector.get(con);
				con.setAutoCommit(false);
				pstmt = con.prepareStatement(sbQuery.toString());
				boolean bExecutedFlag = false;
				if (iEnd == 0 ) {
					iEnd = 999999999;
				}
				while ((sLine = bufferedReader.readLine()) != null ) {
	            	 i++;
	            	 if (i >= iStart && i <= iEnd) {
		            	String[] arrOfStr = sLine.split("\t", 3); 
						
		            	if (null != arrOfStr && arrOfStr.length == 3) {
							
							pstmt.setString(1, arrOfStr[0]);
							pstmt.setString(2, arrOfStr[0]);
							pstmt.setString(3, arrOfStr[0]);
							pstmt.setString(4, arrOfStr[0]);
							pstmt.setString(5, arrOfStr[0]);
							pstmt.setString(6, arrOfStr[1]);
							pstmt.setString(7, arrOfStr[1]);
							pstmt.addBatch();
							bExecutedFlag = false;
							if (i % 500 == 0) {
								pstmt.executeBatch();
								 con.commit();
								bExecutedFlag = true;
							}
							
							if (i %1000 == 0)
								printLog.writeDebugLog(String.format(i + "\t Table File has been loaded into %s added : %s.", sTblName, sTabFilePath));
						}
	            	 }else if(i > iEnd){
							break;
					}
				}
				if (!bExecutedFlag) {
					pstmt.executeBatch();
					con.commit();
				}
			} catch (Exception e) {
				e.printStackTrace();
				con.rollback();
				printLog.writeError(i + " " + sbQuery.toString() + " \n sTabFilePath");
				printLog.writeError("insertPatentDataToDB", e);
			} finally {
				try {
					connector.close(pstmt);
					connector.close(con);
					reader.close();
					bufferedReader.close();
				}catch (Exception e) {
					
				}
			}
		}
		
		printLog.writeDebugLog("End insertPatentDataToDB.");
	}
	
	public void ExportPairIDS(String sSourceLang, String sTargetLang, String sYear, String sCategory, String sOutputPath) throws Exception {
		ExportPairIDS( sSourceLang, sTargetLang, sYear, sCategory, sOutputPath, null);
	}
	
	public void ExportPairIDS(String sSourceLang, String sTargetLang, String sYear, String sCategory, String sOutputPath, String sEndYear) throws Exception {
		printLog.writeDebugLog("Start : ExportPairIDS...");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
	    List<String> lSourceID	= new ArrayList<String>();
	    HashMap<String, String> hSourceTargetIDMap = new HashMap<String, String>();
	    HashMap<String, List<String>> hTargetIDsMap = new HashMap<String, List<String>>();
	    
		if (!StringUtils.isEmpty(sCategory) && !StringUtils.isEmpty(sSourceLang) && !StringUtils.isEmpty(sTargetLang) 
				&& !StringUtils.isEmpty(sYear) && !StringUtils.isEmpty(sOutputPath)) {
			
			String sTableSource = GetTableName("ep", sSourceLang, sCategory);
			String sTableTarget = GetTableName("us", sTargetLang, sCategory);
			String sFileName  = GetOutputFileName(sSourceLang, sTargetLang, sYear, sCategory);

			StringBuilder sbQuery = new StringBuilder();
			sbQuery.append("select temp.doc_id \n");
			sbQuery.append(", temp.dt_publ\n");
			sbQuery.append(", temp.publ_dt\n");
			sbQuery.append(", us_title.doc_id us_doc_id\n");
			sbQuery.append(", us_title.dt_publ us_dt_publ\n");
			sbQuery.append(", us_title.publ_dt us_publ_dt\n");
			sbQuery.append("from patent_docno pd \n");
			sbQuery.append("inner join (\n");
			sbQuery.append("select dpte.doc_no , docno.family_id, dpte.doc_id, dpte.dt_publ, dpte.publ_dt \n");
			sbQuery.append("from ").append(sTableSource).append(" dpte\n");
			sbQuery.append("inner join patent_docno docno on docno.country_code = dpte.country_cd\n");
			sbQuery.append("          and docno.doc_no   = dpte.doc_no \n");
			sbQuery.append("          and docno.kind_code  = dpte.kind_cd\n");
			sbQuery.append("where dpte.dt_publ  between STR_TO_DATE(concat(?, '0101'),'%Y%m%d') and STR_TO_DATE( concat(?, '1231'),'%Y%m%d')\n");
			sbQuery.append(") \n");
			sbQuery.append("temp on temp.family_id = pd.family_id and pd.country_code = 'US'\n");
			sbQuery.append("inner join  ").append(sTableTarget).append("  us_title on us_title.country_cd = pd.country_code\n");
			sbQuery.append("            and us_title.kind_cd = pd.kind_code\n");
			sbQuery.append("            and us_title.doc_no  = lpad(pd.doc_no, 8, 0) \n");
			if (!StringUtils.isEmpty(sEndYear)) {
				sbQuery.append("            and us_title.dt_publ between STR_TO_DATE(concat(?, '0101'),'%Y%m%d') and STR_TO_DATE( concat(?, '1231'),'%Y%m%d')\n");
			}
			sbQuery.append("order by temp.doc_id, temp.dt_publ \n");
			sbQuery.append(";\n");
			
			printLog.writeDebugLog(sbQuery.toString());
			int rowNum = 1;
			try {
				con = connector.get(con);
				pstmt = con.prepareStatement(sbQuery.toString());
				pstmt.setString(1, sYear);
				pstmt.setString(2, sYear);
				if (!StringUtils.isEmpty(sEndYear)) {
					pstmt.setString(3, sEndYear);
					pstmt.setString(4, sEndYear);
				}
				rs = pstmt.executeQuery();
			    
			    while(rs.next()) {
			    	String sSourceID = "";
			    	String sTargetID = "";
		    		java.sql.Date dtPubl = rs.getDate("us_dt_publ");	
			    	sSourceID = (String) rs.getString("doc_id"); 
			    	sTargetID = (String) rs.getString("us_doc_id");
			    	hSourceTargetIDMap.put(String.valueOf(rowNum), sSourceID + "|" + sTargetID);
			    	lSourceID.add(sSourceID);
			    	if (null != dtPubl) {
				    	Calendar cal = Calendar.getInstance();
				    	cal.setTime(dtPubl);
				    	String	 year = String.valueOf(cal.get(Calendar.YEAR));
				    	
				    	if (null != hTargetIDsMap.get(year)){
				    		hTargetIDsMap.get(year).add(sTargetID);
				    	}else {
				    		List<String> tempList = new ArrayList<String>();
				    		tempList.add(sTargetID);
				    		hTargetIDsMap.put(year, tempList);
				    	}
			    	}
			    	rowNum++;
//			    	printLog.writeDebugLog("Row number : " + rowNum++ + ":" + sSourceID + ":" + sTargetID + ":" + dtPubl.toString());
			    }
//			    printLog.writeDebugLog(hTargetIDsMap.get("2019").size() + "");
//				printLog.writeDebugLog("End Exprot");
	
			} catch (Exception e) {
				printLog.writeError("ExportPairIDS", e);
			} finally {
				connector.close(pstmt);
				connector.close(con);
			}
			
				printLog.writeDebugLog("Row number : " + rowNum);
				// Get Contents of Patents Source
				HashMap<String, String> hSourceContent = new HashMap<String, String>();
				printLog.writeDebugLog("Start Get Source Content:");
				hSourceContent = getContentsFromFile(lSourceID, config._config.SourceFilePath, sSourceLang, sCategory, sYear, ConstantsVal.SOURCE_EUROPAT);
		//			for(Map.Entry<String, String> m: hSourceContent.entrySet()) {
		//				log.print(m.getKey() + ": value: " + m.getValue());
		//			}
				printLog.writeDebugLog("End Get Source Content:");
				// Get Contents of Patent Target
				HashMap<String, String> hTargetContent = new HashMap<String, String>();
				hTargetContent = getContentsFromFileForTarget(hTargetIDsMap, config._config.TargetFilePath, sTargetLang, sCategory, sYear, ConstantsVal.SOURCE_USPTO);
		
		//			for(Map.Entry<String, String> m: hTargetContent.entrySet()) {
		//				log.print(m.getKey() + ": value: " + m.getValue());
		//			}
				FileOutputStream outputStream = null;
				try {
					printLog.writeDebugLog("Start Write Output file");
					printLog.writeDebugLog("Row Number: " + rowNum);
			    	File file = new File(sOutputPath.concat("/").concat(sFileName));
				    if (!file.exists()) {
				    	file.getParentFile().mkdirs();
				    } 
				    printLog.writeDebugLog("Output File Path: " + file.getPath());
				    outputStream = new FileOutputStream(file, false);
				    for (int i = 1; i < rowNum; i++) {
				    	
				    	String[] temp = hSourceTargetIDMap.get(String.valueOf(i)).split("\\|");
				    	String sTempSource = temp[0];
				    	String StempTarget = temp[1];
				    	String sSourceContent = hSourceContent.get(sTempSource);
				    	String sTargetContent = hTargetContent.get(StempTarget);
		//			    	String stempLine2 = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
		//			    	log.print(stempLine2);
				    	if (!StringUtils.isEmpty(sSourceContent) && !StringUtils.isEmpty(sTargetContent)) {
				    		String stempLine = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
				    		outputStream.write(stempLine.getBytes(Charsets.toCharset("utf-8")));
				    		outputStream.flush();
				    	}
				    }
				}catch(Exception e) {
					printLog.writeError("ExportPairIDS",e);
				}finally {
					try {
						outputStream.close();
					}catch (Exception e) {
						
					}
				}
			
			
			
		}else if(StringUtils.isEmpty(sCategory)) {
			ExportPairOnlyIDS(sSourceLang, sTargetLang, sYear, sCategory, sOutputPath, sEndYear);
		}else {
			printLog.writeDebugLog("Please check input parameters, some value is empty or null");
		}
		
		printLog.writeDebugLog("Done");
	}
	
	public void ExportPairOnlyIDS(String sSourceLang, String sTargetLang, String sYear, String sCategory, String sOutputPath, String sEndYear) throws Exception {
		printLog.writeDebugLog("Start : ExportPairIDS...");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
	    List<String> lSourceID	= new ArrayList<String>();
	    HashMap<String, String> hSourceTargetIDMap = new HashMap<String, String>();
	    HashMap<String, List<String>> hTargetIDsMap = new HashMap<String, List<String>>();
	    
		if (!StringUtils.isEmpty(sSourceLang) && !StringUtils.isEmpty(sTargetLang) && !StringUtils.isEmpty(sYear) && !StringUtils.isEmpty(sOutputPath)) {
			
			String sTableSource = GetTableName("ep", sSourceLang, sCategory);
//			String sTableTarget = GetTableName("us", sTargetLang, sCategory);
			String sFileName  = GetOutputFileName(sSourceLang, sTargetLang, sYear, sCategory);
			String sStart = sYear + "-01-01";
			String sEnd = sYear + "-12-31";
			StringBuilder sbQuery = new StringBuilder();
//			sbQuery.append("select temp.family_id, temp.doc_id, temp.dt_publ, \n");
//			sbQuery.append("case when pd.doc_id  is null or pd.doc_id = '' THEN \n");
//			sbQuery.append("       concat(pd.country_code, '-', pd.doc_no, '-', pd.kind_code) \n");
//			sbQuery.append("    else \n");
//			sbQuery.append("      pd.doc_id \n");
//			sbQuery.append("   end tg_doc_id \n");
//			sbQuery.append(", pd.dt_publ tg_dt_publ \n");
//			sbQuery.append("from ( \n");
//			sbQuery.append(" select case when pd1.doc_id  is null or pd1.doc_id = '' THEN \n");
//			sbQuery.append("       concat(title.country_cd, '-', title.doc_no, '-', title.kind_cd) \n");
//			sbQuery.append("    else \n");
//			sbQuery.append("      pd1.doc_id \n");
//			sbQuery.append("   end doc_id \n");
//			sbQuery.append("   , title.dt_publ, pd1.family_id \n");
//			sbQuery.append(" from ").append("t_ep_en_pt_title").append(" title \n");
//			sbQuery.append(" inner join patent_docno pd1  on pd1.country_code = ? \n");
//			sbQuery.append("     and pd1.country_code = title.country_cd \n");
//			sbQuery.append("           and pd1.doc_no     = title.doc_no  \n");
//			sbQuery.append("           and pd1.kind_code  = title.kind_cd \n");
//			sbQuery.append(" where date(title.dt_publ)  between '").append(sStart).append("' and '").append(sEnd).append("' \n");
//			sbQuery.append(" union  \n");
//			sbQuery.append(" select case when pd1.doc_id  is null or pd1.doc_id = '' THEN \n");
//			sbQuery.append("       concat(title.country_cd, '-', title.doc_no, '-', title.kind_cd) \n");
//			sbQuery.append("    else \n");
//			sbQuery.append("      pd1.doc_id \n");
//			sbQuery.append("   end doc_id \n");
//			sbQuery.append("  , title.dt_publ, pd1.family_id  \n");
//			sbQuery.append(" from ").append("t_us_en_pt_title").append(" title \n");
//			sbQuery.append(" inner join patent_docno pd1  on pd1.country_code = ? \n");
//			sbQuery.append("     and pd1.country_code = title.country_cd \n");
//			sbQuery.append("           and LPAD(pd1.doc_no, 8, 0)   = title.doc_no  \n");
//			sbQuery.append("           and pd1.kind_code  = title.kind_cd \n");
//			sbQuery.append(" where date(title.dt_publ)  between '").append(sStart).append("' and '").append(sEnd).append("' \n");
//			sbQuery.append(") temp \n");
//			sbQuery.append("inner join patent_docno pd on temp.family_id = pd.family_id and pd.country_code = ? \n");
//			sbQuery.append("order by temp.dt_publ asc \n");
//			sbQuery.append("; \n");
			
			sbQuery.append("select temp.family_id, temp.doc_id, temp.dt_publ, \n");
			sbQuery.append("case when pd.doc_id  is null or pd.doc_id = '' THEN \n");
			sbQuery.append("       concat(pd.country_code, '-', pd.doc_no, '-', pd.kind_code) \n");
			sbQuery.append("    else \n");
			sbQuery.append("      pd.doc_id \n");
			sbQuery.append("   end tg_doc_id \n");
			sbQuery.append(", pd.dt_publ tg_dt_publ \n");
			sbQuery.append("from\n");
			sbQuery.append("	patent_docno pd\n");
			sbQuery.append("inner join (\n");
			sbQuery.append("		select * \n");
			sbQuery.append("		from	patent_docno pd2\n");
			sbQuery.append("		where\n");
//			sbQuery.append(" 		where date(pd2.dt_publ)  between '").append(sStart).append("' and '").append(sEnd).append("' \n");
//			sbQuery.append("		pd2.dt_publ between STR_TO_DATE(concat(?, '0101'), '%Y%m%d') and STR_TO_DATE( concat(?, '1231'), '%Y%m%d')\n");
			sbQuery.append("		pd2.dt_publ between STR_TO_DATE(concat('").append(sStart).append("'), '%Y-%m-%d') and STR_TO_DATE( concat('").append(sEnd).append("'), '%Y-%m-%d')\n");
			sbQuery.append("		and pd2.country_code in (?, ?)\n");
			sbQuery.append("		\n");
			sbQuery.append("	) temp on 	temp.family_id = pd.family_id\n");
			sbQuery.append("where pd.country_code = ? \n");
			
			printLog.writeDebugLog(sbQuery.toString());
			int rowNum = 1;
			try {
				con = connector.get(con);
				pstmt = con.prepareStatement(sbQuery.toString());
				if ("EN".equalsIgnoreCase(sSourceLang)){
					pstmt.setString(1, "EP");
				}else {
					pstmt.setString(1, sSourceLang.toUpperCase());
				}
				if ("EN".equalsIgnoreCase(sSourceLang)){
					pstmt.setString(2, "US");
				}else {
					pstmt.setString(2, "UNKNOWN");
				}
				
				pstmt.setString(3, sTargetLang.toUpperCase());
				
				rs = pstmt.executeQuery();
			    
			    while(rs.next()) {
			    	String sSourceID = "";
			    	String sTargetID = "";
			    	String sSourceDate = "";
			    	String sTargetDate = "";
			    	String sfamilyId = "";
			    	sfamilyId = (String) rs.getString("family_id"); 
			    	sSourceID = (String) rs.getString("doc_id"); 
			    	sTargetID = (String) rs.getString("tg_doc_id");
			    	sSourceDate = (String) rs.getString("dt_publ");
			    	sTargetDate = (String) rs.getString("tg_dt_publ");
			    	hSourceTargetIDMap.put(String.valueOf(rowNum), String.format("%s\t%s\t%s\t%s\t%s", sSourceID, (null == sSourceDate?"":sSourceDate), sTargetID, (null == sTargetDate?"":sTargetDate), sfamilyId));
			    	rowNum++;
//			    	printLog.writeDebugLog("Row number : " + rowNum++ + ":" + sSourceID + ":" + sTargetID + ":" + dtPubl.toString());
			    }
//			    printLog.writeDebugLog(hTargetIDsMap.get("2019").size() + "");
//				printLog.writeDebugLog("End Exprot");
	
			} catch (Exception e) {
				printLog.writeError("ExportPairIDS", e);
			} finally {
				connector.close(pstmt);
				connector.close(con);
			}
			
			FileOutputStream outputStream = null;
			try {
				printLog.writeDebugLog("Start Write Output file");
				printLog.writeDebugLog("Row Number: " + rowNum);
		    	File file = new File(sOutputPath.concat("/").concat(sFileName));
			    if (!file.exists()) {
			    	file.getParentFile().mkdirs();
			    } 
			    printLog.writeDebugLog("Output File Path: " + file.getPath());
			    outputStream = new FileOutputStream(file, false);
			    for (int i = 1; i < rowNum; i++) {
			    	
			    	String sText = hSourceTargetIDMap.get(String.valueOf(i));
		    		String stempLine = String.format("%s\n",sText);
		    		outputStream.write(stempLine.getBytes(Charsets.toCharset("utf-8")));
		    		outputStream.flush();
			    }
			}catch(Exception e) {
				printLog.writeError("ExportPairIDS",e);
			}finally {
				try {
					outputStream.close();
				}catch (Exception e) {
					
				}
			}
			
			
		}else {
			printLog.writeDebugLog("Please check input parameters, some value is empty or null");
		}
		
		printLog.writeDebugLog("Done");
	}
	
	public void ExportPairOnlyIDSByDate(String sSourceLang, String sTargetLang, String sDateStart, String sOutputPath, String sDateEnd) throws Exception {
		printLog.writeDebugLog("Start : ExportPairOnlyIDSByDate...");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
	    HashMap<String, String> hSourceTargetIDMap = new HashMap<String, String>();
	    
		if (!StringUtils.isEmpty(sSourceLang) && !StringUtils.isEmpty(sTargetLang) && !StringUtils.isEmpty(sDateStart) && !StringUtils.isEmpty(sOutputPath) 
				&& !StringUtils.isEmpty(sDateEnd)) {
			printLog.writeDebugLog("sSourceLang: " + sSourceLang + "\t, sTargetLang: " + sTargetLang + 
								"\t, sDateStart: " + sDateStart + "\t, sDateStart: " + sDateStart);
			
			String sYear = sDateStart.substring(0, 4);
			String sFileName  = GetOutputFileName(sSourceLang, sTargetLang, sYear, "");
			StringBuilder sbQuery = new StringBuilder();
			
			sbQuery.append("select temp.family_id, temp.doc_id, temp.dt_publ, \n");
			sbQuery.append("case when pd.doc_id  is null or pd.doc_id = '' THEN \n");
			sbQuery.append("       concat(pd.country_code, '-', pd.doc_no, '-', pd.kind_code) \n");
			sbQuery.append("    else \n");
			sbQuery.append("      pd.doc_id \n");
			sbQuery.append("   end tg_doc_id \n");
			sbQuery.append(", pd.dt_publ tg_dt_publ \n");
			sbQuery.append("from\n");
			sbQuery.append("	patent_docno pd\n");
			sbQuery.append("inner join (\n");
			sbQuery.append("		select * \n");
			sbQuery.append("		from	patent_docno pd2\n");
			sbQuery.append("		where\n");
			sbQuery.append("		pd2.dt_publ between STR_TO_DATE(concat('").append(sDateStart).append("'), '%Y-%m-%d') and STR_TO_DATE( concat('").append(sDateEnd).append("'), '%Y-%m-%d')\n");
			sbQuery.append("		and pd2.country_code in (?, ?)\n");
			sbQuery.append("		\n");
			sbQuery.append("	) temp on 	temp.family_id = pd.family_id\n");
			sbQuery.append("where pd.country_code = ? \n");
			
			printLog.writeDebugLog(sbQuery.toString());
			int rowNum = 1;
			try {
				con = connector.get(con);
				pstmt = con.prepareStatement(sbQuery.toString());
				if ("EN".equalsIgnoreCase(sSourceLang)){
					pstmt.setString(1, "EP");
				}else {
					pstmt.setString(1, sSourceLang.toUpperCase());
				}
				if ("EN".equalsIgnoreCase(sSourceLang)){
					pstmt.setString(2, "US");
				}else {
					pstmt.setString(2, "UNKNOWN");
				}
				
				pstmt.setString(3, sTargetLang.toUpperCase());
				
				rs = pstmt.executeQuery();
			    
			    while(rs.next()) {
			    	String sSourceID = "";
			    	String sTargetID = "";
			    	String sSourceDate = "";
			    	String sTargetDate = "";
			    	String sfamilyId = "";
			    	sfamilyId = (String) rs.getString("family_id"); 
			    	sSourceID = (String) rs.getString("doc_id"); 
			    	sTargetID = (String) rs.getString("tg_doc_id");
			    	sSourceDate = (String) rs.getString("dt_publ");
			    	sTargetDate = (String) rs.getString("tg_dt_publ");
			    	hSourceTargetIDMap.put(String.valueOf(rowNum), String.format("%s\t%s\t%s\t%s\t%s", sSourceID, (null == sSourceDate?"":sSourceDate), sTargetID, (null == sTargetDate?"":sTargetDate), sfamilyId));
			    	rowNum++;
			    }
	
			} catch (Exception e) {
				printLog.writeError("ExportPairIDS", e);
			} finally {
				connector.close(pstmt);
				connector.close(con);
			}
			
			FileOutputStream outputStream = null;
			try {
				printLog.writeDebugLog("Start Write Output file");
				printLog.writeDebugLog("Row Number: " + rowNum);
		    	File file = new File(sOutputPath.concat("/").concat(sFileName));
			    if (!file.exists()) {
			    	file.getParentFile().mkdirs();
			    } 
			    printLog.writeDebugLog("Output File Path: " + file.getPath());
			    outputStream = new FileOutputStream(file, false);
			    for (int i = 1; i < rowNum; i++) {
			    	
			    	String sText = hSourceTargetIDMap.get(String.valueOf(i));
		    		String stempLine = String.format("%s\n",sText);
		    		outputStream.write(stempLine.getBytes(Charsets.toCharset("utf-8")));
		    		outputStream.flush();
			    }
			}catch(Exception e) {
				printLog.writeError("ExportPairIDS",e);
			}finally {
				try {
					outputStream.close();
				}catch (Exception e) {
					
				}
			}
			
			
		}else {
			printLog.writeDebugLog("Please check input parameters, some value is empty or null");
		}
		
		printLog.writeDebugLog("Done ExportPairOnlyIDSByDate");
	}
	
	private HashMap<String, String> getContentsFromFileForTarget(HashMap<String, List<String>> hTargetIDsMap, String sSourceDRPath, String sLang, String sCategory, String sYear, String sSource){
		HashMap<String, String> hSourceContent = new HashMap<String, String>();
		try {

			printLog.writeDebugLog("Start Get Target content");
			List<String> lPatentID = null;
			List<String> lPatentIDNoYear = hTargetIDsMap.get("0000");
			if (null == lPatentIDNoYear) {
				lPatentIDNoYear = new ArrayList<String>();
			}
			for (Map.Entry<String, List<String>> entry : hTargetIDsMap.entrySet()) {
				if (!entry.getKey().equals("0000")) {
					printLog.writeDebugLog(entry.getKey() + " = " + entry.getValue().size());
			    	lPatentID = entry.getValue();
				    hSourceContent.putAll(getContentsFromFile(lPatentID, lPatentIDNoYear, sSourceDRPath, sLang, sCategory, entry.getKey(), sSource));
				}
			}
			printLog.writeDebugLog("End Get Target content");
		}catch (Exception e) {
			printLog.writeError("GetContentsFromFileForTarget", e);
		}
		return hSourceContent;
	}
	
	private HashMap<String, String> getContentsFromFile(List<String> lSourceID, String sSourceDRPath, String sLang, String sCategory, String sYear, String sSource){
		List<String> lSourceIDNoYear = new ArrayList<>();
		return getContentsFromFile(lSourceID, lSourceIDNoYear, sSourceDRPath, sLang, sCategory, sYear, sSource);
	}
	private HashMap<String, String> getContentsFromFile(List<String> lSourceID, List<String> lSourceIDNoYear, String sSourceDRPath, String sLang, String sCategory, String sYear, String sSource){
		HashMap<String, String> hSourceContent = new HashMap<String, String>();
		BufferedReader buffRead = null;
		try {
			String sFileName = GetPatentFileName(sLang, sSource, sYear, sCategory);
			String sSourceFilePath = common.combine(sSourceDRPath, GetFolderName(sCategory));
			sSourceFilePath = common.combine(sSourceFilePath, sFileName);
			printLog.writeDebugLog("Processing File : " + sSourceFilePath);
			buffRead = new BufferedReader(new InputStreamReader(new FileInputStream(sSourceFilePath), "UTF-8" ));
			String sLine;
			
			Pattern pMetadataClean = null;
			Pattern pMetadataClean2 = null;
			Pattern pCleanSpace = null;
			if (ConstantsVal.METADATA.equalsIgnoreCase(sCategory)) {
				pMetadataClean = Pattern.compile("(\\/[0-9]*)([ ]+.+?[ ]*)(,|$)");
				pMetadataClean2 = Pattern.compile("(^|,)([ ]*[0-9]{1})([A-Z]{1}[ ])");
				pCleanSpace = Pattern.compile("[ ]+");
			}
			while ((sLine = buffRead.readLine()) != null) {
				String[] cols = sLine.split("\t");
				if (cols.length < 3) continue;
				
//				System.out.println(sLine);
				String sID 		= cols[0];
				String sContent = "";
				
				if (ConstantsVal.METADATA.equalsIgnoreCase(sCategory)) {
					if (cols.length >= 6) {
						String sIPC = cols[5];
						sIPC = cols[5];
						sIPC = pMetadataClean2.matcher(sIPC).replaceAll("$1"+"$3");
						sIPC = pMetadataClean.matcher(sIPC).replaceAll("$1"+"$3");
						sIPC = pCleanSpace.matcher(sIPC).replaceAll("");
						sContent = cols[1] + "\t" + cols[2] + "\t" + cols[3] + "\t" + cols[4].toUpperCase() + "\t" + sIPC;

					}else if (cols.length >= 5)
						sContent = cols[1] + "\t" + cols[2] + "\t" + cols[3] + "\t" + cols[4].toUpperCase() + "\t";
					else if (cols.length >= 4)
						sContent = cols[1] + "\t" + cols[2] + "\t" + cols[3] + "\t" + "\t";
					else
						sContent = cols[1] + "\t" + cols[2] + "\t" + "\t" + "\t";
					
				}else {
					sContent = cols[2];
				}
				
				
				if (!lSourceID.isEmpty() && (lSourceID.contains(sID) || lSourceIDNoYear.contains(sID))) {
					if(!StringUtils.isEmpty(sContent)) {
						hSourceContent.put(sID, String.format("%s", sContent));
						lSourceID.remove(sID);
						lSourceIDNoYear.remove(sID);
					}
				}else if (0 == lSourceID.size() && 0 == lSourceIDNoYear.size()) {
					break;
				}
			}
		}catch (Exception e) {
			printLog.writeError("GetContentsFromFile", e);
		}finally {
			try {
				buffRead.close();
			} catch (Exception e) {
				
			}
		}
		
		
		return hSourceContent;
	}
	
	private String GetOutputFileName(String sSourceLang, String sTargetLang, String sYear, String sCategory) {
		String sFileName = "";
		String sTemp = "";
		
		if (null == sCategory)
			sCategory = "";
		sCategory = sCategory.toLowerCase();
		if (StringUtils.isEmpty(sCategory)) {
			sTemp = "FamilyID";
		}else if (ConstantsVal.ABSTRACT.equals(sCategory)) {
			sTemp = "Abstract";
		}else if (ConstantsVal.CLAIM.equals(sCategory)) {
			sTemp = "Claim";
		}else if (ConstantsVal.DSCP.equals(sCategory)) {
			sTemp = "Description";
		}else if (ConstantsVal.METADATA.equals(sCategory)) {
			sTemp = "Metadata";
		}else if (ConstantsVal.TITLE.equals(sCategory)) {
			sTemp = "Title";
		} 
		
		sFileName = sSourceLang.toUpperCase() + "-" + sTargetLang.toUpperCase() + "-" + sYear + "-" + sTemp + ".tab";
		return sFileName;
	}
	
	private HashMap<String, String> getLanguageInfo(String sFamilyfilePath) {
		String sFileName = FilenameUtils.getName(sFamilyfilePath);
		sFileName = FilenameUtils.removeExtension(sFileName);
		printLog.writeDebugLog("sFileName: " + sFileName);
		HashMap<String, String> resultMap = new HashMap<String, String>();
		if (!StringUtils.isEmpty(sFileName)) {
			String[] sTemps = sFileName.split("-");
			if (sTemps.length >= 2) {
				resultMap.put("sourcelang", sTemps[0]);
				resultMap.put("targetlang", sTemps[1]);
			}
			
		}
		
		return resultMap;
	}
	
	private String GetPatentFileName(String sLang, String sSource, String sYear, String sCategory) {
		String sFileName = "";
		String sTemp = "";
		
		sCategory = sCategory.toLowerCase();
		
		if (StringUtils.isEmpty(sCategory)) {
			sTemp = "FamilyID";
		}else if (ConstantsVal.ABSTRACT.equals(sCategory)) {
			sTemp = "Abstract";
		}else if (ConstantsVal.CLAIM.equals(sCategory)) {
			sTemp = "Claim";
		}else if (ConstantsVal.DSCP.equals(sCategory)) {
			sTemp = "Description";
		}else if (ConstantsVal.METADATA.equals(sCategory)) {
			sTemp = "Metadata";
		}else if (ConstantsVal.TITLE.equals(sCategory)) {
			sTemp = "Title";
		} 
		
		String sSourceTemp = "";
		if (ConstantsVal.SOURCE_USPTO.equals(sSource)) {
			sSourceTemp = "USPTO-";
		}else if (ConstantsVal.SOURCE_EUROPAT.equals(sSource)) {
			sSourceTemp = "";
		}else{
			sSourceTemp  = sLang + "-";
			sTemp = sTemp.toLowerCase();
		}
		
		sFileName = sSourceTemp + sLang.toUpperCase() + "-" + sYear + "-" + sTemp + ".tab";
		return sFileName;
	}
	
	private String GetPatentFileName(String sLang, String sYear, String sCategory) {
		String sFileName = "";
		String sTemp = "";
		String sSourceTemp = "";
		
		sCategory = sCategory.toLowerCase();
		
		if (StringUtils.isEmpty(sCategory)) {
			sTemp = "FamilyID";
		}else if (ConstantsVal.ABSTRACT.equals(sCategory)) {
			sTemp = "abstract";
		}else if (ConstantsVal.CLAIM.equals(sCategory)) {
			sTemp = "claim";
		}else if (ConstantsVal.DSCP.equals(sCategory)) {
			sTemp = "desc";
		}else if (ConstantsVal.METADATA.equals(sCategory)) {
			sTemp = "Metadata";
		}else if (ConstantsVal.TITLE.equals(sCategory)) {
			sTemp = "title";
		} 
		
		sFileName = sSourceTemp + sLang.toUpperCase() + "-" + sLang.toUpperCase() + "-" + sYear + "-" + sTemp + ".tab";
		return sFileName;
	}
	
	private String GetFolderName(String sCategory) {
		String sTemp = "";
		
		sCategory = sCategory.toLowerCase();
		if (StringUtils.isEmpty(sCategory)) {
			sTemp = "FamilyID";
		}else if (ConstantsVal.ABSTRACT.equals(sCategory)) {
			sTemp = "abstract";
		}else if (ConstantsVal.CLAIM.equals(sCategory)) {
			sTemp = "claim";
		}else if (ConstantsVal.DSCP.equals(sCategory)) {
			sTemp = "description";
		}else if (ConstantsVal.METADATA.equals(sCategory)) {
			sTemp = "metadata";
		}else if (ConstantsVal.TITLE.equals(sCategory)) {
			sTemp = "title";
		} 
		
		return sTemp;
	}
	
	public void exportPairsByFamilyFile(String sFamilyFilePath, String sYear, String sCategory, String sOutputPath, String sEndYear, String targetPairFilePath) throws Exception {
		printLog.writeDebugLog("Start : exportPairsByFamilyFile...");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		String sEmptyYear = "0000";
		BufferedReader buffRead = null;
	    List<String> lSourceIDEP	= new ArrayList<String>();
	    List<String> lSourceIDUS	= new ArrayList<String>();
	    HashMap<String, String> hSourceTargetIDMap = new HashMap<String, String>();
	    HashMap<String, List<String>> hTargetIDsMap = new HashMap<String, List<String>>();
		if (!StringUtils.isEmpty(sCategory)   && (new File(sFamilyFilePath)).exists()
				&& !StringUtils.isEmpty(sYear) && !StringUtils.isEmpty(sOutputPath)) {
			
			
			HashMap<String, String> langMap = getLanguageInfo(sFamilyFilePath);
			int rowNum = 1;
			try {
				printLog.writeDebugLog("Processing File : " + sFamilyFilePath);
				buffRead = new BufferedReader(new InputStreamReader(new FileInputStream(sFamilyFilePath), "UTF-8" ));
				String sLine;
				while ((sLine = buffRead.readLine()) != null) {
					String[] cols = sLine.split("\t");

					if (cols.length < 3) continue;
					String sSourceID = cols[0];
			    	String sTargetID = cols[2];
			    	String sDtPubl = cols[3];
			    	hSourceTargetIDMap.put(String.valueOf(rowNum++), sSourceID + "|" + sTargetID);
			    	
			    	if (sSourceID.startsWith("EP-")) {
			    		lSourceIDEP.add(sSourceID);
			    	}else if (sSourceID.startsWith("US-")) {
			    		lSourceIDUS.add(sSourceID);
			    	}
			    	if(!StringUtils.isEmpty(sDtPubl) &&  sDtPubl.length() >= 4) {
			    		
			    		String sYearKey = sDtPubl.substring(0, 4);
			    		if (null != hTargetIDsMap.get(sYearKey)){
				    		hTargetIDsMap.get(sYearKey).add(sTargetID);
				    	}else {
				    		List<String> tempList = new ArrayList<String>();
				    		tempList.add(sTargetID);
				    		hTargetIDsMap.put(sYearKey, tempList);
				    	}
			    		
			    	}else {
			    		if (null != hTargetIDsMap.get(sEmptyYear)){
				    		hTargetIDsMap.get(sEmptyYear).add(sTargetID);
				    	}else {
				    		List<String> tempList = new ArrayList<String>();
				    		tempList.add(sTargetID);
				    		hTargetIDsMap.put(sEmptyYear, tempList);
				    	}
			    	}

				}
			    
//			    printLog.writeDebugLog(hTargetIDsMap.get("2019").size() + "");
				printLog.writeDebugLog("End Exprot");
	
			} catch (Exception e) {
				printLog.writeError("ExportPairIDS", e);
			} finally {
				try {
					buffRead.close();
				} catch (Exception e) {
					
				}
			}
			
//			for (Map.Entry<String, String> entry: hSourceTargetIDMap.entrySet()) {
//				printLog.writeDebugLog(entry.getKey() + ": value: " + entry.getValue());
//			}
//			
//			for (Map.Entry<String, List<String>> entry: hTargetIDsMap.entrySet()) {
//				printLog.writeDebugLog(entry.getKey() + ": value: " + entry.getValue().size());
//			}
				printLog.writeDebugLog("Row number : " + rowNum);
				
//				// Get Contents of Patents Source
				HashMap<String, String> hSourceContent = new HashMap<String, String>();
				HashMap<String, String> hSourceContentEP = new HashMap<String, String>();
				HashMap<String, String> hSourceContentUS = new HashMap<String, String>();
				HashMap<String, String> hTargetContent = new HashMap<String, String>();

				printLog.writeDebugLog("Start Get Source Content:");
				hSourceContentEP = getContentsFromFile(lSourceIDEP, config._config.SourceFilePath, langMap.get("sourcelang"), sCategory, sYear, ConstantsVal.SOURCE_EUROPAT);
				hSourceContentUS = getContentsFromFile(lSourceIDUS, config._config.TargetFilePath, langMap.get("sourcelang"), sCategory, sYear, ConstantsVal.SOURCE_USPTO);
//				for(Map.Entry<String, String> m: hSourceContentEP.entrySet()) {
//					printLog.writeDebugLog(m.getKey() + ": value: " + m.getValue());
//				}
				for(Map.Entry<String, String> m: hSourceContentUS.entrySet()) {
					printLog.writeDebugLog(m.getKey() + ": value: " + m.getValue());
				}
				hSourceContent.putAll(hSourceContentEP);
				hSourceContent.putAll(hSourceContentUS);
//				for(Map.Entry<String, String> m: hSourceContent.entrySet()) {
//					printLog.writeDebugLog(m.getKey() + ": value: " + m.getValue());
//				}
				printLog.writeDebugLog("End Get Source Content:");
//				// Get Contents of Patent Target
				hTargetContent = getContentsFromFileForTarget(hTargetIDsMap, targetPairFilePath, langMap.get("targetlang"), sCategory, sYear, ConstantsVal.SOURCE_OTHERS);
//		
		//			for(Map.Entry<String, String> m: hTargetContent.entrySet()) {
		//				log.print(m.getKey() + ": value: " + m.getValue());
		//			}
				FileOutputStream outputStream = null;
				try {
					printLog.writeDebugLog("Start Write Output file");
					printLog.writeDebugLog("Row Number: " + rowNum);
					String sFileName  = GetOutputFileName(langMap.get("sourcelang"), langMap.get("targetlang"), sYear, sCategory);
			    	File file = new File(sOutputPath.concat("/").concat(sFileName));
				    if (!file.exists()) {
				    	file.getParentFile().mkdirs();
				    } 
				    printLog.writeDebugLog("Output File Path: " + file.getPath());
				    outputStream = new FileOutputStream(file, false);
				    for (int i = 1; i < rowNum; i++) {
				    	
				    	String[] temp = hSourceTargetIDMap.get(String.valueOf(i)).split("\\|");
				    	String sTempSource = temp[0];
				    	String StempTarget = temp[1];
				    	String sSourceContent = hSourceContent.get(sTempSource);
				    	String sTargetContent = hTargetContent.get(StempTarget);
		//			    	String stempLine2 = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
		//			    	log.print(stempLine2);
				    	if (!StringUtils.isEmpty(sSourceContent) && !StringUtils.isEmpty(sTargetContent)) {
				    		String stempLine = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
				    		outputStream.write(stempLine.getBytes(Charsets.toCharset("utf-8")));
				    		outputStream.flush();
				    	}
				    }
				}catch(Exception e) {
					printLog.writeError("ExportPairIDS",e);
				}finally {
					try {
						outputStream.close();
					}catch (Exception e) {
						
					}
				}
			
			
//			
		}else {
			printLog.writeDebugLog("Please check input parameters, some value is empty or null");
		}
//		
		printLog.writeDebugLog("Done exportPairsByFamilyFile.");
	}
	
	public void getPatentContents(String sFamilyFilePath, String sOutputPath) throws Exception {
		getPatentContents(sFamilyFilePath, sOutputPath, null);
	}
	
	public void getPatentContents(String sFamilyFilePath, String sOutputPath, String contentType) throws Exception {
		printLog.writeDebugLog("Start : getPatentContents... contentType :" + contentType);
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		BufferedReader buffRead = null;
	    List<DocNoYear> lSourceTarget	= new ArrayList<DocNoYear>();
		if ((new File(sFamilyFilePath)).exists() && !StringUtils.isEmpty(sOutputPath)) {
			
			
			HashMap<String, String> langMap = getLanguageInfo(sFamilyFilePath);
			int rowNum = 1;
			try {
				printLog.writeDebugLog("Processing File : " + sFamilyFilePath);
				buffRead = new BufferedReader(new InputStreamReader(new FileInputStream(sFamilyFilePath), "UTF-8" ));
				String sLine;
				while ((sLine = buffRead.readLine()) != null) {
					DocNoYear docNoYear = new DocNoYear();
					String[] cols = sLine.split("\t");

					if (cols.length < 3) continue;
			    	String sTargetID = cols[2];
			    	String sDtPubl = cols[3];
			    	docNoYear.setDatePublication(sDtPubl);
			    	if (!StringUtils.isEmpty(sTargetID))
			    		docNoYear.setDocno(sTargetID.replaceAll("-", "."));
			    	
			    	if(!StringUtils.isEmpty(sDtPubl) &&  sDtPubl.length() >= 4) {
			    		
			    		String sYearKey = sDtPubl.substring(0, 4);
			    		docNoYear.setYear(sYearKey);
			    		
			    	}else {
			    		docNoYear.setYear("0000");
			    	}
			    	
			    	if (!StringUtils.isEmpty(docNoYear.getDocno())) {
			    		lSourceTarget.add(docNoYear);
			    	}

				}
				printLog.writeDebugLog("End Exprot");
	
			} catch (Exception e) {
				printLog.writeError("ExportPairIDS", e);
			} finally {
				try {
					buffRead.close();
				} catch (Exception e) {
					
				}
			}
		}
		
		if (null != lSourceTarget && !lSourceTarget.isEmpty()) {
			List<String> contentTypeList = Arrays.asList("description", "claims", "abstract,biblio");
			File fileOuput = new File(sOutputPath);
			for (DocNoYear docNoYear: lSourceTarget) {
				if (null != contentType && contentType.trim().length() > 0) {
					getContents(docNoYear, fileOuput, contentType);
				}else {
					for (String sContentType: contentTypeList) {
//						if (("description".equals(sContentType) ||  "claims".equals(sContentType)) 
//								&& (null != docNoYear.getDocno() && !docNoYear.getDocno().startsWith("US"))
//							) {
//							getContents(docNoYear, fileOuput, sContentType);
//						}else if ("abstract,biblio".equals(sContentType)){
							getContents(docNoYear, fileOuput, sContentType);
//						}
					}
				}
			}
		}
		
		
		
		printLog.writeDebugLog("End : getPatentContents...");
	}
	
	public void getPatentContentsByDotFile(String sFamilyFilePath, String sOutputPath) throws Exception {
		printLog.writeDebugLog("Start : getPatentContentsByDotFile...");
		BufferedReader buffRead = null;
	    List<DocNoYear> lSourceTarget	= new ArrayList<DocNoYear>();
		if ((new File(sFamilyFilePath)).exists() && !StringUtils.isEmpty(sOutputPath)) {
			
			
			try {
				printLog.writeDebugLog("Processing File : " + sFamilyFilePath);
				buffRead = new BufferedReader(new InputStreamReader(new FileInputStream(sFamilyFilePath), "UTF-8" ));
				String sLine;
				while ((sLine = buffRead.readLine()) != null) {
					DocNoYear docNoYear = new DocNoYear();
					String[] cols = sLine.split("\\.");
					if (cols.length < 3) continue;
//			    	String sTargetID = cols[2];
//			    	String sDtPubl = cols[3];
			    	docNoYear.setDatePublication("");
//			    	if (!StringUtils.isEmpty(sTargetID))
			    	if (cols.length > 1 && "EP".equalsIgnoreCase(cols[0]) && cols[1].length() > 7)
			    		docNoYear.setDocno(cols[0] + "." + cols[1].substring(cols[1].length() - 7) + "." + cols[2]);
			    	else
			    		continue;
			    	docNoYear.setYear("2020");
			    		
			    	
			    	if (!StringUtils.isEmpty(docNoYear.getDocno())) {
			    		lSourceTarget.add(docNoYear);
			    	}

				}
				printLog.writeDebugLog("lSourceTarget Size:" + lSourceTarget.size());
				printLog.writeDebugLog("End Exprot");
	
			} catch (Exception e) {
				printLog.writeError("ExportPairIDS", e);
			} finally {
				try {
					buffRead.close();
				} catch (Exception e) {
					
				}
			}
		}
		
		if (null != lSourceTarget && !lSourceTarget.isEmpty()) {
			List<String> contentTypeList = Arrays.asList("description", "claims", "abstract,biblio");
			File fileOuput = new File(sOutputPath);
			for (DocNoYear docNoYear: lSourceTarget) {
				for (String sContentType: contentTypeList) {
//					if (("description".equals(sContentType) ||  "claims".equals(sContentType)) 
//							&& (null != docNoYear.getDocno() && !docNoYear.getDocno().startsWith("US"))
//						) {
//						getContents(docNoYear, fileOuput, sContentType);
//					}else if ("abstract,biblio".equals(sContentType)){
						getContents(docNoYear, fileOuput, sContentType);
//					}
				}
			}
		}
		
		
		
		printLog.writeDebugLog("End : getPatentContents...");
	}
	
	class DocNoYear{
		private String docno;
		private String year;
		private String datePublication;
		
		public String getDatePublication() {
			return datePublication;
		}
		public void setDatePublication(String datePublication) {
			this.datePublication = datePublication;
		}
		public String getDocno() {
			return docno;
		}
		public void setDocno(String docno) {
			this.docno = docno;
		}
		public String getYear() {
			return year;
		}
		public void setYear(String year) {
			this.year = year;
		}
		
		
		
	}
	
	public void ExportPairIDSRealeas2(String sSourceLang, String sTargetLang, String sYear, String sCategory, String sOutputPath, String sSourceDRPath) throws Exception {
		printLog.writeDebugLog("Start : ExportPairIDSRealeas2...");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
	    List<String> lSourceID	= new ArrayList<String>();
	    HashMap<String, String> hSourceTargetIDMap = new HashMap<String, String>();
	    HashMap<String, List<String>> hTargetIDsMap = new HashMap<String, List<String>>();
	    HashMap<String, List<String>> hTargetIDsMapUS = new HashMap<String, List<String>>();
	    HashMap<String, List<String>> hTargetIDsMapEP = new HashMap<String, List<String>>();
	    HashMap<String, String> hSourceContent = new HashMap<String, String>();
		if (!StringUtils.isEmpty(sCategory) && !StringUtils.isEmpty(sSourceLang) && !StringUtils.isEmpty(sTargetLang) 
				&& !StringUtils.isEmpty(sYear) && !StringUtils.isEmpty(sOutputPath)) {
			
			String sFileName  = GetOutputFileName(sSourceLang, sTargetLang, sYear, sCategory);
			printLog.writeDebugLog("Start Get Source Content:");
			hSourceContent = getContentsFromFile(lSourceID, sSourceDRPath, sSourceLang, sCategory, sYear);
			StringBuilder sbQuery = new StringBuilder();
			
			sbQuery.append("select pd1.doc_id sdoc_id, pd1.dt_publ sdt_publ, pd2.doc_id tdoc_id, pd2.dt_publ tdt_publ, pd2.family_id, pd2.country_code tcountry_code  \n");
			sbQuery.append("from patent_docno pd1 \n");
			sbQuery.append("inner join patent_docno pd2 on pd2.family_id  = pd1.family_id and pd2.country_code in ('EP', 'US') \n");
			sbQuery.append("left join t_ep_en_pt_title ep on pd2.country_code = ep.country_cd \n");
			sbQuery.append("	          and pd2.doc_no   = ep.doc_no  \n");
			sbQuery.append("	          and pd2.kind_code  = ep.kind_cd \n");
			sbQuery.append("	          and ep.kind_cd in ('A1', 'B1', 'B2') \n");
			sbQuery.append("left join t_us_en_pt_title us on pd2.country_code = us.country_cd \n");
			sbQuery.append("	          and lpad(pd2.doc_no, 8, 0)   = us.doc_no  \n");
			sbQuery.append("	          and pd2.kind_code  = us.kind_cd \n");
			sbQuery.append("where pd1.doc_id  in (@patentids@) \n");
			sbQuery.append("and (ep.id is not null or us.id is not null) \n");
			sbQuery.append("order by pd1.dt_publ, pd2.dt_publ asc; \n");
			
			printLog.writeDebugLog(sbQuery.toString());
			int rowNum = 1;
			try {
				con = connector.get(con);
				List<String> tmpPatentIds = new ArrayList<>();
				String sPatentIds = "";
				for (int i =0; i < lSourceID.size(); i++) {
					
//						tmpPatentIds.add(lSourceID.get(i));
						
						if ((i % 150 == 0 || i == lSourceID.size() -1)) {
//							String[] tmp = new String[tmpPatentIds.size()]; 
//							tmpPatentIds.toArray(tmp);
//							tmpPatentIds = new ArrayList<>();
							sPatentIds= sPatentIds + "'" +lSourceID.get(i) + "'";
							String tmpScript = sbQuery.toString();
							tmpScript = tmpScript.replaceAll("@patentids@", sPatentIds);
							pstmt = con.prepareStatement(tmpScript);
//							pstmt.setString(1, sPatentIds);
							sPatentIds = "";
							rs = pstmt.executeQuery();
						    
						    while(rs.next()) {
						    	String sSourceID = "";
						    	String sTargetID = "";
						    	String sTCountryCode = "";
					    		java.sql.Date dtPubl = rs.getDate("tdt_publ");	
						    	sSourceID = (String) rs.getString("sdoc_id"); 
						    	sTargetID = (String) rs.getString("tdoc_id");
						    	sTCountryCode = (String) rs.getString("tcountry_code");
						    	hSourceTargetIDMap.put(String.valueOf(rowNum), sSourceID + "|" + sTargetID);
						    	if (null != dtPubl) {
							    	Calendar cal = Calendar.getInstance();
							    	cal.setTime(dtPubl);
							    	String	 year = String.valueOf(cal.get(Calendar.YEAR));
							    	
							    	if ("EP".equals(sTCountryCode.toUpperCase())) {
							    		if (null != hTargetIDsMapEP.get(year)){
								    		hTargetIDsMapEP.get(year).add(sTargetID);
								    	}else {
								    		List<String> tempList = new ArrayList<String>();
								    		tempList.add(sTargetID);
								    		hTargetIDsMapEP.put(year, tempList);
								    	}
							    	}else if ("US".equals(sTCountryCode.toUpperCase())) {
							    		if (null != hTargetIDsMapUS.get(year)){
							    			hTargetIDsMapUS.get(year).add(sTargetID);
								    	}else {
								    		List<String> tempList = new ArrayList<String>();
								    		tempList.add(sTargetID);
								    		hTargetIDsMapUS.put(year, tempList);
								    	}
							    	}
							    	
						    	}
						    	rowNum++;
		//				    	printLog.writeDebugLog("Row number : " + rowNum++ + ":" + sSourceID + ":" + sTargetID + ":" + dtPubl.toString());
						    }
						    pstmt = null;
						}else {
					    	sPatentIds= sPatentIds + "'" +lSourceID.get(i) + "', ";
					    }
	//				    printLog.writeDebugLog(hTargetIDsMap.get("2019").size() + "");
	//					printLog.writeDebugLog("End Exprot");
				}
			} catch (Exception e) {
				printLog.writeError("ExportPairIDSRealeas2", e);
			} finally {
				connector.close(pstmt);
				connector.close(con);
			}
			
				printLog.writeDebugLog("Row number : " + rowNum);
				// Get Contents of Patents Source
		//			for(Map.Entry<String, String> m: hSourceContent.entrySet()) {
		//				log.print(m.getKey() + ": value: " + m.getValue());
		//			}
				printLog.writeDebugLog("End Get Source Content:");
				HashMap<String, String> hTargetContent = new HashMap<String, String>();
				HashMap<String, String> hTargetContentEP = new HashMap<String, String>();
				HashMap<String, String> hTargetContentUS = new HashMap<String, String>();
				hTargetContentEP = getContentsFromFileForTarget(hTargetIDsMapEP, config._config.SourceFilePath, sTargetLang, sCategory, sYear, ConstantsVal.SOURCE_EUROPAT);
				hTargetContentUS = getContentsFromFileForTarget(hTargetIDsMapUS, config._config.TargetFilePath, sTargetLang, sCategory, sYear, ConstantsVal.SOURCE_USPTO);
				
				hTargetContent.putAll(hTargetContentEP);
				hTargetContent.putAll(hTargetContentUS);
		//			for(Map.Entry<String, String> m: hTargetContent.entrySet()) {
		//				log.print(m.getKey() + ": value: " + m.getValue());
		//			}
				FileOutputStream outputStream = null;
				try {
					printLog.writeDebugLog("Start Write Output file");
					printLog.writeDebugLog("Row Number: " + rowNum);
			    	File file = new File(sOutputPath.concat("/").concat(sFileName));
				    if (!file.exists()) {
				    	file.getParentFile().mkdirs();
				    } 
				    printLog.writeDebugLog("Output File Path: " + file.getPath());
				    outputStream = new FileOutputStream(file, false);
				    for (int i = 1; i < rowNum; i++) {
				    	
				    	String[] temp = hSourceTargetIDMap.get(String.valueOf(i)).split("\\|");
				    	String sTempSource = temp[0];
				    	String StempTarget = temp[1];
				    	String sSourceContent = hSourceContent.get(sTempSource);
				    	String sTargetContent = hTargetContent.get(StempTarget);
		//			    	String stempLine2 = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
		//			    	log.print(stempLine2);
				    	if (!StringUtils.isEmpty(sSourceContent) && !StringUtils.isEmpty(sTargetContent)) {
				    		String stempLine = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
				    		outputStream.write(stempLine.getBytes(Charsets.toCharset("utf-8")));
				    		outputStream.flush();
				    	}
				    }
				}catch(Exception e) {
					printLog.writeError("ExportPairIDSRealeas2",e);
				}finally {
					try {
						outputStream.close();
					}catch (Exception e) {
						
					}
				}
//		}else if(StringUtils.isEmpty(sCategory)) {
//			ExportPairOnlyIDS(sSourceLang, sTargetLang, sYear, sCategory, sOutputPath, sEndYear);
		}else {
			printLog.writeDebugLog("Please check input parameters, some value is empty or null");
		}
		
		printLog.writeDebugLog("Done");
	}
	
	private HashMap<String, String> getContentsFromFile(List<String> lSourceID, String sSourceDRPath, String sLang, String sCategory, String sYear){
		HashMap<String, String> hSourceContent = new HashMap<String, String>();
		BufferedReader buffRead = null;
//		sSourceDRPath = "/data/patents/pdfpatents";
		try {
			String sFileName = GetPatentFileName(sLang, sYear, sCategory);
			String sSourceFilePath = common.combine(sSourceDRPath, sLang.toUpperCase() + "-" + sYear);
//			String sSourceFilePath = sSourceDRPath;
			sSourceFilePath = common.combine(sSourceFilePath, sFileName);
			printLog.writeDebugLog("Processing File : " + sSourceFilePath);
			buffRead = new BufferedReader(new InputStreamReader(new FileInputStream(sSourceFilePath), "UTF-8" ));
			String sLine;
			
			Pattern pMetadataClean = null;
			Pattern pMetadataClean2 = null;
			Pattern pCleanSpace = null;
			if (ConstantsVal.METADATA.equalsIgnoreCase(sCategory)) {
				pMetadataClean = Pattern.compile("(\\/[0-9]*)([ ]+.+?[ ]*)(,|$)");
				pMetadataClean2 = Pattern.compile("(^|,)([ ]*[0-9]{1})([A-Z]{1}[ ])");
				pCleanSpace = Pattern.compile("[ ]+");
			}
			while ((sLine = buffRead.readLine()) != null) {
				String[] cols = sLine.split("\t");
				if (cols.length < 2) continue;
				
//				System.out.println(sLine);
				String sID 		= cols[0];
				String sContent = "";
				
				if (ConstantsVal.METADATA.equalsIgnoreCase(sCategory)) {
					if (cols.length >= 6) {
						String sIPC = cols[5];
						sIPC = cols[5];
						sIPC = pMetadataClean2.matcher(sIPC).replaceAll("$1"+"$3");
						sIPC = pMetadataClean.matcher(sIPC).replaceAll("$1"+"$3");
						sIPC = pCleanSpace.matcher(sIPC).replaceAll("");
						sContent = cols[1] + "\t" + cols[2] + "\t" + cols[3] + "\t" + cols[4].toUpperCase() + "\t" + sIPC;

					}else if (cols.length >= 5)
						sContent = cols[1] + "\t" + cols[2] + "\t" + cols[3] + "\t" + cols[4].toUpperCase() + "\t";
					else if (cols.length >= 4)
						sContent = cols[1] + "\t" + cols[2] + "\t" + cols[3] + "\t" + "\t";
					else if (cols.length == 2)
						sContent = cols[1] + "\t" + "\t" + "\t" + sLang.toUpperCase() +  "\t";
					else 
						sContent = cols[1] + "\t" + cols[2] + "\t" + "\t" + "\t";
					
				}else {
					sContent = cols[2];
				}
				
				if(!StringUtils.isEmpty(sContent)) {
						hSourceContent.put(sID, String.format("%s", sContent));
						lSourceID.add(sID);
				}
			}
		}catch (Exception e) {
			printLog.writeError("GetContentsFromFile", e);
		}finally {
			try {
				buffRead.close();
			} catch (Exception e) {
				
			}
		}
		
		return hSourceContent;
	} 
	
	
	public void ExportPairIDSRealeas3(String sSourceLang, String sTargetLang, String sYear, String sCategory, String sOutputPath, String sSourceDRPath) throws Exception {
		printLog.writeDebugLog("Start : ExportPairIDSRealeas3...");
		PreparedStatement pstmt = null;
		ResultSet rs = null;
	    List<String> lSourceID	= new ArrayList<String>();
	    HashMap<String, String> hSourceTargetIDMap = new HashMap<String, String>();
	    HashMap<String, List<String>> hTargetIDsMapUS = new HashMap<String, List<String>>();
	    HashMap<String, List<String>> hTargetIDsMapEP = new HashMap<String, List<String>>();
	    HashMap<String, String> hSourceContent = new HashMap<String, String>();
		if (!StringUtils.isEmpty(sCategory) && !StringUtils.isEmpty(sSourceLang) && !StringUtils.isEmpty(sTargetLang) 
				&& !StringUtils.isEmpty(sYear) && !StringUtils.isEmpty(sOutputPath)) {
			
			String sFileName  = GetOutputFileName(sSourceLang, sTargetLang, sYear, sCategory);
			printLog.writeDebugLog("Start Get Source Content:");
			hSourceContent = getContentsFromFile(lSourceID, sSourceDRPath, sSourceLang, sCategory, sYear);
			StringBuilder sbQuery = new StringBuilder();
			
			sbQuery.append("select pd1.doc_id sdoc_id, pd1.dt_publ sdt_publ, pd2.doc_id tdoc_id, pd2.dt_publ tdt_publ, pd2.family_id, pd2.country_code tcountry_code  \n");
			sbQuery.append("from patent_docno pd1 \n");
			sbQuery.append("inner join patent_docno pd2 on pd2.family_id  = pd1.family_id and pd2.country_code in ('EP', 'FR') \n");
			sbQuery.append("inner join t_ep_").append(sTargetLang.toLowerCase()).append("_pt_title ep on pd2.country_code = ep.country_cd \n");
			sbQuery.append("	          and pd2.doc_no   = ep.doc_no  \n");
			sbQuery.append("	          and pd2.kind_code  = ep.kind_cd \n");
			sbQuery.append("	          and ep.kind_cd in ('A1', 'B1', 'B2') \n");
			sbQuery.append("where pd1.doc_id  in (@patentids@) \n");
//			sbQuery.append("and ep.id is not null \n");
			sbQuery.append("order by pd1.dt_publ, pd2.dt_publ asc; \n");
			
			printLog.writeDebugLog(sbQuery.toString());
			int rowNum = 1;
			try {
				con = connector.get(con);
				String sPatentIds = "";
				for (int i =0; i < lSourceID.size(); i++) {
					
//						tmpPatentIds.add(lSourceID.get(i));
						
						if ((i % 150 == 0 || i == lSourceID.size() -1)) {
//							String[] tmp = new String[tmpPatentIds.size()]; 
//							tmpPatentIds.toArray(tmp);
//							tmpPatentIds = new ArrayList<>();
							sPatentIds= sPatentIds + "'" +lSourceID.get(i) + "'";
							String tmpScript = sbQuery.toString();
							tmpScript = tmpScript.replaceAll("@patentids@", sPatentIds);
							pstmt = con.prepareStatement(tmpScript);
//							pstmt.setString(1, sPatentIds);
							sPatentIds = "";
							rs = pstmt.executeQuery();
						    
						    while(rs.next()) {
						    	String sSourceID = "";
						    	String sTargetID = "";
						    	String sTCountryCode = "";
					    		java.sql.Date dtPubl = rs.getDate("tdt_publ");	
						    	sSourceID = (String) rs.getString("sdoc_id"); 
						    	sTargetID = (String) rs.getString("tdoc_id");
						    	sTCountryCode = (String) rs.getString("tcountry_code");
						    	hSourceTargetIDMap.put(String.valueOf(rowNum), sSourceID + "|" + sTargetID);
						    	if (null != dtPubl) {
							    	Calendar cal = Calendar.getInstance();
							    	cal.setTime(dtPubl);
							    	String	 year = String.valueOf(cal.get(Calendar.YEAR));
							    	
							    	if ("EP".equals(sTCountryCode.toUpperCase())) {
							    		if (null != hTargetIDsMapEP.get(year)){
								    		hTargetIDsMapEP.get(year).add(sTargetID);
								    	}else {
								    		List<String> tempList = new ArrayList<String>();
								    		tempList.add(sTargetID);
								    		hTargetIDsMapEP.put(year, tempList);
								    	}
							    	}else if ("US".equals(sTCountryCode.toUpperCase())) {
							    		if (null != hTargetIDsMapUS.get(year)){
							    			hTargetIDsMapUS.get(year).add(sTargetID);
								    	}else {
								    		List<String> tempList = new ArrayList<String>();
								    		tempList.add(sTargetID);
								    		hTargetIDsMapUS.put(year, tempList);
								    	}
							    	}
							    	
						    	}
						    	rowNum++;
		//				    	printLog.writeDebugLog("Row number : " + rowNum++ + ":" + sSourceID + ":" + sTargetID + ":" + dtPubl.toString());
						    }
						    pstmt = null;
						}else {
					    	sPatentIds= sPatentIds + "'" +lSourceID.get(i) + "', ";
					    }
	//				    printLog.writeDebugLog(hTargetIDsMap.get("2019").size() + "");
	//					printLog.writeDebugLog("End Exprot");
				}
			} catch (Exception e) {
				printLog.writeError("ExportPairIDSRealeas2", e);
			} finally {
				connector.close(pstmt);
				connector.close(con);
			}
			
				printLog.writeDebugLog("Row number : " + rowNum);
				// Get Contents of Patents Source
		//			for(Map.Entry<String, String> m: hSourceContent.entrySet()) {
		//				log.print(m.getKey() + ": value: " + m.getValue());
		//			}
				printLog.writeDebugLog("End Get Source Content:");
				HashMap<String, String> hTargetContent = new HashMap<String, String>();
				HashMap<String, String> hTargetContentEP = new HashMap<String, String>();
//				HashMap<String, String> hTargetContentUS = new HashMap<String, String>();
				hTargetContentEP = getContentsFromFileForTarget(hTargetIDsMapEP, config._config.SourceFilePath, sTargetLang, sCategory, sYear, ConstantsVal.SOURCE_EUROPAT);
//				hTargetContentUS = getContentsFromFileForTarget(hTargetIDsMapUS, config._config.TargetFilePath, sTargetLang, sCategory, sYear, ConstantsVal.SOURCE_USPTO);
				
				hTargetContent.putAll(hTargetContentEP);
//				hTargetContent.putAll(hTargetContentUS);
		//			for(Map.Entry<String, String> m: hTargetContent.entrySet()) {
		//				log.print(m.getKey() + ": value: " + m.getValue());
		//			}
				FileOutputStream outputStream = null;
				try {
					printLog.writeDebugLog("Start Write Output file");
					printLog.writeDebugLog("Row Number: " + rowNum);
			    	File file = new File(sOutputPath.concat("/").concat(sFileName));
				    if (!file.exists()) {
				    	file.getParentFile().mkdirs();
				    } 
				    printLog.writeDebugLog("Output File Path: " + file.getPath());
				    outputStream = new FileOutputStream(file, false);
				    for (int i = 1; i < rowNum; i++) {
				    	
				    	String[] temp = hSourceTargetIDMap.get(String.valueOf(i)).split("\\|");
				    	String sTempSource = temp[0];
				    	String StempTarget = temp[1];
				    	String sSourceContent = hSourceContent.get(sTempSource);
				    	String sTargetContent = hTargetContent.get(StempTarget);
		//			    	String stempLine2 = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
		//			    	log.print(stempLine2);
				    	if (!StringUtils.isEmpty(sSourceContent) && !StringUtils.isEmpty(sTargetContent)) {
//				    		String stempLine = String.format("%s\t%s\t%s\t%s\n",sTempSource,  sSourceContent,  StempTarget, sTargetContent);
				    		String stempLine = String.format("%s\t%s\t%s\t%s\n",  StempTarget, sTargetContent ,sTempSource,  sSourceContent);
				    		outputStream.write(stempLine.getBytes(Charsets.toCharset("utf-8")));
				    		outputStream.flush();
				    	}
				    }
				}catch(Exception e) {
					printLog.writeError("ExportPairIDSRealeas2",e);
				}finally {
					try {
						outputStream.close();
					}catch (Exception e) {
						
					}
				}
//		}else if(StringUtils.isEmpty(sCategory)) {
//			ExportPairOnlyIDS(sSourceLang, sTargetLang, sYear, sCategory, sOutputPath, sEndYear);
		}else {
			printLog.writeDebugLog("Please check input parameters, some value is empty or null");
		}
		
		printLog.writeDebugLog("Done");
	}
	
	
	
}
