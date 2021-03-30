package patentdata.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import patentdata.utils.Common;
import patentdata.utils.PatentData;

public class ProcessCleanASCII extends PatentData{

	private Object _objectWorker = new Object();
	private static String lineMarker = "<br\\/>";
	HashMap<String, String> hTag = new HashMap<String, String>();
	HashMap<String, String> hISO79Code = new HashMap<String, String>();
	HashMap<String, String> hAsciiUnicode = new HashMap<String, String>();
	String sPatternAscii = "";
    

	public ProcessCleanASCII(String path) throws Exception {
		super(path);
		ReadAsciiUnicode();
		pareparePattern();
	}
	
	public ProcessCleanASCII(String path, boolean bVerbose) throws Exception {
		super(path, bVerbose);
		ReadAsciiUnicode();
		pareparePattern();
	}
	
	private String outputDirectory = "";
	private Pattern pRemainingTags0, pRemainingTags1, pRemainingTags2, pRemainingTags3, pRemainingTags4,
			pRemainingTags5, pRemainingTags6, pRemainingTags7;
	private Pattern pAsciiUnicode;
	private Pattern pParagraph;
	private Pattern pNewLineSpace;
	private Pattern pParagraphSpace;
	private Pattern pDoubleSpace;
	private Pattern pLineBreak;
	private Pattern pEndLine;
	private Pattern pRemove;
	private Pattern pRemoveSpaceEndLine;
	private Pattern pLessSymb, pGreaterSymb;


