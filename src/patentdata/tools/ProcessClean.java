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

public class ProcessClean extends PatentData{

	private Object _objectWorker = new Object();
	HashMap<String, String> hTag = new HashMap<String, String>();
	HashMap<String, String> hISO79Code = new HashMap<String, String>();
	HashMap<String, String> hMathMLUnicode = new HashMap<String, String>();
	String sPaternMathML = "";
    

	public ProcessClean(String path) throws Exception {
		super(path);
		ReadMahtMLUnicode(path);
		pareparePattern();
	}
	
	public ProcessClean(String path, boolean bVerbose) throws Exception {
		super(path, bVerbose);
		ReadMahtMLUnicode(path);
		pareparePattern();
	}
	
	private String outputDirectory = "";
	private Pattern pRemoveInline;
	private Pattern pReplaceUnprocessable1, pReplaceUnprocessable2;
	private Pattern pInlineSpacing;
	private Pattern pBullet, pBullet1;
	private Pattern pBracketOpen;
	private Pattern pBracketClose;
	private Pattern pSpChar, pSpChar2;
	private Pattern pRemainingTags0, pRemainingTags1, pRemainingTags2, pRemainingTags3, pRemainingTags4,
			pRemainingTags5, pRemainingTags6, pRemainingTags7, pRemainingTags8, pRemainingTags9, pRemainingTags10;
	private Pattern pMathML;


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

