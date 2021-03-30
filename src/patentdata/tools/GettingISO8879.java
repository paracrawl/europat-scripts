package patentdata.tools;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;

import patentdata.utils.PatentData;


public class GettingISO8879  extends PatentData{

	public GettingISO8879() throws Exception {
		super();
		// TODO Auto-generated constructor stub
	}
	
	public GettingISO8879(String path) throws Exception {
		super(path);
		ReadMahtMLUnicode();

	}
	
	public GettingISO8879(String path, boolean bVerbose) throws Exception {
		super(path, bVerbose);
		ReadMahtMLUnicode();
	}

	HashMap<String, Integer> hISO79Code = new HashMap<String, Integer>();
	HashMap<String, String> hTag = new HashMap<String, String>();
	HashMap<String, String> hMathMLUnicode = new HashMap<String, String>();
	String sPaternMathML = "";
	private String outputDirectory = "";

	public void Start(String inputPath, String outputPath) throws Exception {
		WriteOutputUnicode(outputPath);
	}

	private void Read(String inputFilePath) {
		BufferedReader br = null;
		File inFile = new File(inputFilePath);
//		StringBuffer bufferClean = new StringBuffer();
		StringBuffer bufferPair = new StringBuffer();
		try {

			br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), "UTF-8"));
			String line;
			int i = 1;
			boolean bCleanFlag = false;
			while ((line = br.readLine()) != null) {
				
				bufferPair.append(line+"\n");
				bCleanFlag = true;
				if (i % 500 == 0) {
					DisplaysRemainingISO79Code(bufferPair.toString());
					bufferPair = new StringBuffer();
					bCleanFlag = false;
				}
				i++;
			}
			if (bCleanFlag) {
				DisplaysRemainingISO79Code(bufferPair.toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				br = null;
			}
		}
	}
	
	private void DisplaysRemainingISO79Code(String text) {
		text = StringEscapeUtils.unescapeHtml4(text);
		Pattern pTest = Pattern.compile("(?<unicode>&(\\S+?;)?)");
		Matcher m = pTest.matcher(text);
//		System.out.println("Checking");
		while (m.find()) {
			String key = m.group("unicode");
			if (key.length() > 2 && key.length() < 30 ) {
				int count = 0;
				if (hISO79Code.containsKey(key)) {
					count = hISO79Code.get(key);
				}
				hISO79Code.put(key, ++count);
			}
		}
	}
	
	private void WriteOutputFile(String outputPath) {
		String outputFilePath = common.combine(outputPath, "/ListUnicode.json");
		FileOutputStream outputStream = null;

		
		try {
			Map<String, Integer> map = new TreeMap<>(hISO79Code);
			outputStream = new FileOutputStream(outputFilePath, false);
			JSONObject json = new JSONObject(map);
			outputStream.write(json.toString().getBytes(Charsets.toCharset("utf-8")));
			outputStream.flush();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void WriteOutputFileNot8879(String outputPath) {
		String outputFilePath = common.combine(outputPath, "/ListNot8879Unicode.json");
		FileOutputStream outputStream = null;

		
		HashMap<String, Integer> hNot8879 = new HashMap<String, Integer>();
		
		 for (Map.Entry<String, Integer>  me : hISO79Code.entrySet()) {
//		        System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
		        if (!hMathMLUnicode.containsKey(me.getKey())){
		        	hNot8879.put(me.getKey(), me.getValue());
		        }
		  }
		
		
		try {
			outputStream = new FileOutputStream(outputFilePath, false);
			JSONObject json = new JSONObject(hNot8879);
			outputStream.write(json.toString().getBytes(Charsets.toCharset("utf-8")));
			outputStream.flush();
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	private void ReadMahtMLUnicode() {
		String sJsonText;
		try {
			sJsonText = common.readFile(config._config.UnicodePath);
			if (common.IsEmpty(sJsonText)) {
				return;
			}
			JSONObject jsonObj = common.getJSONObject(sJsonText);
			Iterator<?> keys = jsonObj.keys();
	        while( keys.hasNext() ){
	            String key = (String)keys.next();
	            String value = jsonObj.getString(key);
	            if (!StringUtils.isEmpty(value)) {
//	            	hMathMLUnicode.put(key, StringEscapeUtils.unescapeJava(value));
	            	hMathMLUnicode.put(key, value);
	            	sPaternMathML = sPaternMathML.concat(key + "|");
	            }
	        }
	        if (sPaternMathML.length() > 0)
	        sPaternMathML = sPaternMathML.substring(0, sPaternMathML.length()-1);
	        
		}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
	}
	
	private HashMap<String, String> ReadUnicode(String sFilePath) {
		HashMap<String, String> hResult = new HashMap<String, String>();
		String sJsonText;
		try {
			sJsonText = common.readFile(sFilePath);
			if (common.IsEmpty(sJsonText)) {
				return hResult;
			}
			JSONObject jsonObj = common.getJSONObject(sJsonText);
			Iterator<?> keys = jsonObj.keys();
	        while( keys.hasNext() ){
	            String key = (String)keys.next();
	            String value =  String.valueOf(jsonObj.getInt(key));
	            if (!StringUtils.isEmpty(value)) {
	            	hResult.put(key, value);
	            }
	        }
		}catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
		
		return hResult;
	}
	
	
	private void WriteOutputUnicode(String outputPath) {
		String outputFilePath = common.combine(outputPath, "/Result.json");
		FileOutputStream outputStream = null;
		System.out.println(outputFilePath);
		
		HashMap<String, String> hNot8879 = new HashMap<String, String>();
		HashMap<String, String> tempFile1 = new HashMap<String, String>();
		tempFile1 = ReadUnicode("/home/ramoslee/work/EPOOPS/testing/20200710/2001-2012/ListUnicode.json");
		
		 for (Map.Entry<String, String>  me : tempFile1.entrySet()) {
//		        System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
			 	String key = me.getKey();
		        if (hMathMLUnicode.containsKey(key)){
		        	hNot8879.put(key, hMathMLUnicode.get(key));
		        }else if (key.indexOf("&#x") >= 0) {
		        	String sVal = key.substring(3,key.length()-1);
		        	sVal = String.format("%1$" + 4 + "s", sVal).replace(' ', '0');
		        	hNot8879.put(key, "\\u" + sVal);
		        }else {
		        	hNot8879.put(key, "@Find@");
		        }
		  }
		 
		 tempFile1 = ReadUnicode("/home/ramoslee/work/EPOOPS/testing/20200710/ListUnicode.json");
			
		 for (Map.Entry<String, String>  me : tempFile1.entrySet()) {
//		        System.out.println("Key: "+me.getKey() + " & Value: " + me.getValue());
			 	String key = me.getKey();
		        if (hMathMLUnicode.containsKey(key)){
		        	hNot8879.put(key, hMathMLUnicode.get(key));
		        }else if (key.indexOf("&#x") >= 0) {
		        	String sVal = key.substring(3, key.length()-1);
		        	sVal = String.format("%1$" + 4 + "s", sVal).replace(' ', '0');
		        	hNot8879.put(key, "\\u" + sVal);
		        }else {
		        	hNot8879.put(key, "@Find@");
		        }
		  }
		
		
		try {
			outputStream = new FileOutputStream(outputFilePath, false);
			JSONObject json = new JSONObject(hNot8879);
			outputStream.write(json.toString().getBytes(Charsets.toCharset("utf-8")));
			outputStream.flush();
			
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			try {
				outputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}

}