	public void Start(String inputPath, String outputPath) throws Exception {
		try {
			// input
			if (!new File(inputPath).exists()) {
				throw new Exception("Input path does not exist.");
			}
			printLog.writeDebugLog("Input: " + inputPath);
			File oInput = new File(inputPath);

			// output
			this.outputDirectory = outputPath;
			if (!new File(this.outputDirectory).exists()) {
				new File(this.outputDirectory).mkdirs();
			}
			printLog.writeDebugLog("Output: " + this.outputDirectory);

			if (inputPath.equals(this.outputDirectory)) {
				throw new Exception("Input and Output path cannot be the same.");
			}

			if (oInput.isDirectory()) {
				File[] files = oInput.listFiles();
				for (File file : files) {

					try {
						printLog.writeDebugLog("Processing File: " + file.getName());
						if (!file.isDirectory())
							Read(file.getPath());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} else {
				printLog.writeDebugLog("Processing File: " + oInput.getName());
				Read(oInput.getPath());
			}
		} finally {
			printLog.writeDebugLog("Done");
		}
	}

	private void ReadAsciiUnicode() {
		String sJsonText;
		try {
			sJsonText = common.readFile(configInfo.AsciiUnicodePath);
			if (common.IsEmpty(sJsonText)) {
				return;
			}
			JSONObject jsonObj = common.getJSONObject(sJsonText);
			Iterator<?> keys = jsonObj.keys();
	        while( keys.hasNext() ){
	            String key = (String)keys.next();
	            String value = jsonObj.getString(key);
	            if (!StringUtils.isEmpty(value)) {
	            	hAsciiUnicode.put(key, StringEscapeUtils.unescapeJava(value));
	            	sPatternAscii = sPatternAscii.concat(key + "|");
	            }
	        }
	        if (sPatternAscii.length() > 0)
	        sPatternAscii = sPatternAscii.substring(0, sPatternAscii.length()-1);
	        
	        System.out.println(sPatternAscii);
		}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
	}
	
	private void Read(String inputFilePath) {
		BufferedReader br = null;
		FileOutputStream outputStream = null;
		File inFile = new File(inputFilePath);
//		StringBuffer bufferClean = new StringBuffer();
		StringBuffer bufferPair = new StringBuffer();
		int fileSize = (int) inFile.length();
		int iNumberByte = 0;
		DecimalFormat df=new DecimalFormat("0.00");
		long start = System.currentTimeMillis();

		try {
			String outputFilePath = common.combine(this.outputDirectory, inFile.getName());
			outputStream = new FileOutputStream(outputFilePath, false);

			br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), "UTF-8"));
			String line;
			int i = 1;
			while ((line = br.readLine()) != null) {
				
				bufferPair.append(line+"\n");
				String sText1 = cleanTags(bufferPair.toString());
				iNumberByte = iNumberByte + bufferPair.length();
				outputStream.write((sText1 + "\n").getBytes(Charsets.toCharset("utf-8")));
				outputStream.flush();
				bufferPair = new StringBuffer();
				if (i % 200 == 0) {
					double dp = ((double)iNumberByte/ (double)fileSize)*100;
					printLog.writeDebugLog("Percentage of complete:\t" + df.format(dp) +"%.");
				}
				i++;
			}
			printLog.writeDebugLog("Percentage of complete:\t100%.");

			//DisplaysRemainingTag(bufferClean.toString());
		} catch (FileNotFoundException e) {
			printLog.writeDebugLog("Error(Read): " + common.getStackTrace(e));
		} catch (IOException e) {
			printLog.writeDebugLog("Error(Read): " + common.getStackTrace(e));
		}finally {
//			 for (Map.Entry<String, String>  me : hTag.entrySet()) {
//		          System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
//		     }
//			 
//			 System.out.println("ISOCode");
//			 for (Map.Entry<String, String>  me : hISO79Code.entrySet()) {
//		          System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
//		     }
			printLog.writeDebugLog("Processing tiem: " + (System.currentTimeMillis() - start) + " ms.");
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					printLog.writeDebugLog("Error(Read.finally): " + common.getStackTrace(e));
				}
				outputStream = null;
			}
			
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					printLog.writeDebugLog("Error(Read.finally): " + common.getStackTrace(e));
				}
				br = null;
			}
		}
	}

	private void pareparePattern() {
		
		String sParagraphTag = "PAR|PAC|PAL|PA[0-9]+|FNT|TBL|TBL[0-9]+|EQU|DCLM|CLMS|STM|DETD|DRWD|BSUM|GOVT|ABST";
		pParagraph = Pattern.compile("(\\t|" +lineMarker+ ")("+ sParagraphTag + ")");
		
		pNewLineSpace = Pattern.compile("(" +lineMarker+ "[ ]{5}|\\.(sub|sup|sbsb|sbsp|spsp|spsb)\\.)");
		pRemove = Pattern.compile("(\\t|" +lineMarker+ ")(NUM[ ]{2}[0-9]{1,}\\.*)");
		pParagraphSpace = Pattern.compile("(<p>)([ ]{1,})");
		pDoubleSpace = Pattern.compile("([ ]{2,})");
		pLineBreak = Pattern.compile("(" +lineMarker+"|$|\\n)");
		pEndLine = Pattern.compile("($|\\n)");
		
		pRemainingTags1 = Pattern.compile("(<\\/p><p>){2,}|(<\\/p><p>)[ ]*(<\\/p><p>)");
	    pRemainingTags5 = Pattern.compile("\\t[ ]*<\\/p><p>");
		pRemainingTags4 = Pattern.compile("(<\\/p><p>[ ]*)(\\n|$)");
		
		pLessSymb = Pattern.compile("(<)([^br])");
		pGreaterSymb = Pattern.compile("([^\\/])(>)");
		pRemoveSpaceEndLine = Pattern.compile("([ ]+)("+lineMarker + ")");
		
	    // MathML
	    pAsciiUnicode = Pattern.compile("(\\.)("+ sPatternAscii + ")(\\.)");

	}

	private String cleanTags(String text) {
		synchronized (_objectWorker) {
			try {

				
				text = pLessSymb.matcher(text).replaceAll("&lt;2$");
				text = pGreaterSymb.matcher(text).replaceAll("1$&gt;");
				text = pRemoveSpaceEndLine.matcher(text).replaceAll("$2");
				
				text = pParagraph.matcher(text).replaceAll("$1</p><p>");
				text = pNewLineSpace.matcher(text).replaceAll("");
				text = pRemove.matcher(text).replaceAll("");
				text = pParagraphSpace.matcher(text).replaceAll("$1");
				text = pEndLine.matcher(text).replaceAll("</p><p>$1");
				text = pDoubleSpace.matcher(text).replaceAll("</p><p>");
				text = pLineBreak.matcher(text).replaceAll("</p><p>");
				
				text = pRemainingTags1.matcher(text).replaceAll("</p><p>");
				text = pRemainingTags5.matcher(text).replaceAll("\t<p>");
				text = pRemainingTags4.matcher(text).replaceAll("</p>$2");

				text = cleanAsciiUnicode(text);
			}catch (Exception e) {
				printLog.writeError("cleanTags", e);
			}
		}
		return text; 

	}
	
	private String cleanAsciiUnicode(String text) {
		
		if (hAsciiUnicode.size() > 0) {
			 Matcher matcher = pAsciiUnicode.matcher(text);
			  int count =0;
			  HashMap<String, String> mahtMLDoneMap = new HashMap<String, String>();
			  while(matcher.find()) {
				  String key= matcher.group(2);
				  if (!mahtMLDoneMap.containsKey(key) && key.length() > 0 && hAsciiUnicode.containsKey(key)) {
					  mahtMLDoneMap.put(key, key);
					  text = text.replaceAll("\\." + key + "\\.", hAsciiUnicode.get(key));
				  }
			  }
		}
		
		return text;
	}
	
	
	private void DisplaysRemainingISO79Code(String text) {
		Pattern pTest = Pattern.compile("(&.{1,20};)");
		Matcher m = pTest.matcher(text);
		printLog.writeDebugLog("Checking");
		while (m.find()) {
			hISO79Code.put(m.group(0), m.group(0));
		}
		
//		 for (Map.Entry<String, String>  me : hTag.entrySet()) {
//	          System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
//	     }
	}
}