	private void ReadMahtMLUnicode(String sPath) {
		String sJsonText;
		try {
			sJsonText = common.readFile(configInfo.UnicodePath);
			if (common.IsEmpty(sJsonText)) {
				return;
			}
			JSONObject jsonObj = common.getJSONObject(sJsonText);
			Iterator<?> keys = jsonObj.keys();
	        while( keys.hasNext() ){
	            String key = (String)keys.next();
	            String value = jsonObj.getString(key);
	            if (!StringUtils.isEmpty(value)) {
	            	hMathMLUnicode.put(key, StringEscapeUtils.unescapeJava(value));
	            	sPaternMathML = sPaternMathML.concat(key + "|");
	            }
	        }
	        if (sPaternMathML.length() > 0)
	        sPaternMathML = sPaternMathML.substring(0, sPaternMathML.length()-1);
	        
//	        System.out.println(sPaternMathML);
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
				outputStream.write(sText1.toString().getBytes(Charsets.toCharset("utf-8")));
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
		
		// Remove inline unwanted
		pRemoveInline = Pattern
				.compile("(<\\?delete-start.*?(<\\?delete-end).*?>)|<script .+?>.+?<\\/script>|<!--.+?-->");

		// Replace unprocessable content with datatype markers so that alignment can
		// still work
		pReplaceUnprocessable1 = Pattern.compile("(<maths.+?>.+?<\\/maths>|<MATH-US.+?>.+?<\\/MATH-US>)");
		pReplaceUnprocessable2 = Pattern.compile("(<[?]in-line-formulae .+?[?]>).+?(<[?]in-line-formulae .+?[?]>)");
//		pReplaceUnprocessable3 = Pattern.compile("(<document-id .+?>).+?(<\\/document-id>)");

		// Add spacing
		String sInline = "a|abbr|acronym|b|bdo|big|br|button|cite|dfn|em|i|input|kbd|label|map|object|output|q|samp|select|small|span|strong|time|tt|var";
		String sInlineDtd = "|b|i|u .+?|u|o .+?|o|dl .+?|dl|ol .+?|ol|patcit .+?|patcit|nplcit .+?|nplcit|rel-passage|text|ul .+?|ul";
		String sInlineClaim = "|claim .+?|claim|claim-ref .+?|claim-ref|smallcaps|pre .+?|pre|amended-claims .+?|amended-claims|amended-claims|amended-claims-statement .+?|amended-claims-statement|claims-statement|chemistry .+?|chemistry|figref .+?|figref|crossref .+?|crossref";
		String sInlineTable = "|tables .+?|tables|table .+?|table|tgroup .+?|tgroup|thead .+?|thead|tbody .+?|tbody|row .+?|row";
		String sInlineAbstract = "|abstract .+?|abstract|abst-problem .+?|abst-problem|abst-solution .+?|abst-problem";
		String sInLineEmpty = "<(colspec .+?)\\/>|<(chem .+?)\\/>|<(img .+?)\\/>|<[?].+?[?]>|<entry/>";
		String sSup = "sup2|sub2|sup|sub";
		String uspto = "|PDAT|HIL|SB|CHEMCDX .+?|CHEMMOL .+?|CHEMMOL|CHEM-US .+?|CHEM-US|EMI .+?|EMI|CLREF .+?|CLREF|"
				+ "ITALIC|CL|H .+?|F|BOLD|CLREF|TABLE-.+?|CLMSTEP.+?|H|COLSPEC.+?|TBODY.+?|TGROUP.+?|FGREF.+?|SMALLCAPS.+?|TABLE .+?|CUSTOM-CHARACTER .+?"
				+ "|S-1-.+?|S-2-.+?|SP";
		pInlineSpacing = Pattern.compile("<\\/(" + sInline + sInlineClaim + sInlineTable + sInlineAbstract + sInlineDtd + uspto
				+ ")>|" + "<(" + sInline + sInlineClaim + sInlineTable + sInlineAbstract + sInlineDtd + uspto
				+ ")>|<(\\/{0,1})(" + sSup + ")>" + "|<(" + sSup + ")>|<br\\/>|" + sInLineEmpty);
//		String sBlock = "p .+?|p|entry .+?|entry|dd|dt|description-of-drawings .+?|description-of-drawings|heading .+?|heading|title|claim-text|li .+?|li";
//		String sUsptoBlock = "|PARA .+?|STEXT|CLM .+?|S-1 .+?|S-2 .+?|S-2|S-1";
//		pRemainingTags0 = Pattern.compile("<("+sBlock+ sUsptoBlock+")>|<\\/(" + sBlock +sUsptoBlock + ")>");
		pRemainingTags1 = Pattern.compile("(<\\/p><p>){2,}|(<\\/p><p>)[ ]*(<\\/p><p>)");
		pRemainingTags2 = Pattern.compile("([ ]{2,})");
		pRemainingTags3 = Pattern.compile("(<\\/p><p>)[ ]*\\t");
		pRemainingTags4 = Pattern.compile("(<\\/p><p>[ ]*)\n");
	    pRemainingTags5 = Pattern.compile("\\t[ ]*<\\/p><p>");
	    pRemainingTags6 = Pattern.compile("<p>[ ]<\\/p>");
//	    pRemainingTags7 = Pattern.compile("<.+?>");
	    pRemainingTags7 = Pattern.compile("<(?!\\/?p(?=>|\\s[a-z0-9\\W ]*>))\\/?[a-z0-9\\W ]*?>");
	    pRemainingTags8 = Pattern.compile("((?<rpl>:|;)(<br>|<br/>))");
	    pRemainingTags9 = Pattern.compile("<p[ ]+id.+?>");
	    pRemainingTags10 = Pattern.compile("(<\\/p>){2,}");
	    
	    pRemainingTags0 = Pattern.compile("(<(?<rpl>[0-9]+(\\-*|[ ]*))>)|(<(" + sSup + ")>(?<rpl1>.+?)<\\/(" + sSup + ")>)");
	    
	    String sBullet3 = "(([a-z]{1}[0-9]*\\.*[ ]*)?((([0-9]{1,10}\\.[ ]*){0,})([0-9]{1,10})(\\.|[)\\]]{0,3})))";
	    String sBullet1 = "|((([0-9]{1,})|([a-z]{1})|(XXXVIIIXC|LXXXVIII|CXCVIII|LXXVIII|LXXXIII|LXXXVII|XXXIII|XCVIII|XXXVII|LXVIII|LXIXXC|LXXIII|DCCVII|DCCCXC|LXXVII|XLVIII|LXXXII|LXXXIV|LXXXVI|LXXXIX|XXVIII|XXXII|LXIII|XXXIV|XCVII|XXXVI|LXVII|XXXIX|LXXII|XLIII|LXXIV|LXXVI|XLVII|MDCCC|LXXIX|XVIII|LXXXI|XXIII|LXXXV|XXVII|LVIII|XCIII|VIII|XCIV|LXIV|XCVI|XXXV|LXVI|XCIX|LXXI|XLII|DXXX|XIII|XLIV|LXXV|XLVI|XVII|XLIX|LXXX|XXII|LIII|XXIV|XXVI|LVII|XXIX|XCII|XXXI|LXII|XCV|III|LXV|VII|LXX|XLI|XII|XIV|XLV|XVI|XIX|XXI|LII|LIV|XXV|LVI|LIX|XCI|XXX|LXI|II|IV|VI|IX|XL|DI|XI|DL|XV|MD|CM|XX|LI|LV|XC|LX|I|V|X|L))[.\\])]{0,3}\\.)";
	    String sBullet2 = "|(-{1,}|[+]{1,})|(([0-9]{1,10}\\.[ ]*){0,10}[0-9]{1,10}\\([0-9a-zA-Z]{1,2}([\\)]?[\\,]?[\\(]?[0-9a-zA-Z]{1,2}){0,5}[)]{1,3})";
//	    String sBullet3 = "|(([a-z]{1}\\.)?((([0-9]{1,10}\\.){0,})([0-9]{1,10})([)\\]]{1,3})))";
	    String sBullet4 = "|((([(\\[]{1,3})(([0-9]{1,10})|([a-z]{1,3}))([)\\]]{1,3})){1,})";
	    String sBullet5 = "|((((XXXVIIIXC|LXXXVIII|CXCVIII|LXXVIII|LXXXIII|LXXXVII|XXXIII|XCVIII|XXXVII|LXVIII|LXIXXC|LXXIII|DCCVII|DCCCXC|LXXVII|XLVIII|LXXXII|LXXXIV|LXXXVI|LXXXIX|XXVIII|XXXII|LXIII|XXXIV|XCVII|XXXVI|LXVII|XXXIX|LXXII|XLIII|LXXIV|LXXVI|XLVII|MDCCC|LXXIX|XVIII|LXXXI|XXIII|LXXXV|XXVII|LVIII|XCIII|VIII|XCIV|LXIV|XCVI|XXXV|LXVI|XCIX|LXXI|XLII|DXXX|XIII|XLIV|LXXV|XLVI|XVII|XLIX|LXXX|XXII|LIII|XXIV|XXVI|LVII|XXIX|XCII|XXXI|LXII|XCV|III|LXV|VII|LXX|XLI|XII|XIV|XLV|XVI|XIX|XXI|LII|LIV|XXV|LVI|LIX|XCI|XXX|LXI|II|IV|VI|IX|XL|DI|XI|DL|XV|MD|CM|XX|LI|LV|XC|LX|I|V|X|L)|([a-z]{1}))\\.[ ]*)((([0-9]{1,10}\\.[ ]*){0,})([0-9]{1,10})))";
	    String sBullet6 = "|(([0-9]{1,10}\\.[ ]*){1,}([0-9]{1,10}))";
	    String sBullet7 = "|([a-z]{1}[0-9]{0,10}(\\.|([)\\]])))";
	    String sBullet8 = "|((\\(|\\[|\\{)([a-z]{1}|[0-9]{1,10})(\\.[ ]*([a-z]{1}|[0-9]))*(\\)|\\]|\\}))\\.*";

	    String sBullet = sBullet3 + sBullet1 + sBullet2 + sBullet4 + sBullet5 + sBullet6 + sBullet7 + sBullet8;
//	    String sBullet = sBullet3+ sBullet1 ;
	    pBullet = Pattern.compile("(<p>[ ]*)("+ sBullet + ")([ ]:+[ ]*|[ ])"
	    						, Pattern.CASE_INSENSITIVE);
	    pBullet1 = Pattern.compile("(\\t|<br>[ ]*|<br\\/>[ ]*)("+ sBullet + ")([ ]*|<br>|<br\\/>)"
				, Pattern.CASE_INSENSITIVE);
	    pBracketOpen = Pattern.compile("(\\(|\\{|\\[)[ ]");
	    pBracketClose = Pattern.compile("[ ](\\)|\\}|\\])");
	    pSpChar = Pattern.compile("((\\u2003){3,})");
	    pSpChar2 = Pattern.compile("(\\u2003)+");
	    
	    // MathML
	    pMathML = Pattern.compile("("+ sPaternMathML + ")");
	}

	private String cleanTags(String text) {
		synchronized (_objectWorker) {
			try {
				// Bullet
				text = pBullet1.matcher(text).replaceAll("$1</p><p>");
				text = pRemainingTags0.matcher(text).replaceAll("${rpl}${rpl1}");
				text = pRemainingTags8.matcher(text).replaceAll("${rpl}</p><p>");
				// text = pSpChar2.matcher(text).replaceAll(" ");
				
				// Remove inline unwanted
				text = pRemoveInline.matcher(text).replaceAll(" ");
		
				// Remove Inline
				text = pInlineSpacing.matcher(text).replaceAll(" ");
		
				// Replace unprocessable content with datatype markers so that alignment can
				text = pReplaceUnprocessable1.matcher(text).replaceAll(" EPMATHMARKEREP ");
				text = pReplaceUnprocessable2.matcher(text).replaceAll(" EPFORMULAMARKEREP ");
				//text = pReplaceUnprocessable3.matcher(text).replaceAll(" EPDOCIDMAKER ");
//				text = pRemainingTags0.matcher(text).replaceAll("@STTAG@");
				text = pRemainingTags2.matcher(text).replaceAll(" ");
//				text = pRemainingTags0.matcher(text).replaceAll("</p><p>");
				text = pRemainingTags9.matcher(text).replaceAll("</p><p>");
				text = pRemainingTags7.matcher(text).replaceAll("</p><p>");
				// text = pSpChar.matcher(text).replaceAll("</p><p>");

				text = pRemainingTags6.matcher(text).replaceAll("");
				text = pRemainingTags1.matcher(text).replaceAll("</p><p>");
		
				text = pRemainingTags3.matcher(text).replaceAll("</p>\t");
				text = pRemainingTags5.matcher(text).replaceAll("\t<p>");
				text = pRemainingTags4.matcher(text).replaceAll("</p>\n");
				text = pBracketOpen.matcher(text).replaceAll("$1");
				text = pBracketClose.matcher(text).replaceAll("$1");
				// text = pSpChar2.matcher(text).replaceAll(" ");
				
				text = pBullet.matcher(text).replaceAll("<p>");
				// to handle the bullet contain space: 10. 10. 1. 2. 12.
				text = pBullet.matcher(text).replaceAll("<p>");
				text = pSpChar2.matcher(text).replaceAll(" ");
				text = pRemainingTags1.matcher(text).replaceAll("</p><p>");
				text = pRemainingTags10.matcher(text).replaceAll("</p>");
				
				text = StringEscapeUtils.unescapeHtml4(text);
				text = cleanMathMLUnicode(text);
			}catch (Exception e) {
				printLog.writeError("cleanTags", e);
			}
		}
		return text; 

	}
	
	private String cleanMathMLUnicode(String text) {
		
		if (hMathMLUnicode.size() > 0) {
			 Matcher matcher = pMathML.matcher(text);
			  int count =0;
			  HashMap<String, String> mahtMLDoneMap = new HashMap<String, String>();
			  while(matcher.find()) {
				  String key= matcher.group(0);
				  if (!mahtMLDoneMap.containsKey(key) && key.length() > 0 && hMathMLUnicode.containsKey(key)) {
					  mahtMLDoneMap.put(key, key);
					  text = text.replaceAll(key, hMathMLUnicode.get(key));
				  }
			  }
		}
		
		return text;
	}
	
	private void DisplaysRemainingTag(String text) {
		Pattern pTest = Pattern.compile("<.+?>");
		Matcher m = pTest.matcher(text);
		printLog.writeDebugLog("Checking");
		while (m.find()) {
			hTag.put(m.group(0), m.group(0));
		}
		
//		 for (Map.Entry<String, String>  me : hTag.entrySet()) {
//	          System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
//	     }
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
