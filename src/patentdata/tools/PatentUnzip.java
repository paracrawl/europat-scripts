package patentdata.tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import patentdata.utils.DocumentVO;
import patentdata.utils.PatentDocumentVO;

public class PatentUnzip extends Patent {


	public PatentUnzip() throws Exception {
	}

	public PatentUnzip(String path) throws Exception {
		super(path);
		// TODO Auto-generated constructor stub
	}

	static PatentUnzip oTool;
	PatentExtractEPO patentExtractEPO;
	
	

	public static void main(String[] args) throws Exception {
		String line = String.join(" ", args);
		String sConfigPath = new PatentUnzip().getOptionConfig(line);
		oTool = new PatentUnzip(sConfigPath);
		
		try {
			if (oTool.validateMandatory(args)) {

				String sInputPath = oTool.getOptionValue("I", line);
				if (sInputPath == null || sInputPath.trim().length() == 0)
					throw new Exception("Required: -I <InputPath>");

				File fileInput = new File(sInputPath);
				if (!fileInput.exists())
					throw new Exception("InputPath is not exist.");
				if (!fileInput.isDirectory()
						&& !"zip".equalsIgnoreCase(FilenameUtils.getExtension(fileInput.toString())))
					throw new Exception("InputPath must be a ZIP file.");

				String sOutputPath = oTool.getOptionValue("O", line);
				File fileOutput = new File(
						(sOutputPath == null || sOutputPath.length() == 0) ? oTool.configInfo.WorkingDir : sOutputPath);
				String sOutExt = FilenameUtils.getExtension(fileOutput.toString());
				if (!StringUtils.isEmpty(sOutExt) && !"txt".equalsIgnoreCase(sOutExt))
					throw new Exception("OutputPath must be a TXT file.");


				List<String> list = oTool.listStringPattern("(?i)-(datacoverage)", line);
				if (list.size() < 1) {
					throw new Exception("Invalid command (\"-datacoverage\" missing).");
				} else {
					String sYear = oTool.getOptionValue("Y", line);
					if (sYear == null || sYear.length() == 0)
						throw new Exception("Required: -Y <Year>");
					oTool.getDataCoverage(fileInput, fileOutput, Integer.parseInt(sYear));
				}
			}
			System.out.println("finished");
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}
	}

	private boolean validateMandatory(String[] args) {
		try {
			if (args == null || args.length == 0 || args[0].equals("--help")) {
				System.out.println("------------------------");
				System.out.println("Parameters");
				System.out.println("------------------------");
				System.out.println("1. -I <InputPath> (Required): Input path or folder (ZIP only)");
				System.out.println("2. -O <OutputPath> (Optional): Output path or folder (TXT only)");
				System.out.println("3. -C <ConfigPath> (Optional): Config path (JSON only)");
				System.out.println("4. -Y <Year> (Required): Year of PublicationDate");
				System.out.println("------------------------");
				System.out.println("Example");
				System.out.println("------------------------");
				System.out.println(
						"java -jar patent.jar -datacoverage -Y 2019 -I \"input/directory\" -O \"output/directory\" -C \"config/path/patent.json\"");
				System.exit(0);
				return false;
			} else {
				return true;
			}
		} catch (Exception e) {
			System.exit(0);
			return false;
		}
	}

	public void getDataCoverage(File folderInput, File folderOutput, int year) throws Exception {
//		int year = 2017;
//		for (int year=1979; year<2020; year++) {}
		LocalDate localDate = LocalDate.of(year, Month.JANUARY, 1);
		int weekNumber = 0;
		while (localDate.getYear()==year) {
			try {
				localDate = getNextWed(localDate);
				weekNumber++;
//				log.print(localDate.toString()); 
				if (52 == weekNumber) {
					readUrl(folderInput, folderOutput, localDate.toString(), weekNumber);
				}
			} catch (Exception e) {
				log.printErr(e);
			}
		}
	}

	public void readUrl(File folderInput, File folderOutput, String sDate, int weekNumber) throws Exception {
		List<PatentDocumentVO> lPatentDocumentVoList = new ArrayList<PatentDocumentVO>();
        PatentDocumentVO patentDocumentVO = new PatentDocumentVO();
        DocumentVO oDocumentVO =  new DocumentVO();
        
		URL url = new URL("https://data.epo.org/publication-server/data-coverage-file?sDate=EPO-"+sDate);
        log.print("reading url : "+url);
        URLConnection conn = url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));;
        String contents = in.lines().collect(Collectors.joining());
        XMLEventReader eventReader = null;
        try {
	        eventReader = XMLInputFactory.newInstance()
					.createXMLEventReader(new ByteArrayInputStream(contents.getBytes()));
	        
			// read the XML document
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				
				if (event.isStartElement()) {
					String localPart = event.asStartElement().getName().getLocalPart();
					switch (localPart) {
					case "country":
						oDocumentVO.setsCountry(getCharacterData(event, eventReader));
						break;
					case "doc-number":
						oDocumentVO.setsDocNumber(getCharacterData(event, eventReader).replaceFirst("^0{0,7}", ""));
						break;
					case "kind":
						oDocumentVO.setSkind(getCharacterData(event, eventReader));
						break;
					case "date":
						oDocumentVO.setsDatePubl(getCharacterData(event, eventReader));
						break;
					}
				} else if (event.isEndElement()) {
					String localPart = event.asEndElement().getName().getLocalPart();
					if (localPart == ("publication-reference")) {
						patentDocumentVO.setPublDocumentVO(oDocumentVO);
						// reset variable
						oDocumentVO = new DocumentVO();
					} else if (localPart == ("application-reference")) {
						patentDocumentVO.setApplDocumentVO(oDocumentVO);
						lPatentDocumentVoList.add(patentDocumentVO);
						// reset variable
						oDocumentVO = new DocumentVO();
						patentDocumentVO = new PatentDocumentVO();
					}
				}
			}
        }catch (Exception e) {
			log.printErr(e);
		}finally {
			eventReader.close();
		}
        
        PatentExtractEPO extractEPO = new PatentExtractEPO(log);
        extractEPO.readZipFileList(folderInput, folderOutput, weekNumber, lPatentDocumentVoList);
		log.print("end");
	}


	public LocalDate getNextWed(LocalDate localDate) throws Exception {
		DayOfWeek dayOfWeek = DayOfWeek.from(localDate); 
		int daysToAdd = dayOfWeek.getValue();
//		System.out.println("Day of the Week on " + localDate + " - " + dayOfWeek.name() + " - " + daysToAdd); 
		if (daysToAdd>3) {
			daysToAdd = 7-dayOfWeek.getValue()+3;
		} else if (daysToAdd<3) {
			daysToAdd = 3-daysToAdd;
		} else {
			daysToAdd = 7;
		}
		localDate = localDate.plusDays(daysToAdd); 
		return localDate;
	}


}
