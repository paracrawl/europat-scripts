package patentdata.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.commons.codec.Charsets;
import org.apache.commons.lang3.StringUtils;

import patentdata.utils.Common;
import patentdata.utils.PatentData;
import patentdata.utils.StreamGobbler;

public class SentenceSegmentSingleLang extends PatentData{

	Common common = new Common();
	private int bufferPipe = 500000;

	public SentenceSegmentSingleLang(String path) throws Exception {
		super(path);
	}
	
	public SentenceSegmentSingleLang(String path, boolean bVerbose) throws Exception {
		super(path, bVerbose);
	}
	
	public SentenceSegmentSingleLang(String path, boolean bVerbose, int bufferPipe) throws Exception {
		super(path, bVerbose);
		if (bufferPipe > 0)
			this.bufferPipe = bufferPipe;
	}
	
	public void Run(String sInputFilePath, String sOutputPath, String sLangSource) throws Exception {
		printLog.writeDebugLog("Start Sentence Segment.");
		printLog.writeDebugLog("Pipe Buffer: " + this.bufferPipe);
		try {
			// input
			if (!new File(sInputFilePath).exists()) {
				throw new Exception("Input path does not exist.");
			}
			printLog.writeDebugLog("Input: " + sInputFilePath);
			File oInput = new File(sInputFilePath);

			// output
			if (!new File(sOutputPath).exists()) {
				new File(sOutputPath).mkdirs();
			}
			printLog.writeDebugLog("Output Directory: " + sOutputPath);

			if (sInputFilePath.equals(sOutputPath)) {
				throw new Exception("Input and Output path cannot be the same.");
			}

			if (oInput.isDirectory()) {
				File[] files = oInput.listFiles();
				for (File file : files) {

					try {
						printLog.writeDebugLog("Processing File: " + file.getName());
						if (!file.isDirectory())
							ReadFile(file.getPath(), sOutputPath, sLangSource);
					} catch (Exception e) {
						printLog.writeError("Run", e);
					}
				}
			} else {
				printLog.writeDebugLog("Processing File: " + oInput.getName());
				ReadFile(oInput.getPath(), sOutputPath, sLangSource);
			}
		} finally {
			printLog.writeDebugLog("End Sentence Segment.");
			System.out.println("End Sentence Segment.");
		}
		
	}
	
	public void ReadFile(String sInputFilePath, String sOutputPath, String sLangSource) {
		
		BufferedReader br = null;
		FileOutputStream outputStream = null;
		File inFile = new File(sInputFilePath);
		
		StringBuilder sbSource = new StringBuilder();
		printLog.writeDebugLog("BinSegmentPath:" + configInfo.BinSegmentPath );
		printLog.writeDebugLog("SrxFilePath:" + configInfo.SrxFilePath );
		printLog.writeDebugLog("Processing File Path : " + sInputFilePath);
		long start = System.currentTimeMillis();

//		printLog.writeDebugLog("Output Directory : " + sOutputPath);
		try {
			if(!common.IsExist(inFile)) {
				printLog.writeDebugLog("the input does not exist (" + sInputFilePath + ")."); 
				return;
			}
			if (!common.IsExist(sOutputPath)) {
				new File(sOutputPath).mkdirs();
			}
			
			int fileSize = (int) inFile.length();
			int iNumberByte = 0;
			DecimalFormat df=new DecimalFormat("0.00");
			String sSourceLang = sLangSource;

			String outputFilePath = common.combine(sOutputPath, inFile.getName());
			outputStream = new FileOutputStream(outputFilePath, false);

			br = new BufferedReader(new InputStreamReader(new FileInputStream(inFile), "UTF-8"));
			String line;
			int j = 0;
			int i = 0;
			boolean bCleanFlag = false;
			while ((line = br.readLine()) != null) {
				i++;
				if (!StringUtils.isEmpty(line)) {
					String sLineSource = line+"\n";
					if (sbSource.length() > bufferPipe || (sbSource.length() > 0 && sbSource.length() + sLineSource.length() > bufferPipe)) 
					{
//						if (i >= 60)
						PrecessSentenceSegementWriteOupput(sSourceLang, j, sbSource, outputStream);
						iNumberByte = iNumberByte + sbSource.length();
						double dp = ((double)iNumberByte/ (double)fileSize)*100;
						printLog.writeDebugLog("Percentage of complete:\t" + df.format(dp) +"%.");

						j = 0;
						sbSource = new StringBuilder();
//						break;
					}
					sbSource.append(sLineSource);
					j++;
					bCleanFlag = true;
				}
			}
			if (bCleanFlag) {
				PrecessSentenceSegementWriteOupput(sSourceLang, j, sbSource, outputStream);
				printLog.writeDebugLog("Percentage of complete:\t 100%.");
			}
		} catch (Exception e) {
			printLog.writeError("ReadPairingFile", e);

		}finally {
			
			printLog.writeDebugLog("Processing tiem: " + (System.currentTimeMillis() - start) + " ms.");
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {

				}
				outputStream = null;
			}
			
			if (null != br) {
				try {
					br.close();
				} catch (IOException e) {

				}
				br = null;
			}
		}

	}
	
	private void PrecessSentenceSegementWriteOupput(String sLangSource, int j, StringBuilder sbSource, OutputStream outputStream) {
		StringBuilder sbOutputSource = new StringBuilder();
		Pattern pParaTabStart = Pattern.compile("(<p><br\\/>)");
		Pattern pParaTabEnd = Pattern.compile("((<br\\/><\\/p><br\\/>)|<br\\/><\\/p>)|(<\\/p><br\\/>)");
		if (j==1 && (sbSource.length() > bufferPipe)){
			sbOutputSource.append(SeparateCallSentenceSegment(sbSource, sLangSource));
		}else {
			sbOutputSource.append(RunScriptWithInputStream(sbSource, sLangSource));
		}
		
		Scanner scanSource = null;
		try {
			sbOutputSource = new StringBuilder(pParaTabStart.matcher(sbOutputSource).replaceAll("<p>"));
			sbOutputSource = new StringBuilder(pParaTabEnd.matcher(sbOutputSource).replaceAll("</p>"));
			
			
			scanSource = new Scanner(sbOutputSource.toString()); 
			while (scanSource.hasNextLine()){
				 String sLineSoure = scanSource.nextLine();
				 String sLine = sLineSoure + "\n";
				 outputStream.write(sLine.getBytes(Charsets.toCharset("utf-8")));
				 outputStream.flush();
			}
		}catch (Exception e){
			e.printStackTrace();
			printLog.writeError("PrecessSentenceSegementWriteOupput", e);
		}finally {
			if (null != scanSource)
				scanSource.close();
		}
	}
	private StringBuilder SeparateCallSentenceSegment(StringBuilder sbInput, String sLanguage) {
		StringBuilder sb = new StringBuilder();
		int iSourceLeght = sbInput.length();
		int sizeSource = (int) Math.ceil((double)iSourceLeght/bufferPipe);
		
		for (int i = 0; i < sizeSource; i++){
			StringBuilder sbtemp = new StringBuilder();
			int iEnd = ( i == (sizeSource-1) ? (i*bufferPipe)+iSourceLeght%bufferPipe : (i+1)*bufferPipe);
			sbtemp.append(sbInput.substring(i*bufferPipe, iEnd));

			StringBuilder sbtempOutput = new StringBuilder();
			sbtempOutput.append(RunScriptWithInputStream(sbtemp, sLanguage));
			sbtempOutput = new StringBuilder(sbtempOutput.substring(0, sbtempOutput.length() - "<br/>\n".length()));
			sb.append(sbtempOutput);
			
		}
		return sb;

	}
	
	public StringBuilder RunScriptWithInputStream(StringBuilder inputText, String sLanguage) {
		Process proc = null;
		StringBuffer sbError = null;
		StringBuilder sb = new StringBuilder();
		try {

			String sCommand[] = new String[] {configInfo.BinSegmentPath,  
					"-l " + sLanguage //, "-u" //, "-p"
					, "-s " + configInfo.SrxFilePath
					, "-e \"<br/>\""
					, "buffer-length 10485760"
			};
			

			
			sb = new StringBuilder();
			sbError = new StringBuffer();

			proc = Runtime.getRuntime().exec(sCommand);
			
			//Deadlock on stderr from pdftohtml
			StreamGobbler errorStreamGobbler = new StreamGobbler("ErrorStream", proc.getErrorStream());
			StreamGobbler inputStreamGobbler = new StreamGobbler("InputStreamST"
					, proc.getInputStream()
					, proc.getOutputStream()
					, new StringBuffer(inputText.toString()));

			inputStreamGobbler.SetOutputStreamFromStringBuilder();
			Thread tInput = new Thread(inputStreamGobbler);
			Thread tError = new Thread(errorStreamGobbler);
			tError.start();
			tInput.start();
			
			if (!proc.waitFor(60, TimeUnit.SECONDS)) {
				// timeout - kill the process.
				proc.destroyForcibly();
			}
//			System.out.println("Test1");
//			tError.join();
			tInput.join();
//			System.out.println("Test2");

			errorStreamGobbler.CloseBuffer();
			inputStreamGobbler.CloseBuffer();
			
			sbError = errorStreamGobbler.getSbText();
			sb = new StringBuilder(inputStreamGobbler.getSbText().toString());
			
//			System.out.println("ERROR: : " + sbError);
//			System.out.println("Output: : " + sb);
			
			errorStreamGobbler.CloseBuffer();
			inputStreamGobbler.CloseBuffer();
			if (sbError.length() > 0) {
				sb = inputText;
				throw new Exception(sbError.toString());
			}else {
				// success
				return sb;
			}
			
		}catch (Exception e) {
			// TODO: handle exception
			sb = inputText;
			printLog.writeError("RunScriptWithInputStream", e);
		}finally {
			proc.destroy();
		}
		return sb;
	}
	
	public List RunScriptWithInputStreamThread(StringBuilder inputTextSource, StringBuilder inpuTextTarget, String sLanguageSource, String sLanguageTarget) {
		Process procSource = null;
		Process procTarget = null;
		StringBuffer sbErrorSource = null;
		StringBuffer sbErrorTarget = null;
		StringBuffer sb = new StringBuffer();
		StringBuffer sbTarget = new StringBuffer();
		List<StringBuilder> result = new ArrayList<StringBuilder>();
		try {

			String sCommand[] = new String[] {configInfo.BinSegmentPath,  
					"-l " + sLanguageSource //, "-u" //, "-p"
					, "-s " + configInfo.SrxFilePath
					, "-e \"<br/>\""
//					, "buffer-length 10485760"
			};
			
			String sCommandTarget[] = new String[] {configInfo.BinSegmentPath,  
					"-l " + sLanguageTarget //, "-u" //, "-p"
					, "-s " + configInfo.SrxFilePath
					, "-e \"<br/>\""
//					, "buffer-length 10485760"
			};
			

			
			sb = new StringBuffer();
			sbErrorSource = new StringBuffer();
			sbErrorTarget = new StringBuffer();

			procSource = Runtime.getRuntime().exec(sCommand);
			procTarget = Runtime.getRuntime().exec(sCommandTarget);
			
			//Deadlock on stderr from pdftohtml
			StreamGobbler errorStreamGobblerSource = new StreamGobbler("ErrorStreamSource", procSource.getErrorStream());
			StreamGobbler inputStreamGobblerSource = new StreamGobbler("InputStreamSource"
					, procSource.getInputStream()
					, procSource.getOutputStream()
					, new StringBuffer(inputTextSource.toString()));
			StreamGobbler errorStreamGobblerTarget = new StreamGobbler("ErrorStreamSource", procTarget.getErrorStream());
			StreamGobbler inputStreamGobblerTarget = new StreamGobbler("InputStreamSource"
					, procTarget.getInputStream()
					, procTarget.getOutputStream()
					, new StringBuffer(inpuTextTarget.toString()));

			inputStreamGobblerSource.SetOutputStreamFromStringBuilder();
			Thread tInput = new Thread(inputStreamGobblerSource);
			Thread tError = new Thread(errorStreamGobblerSource);
			Thread tInputTarget = new Thread(inputStreamGobblerTarget);
			Thread tErrorTarget = new Thread(errorStreamGobblerTarget);
			
			inputStreamGobblerTarget.SetOutputStreamFromStringBuilder();
			tError.start();
			tInput.start();
			tErrorTarget.start();
			tInputTarget.start();
			
			if (!procSource.waitFor(60, TimeUnit.SECONDS)) {
				// timeout - kill the process.
				procSource.destroyForcibly();
			}
			System.out.println("Test1");
//			tError.join();
			tInput.join();
			tInputTarget.join();
			System.out.println("Test2");
			
			sbErrorSource = errorStreamGobblerSource.getSbText();
			sbErrorTarget = errorStreamGobblerTarget.getSbText();
			sb = inputStreamGobblerSource.getSbText();
			sbTarget = inputStreamGobblerTarget.getSbText();
			
//			System.out.println("ERROR: : " + sbError);
//			System.out.println("Output: : " + sb);
			
			errorStreamGobblerSource.CloseBuffer();
			inputStreamGobblerSource.CloseBuffer();
			errorStreamGobblerTarget.CloseBuffer();
			inputStreamGobblerTarget.CloseBuffer();
			
			if (sbErrorTarget.length() > 0 || sbErrorSource.length() > 0) {
				sb = new StringBuffer(inputTextSource.toString());
				throw new Exception(sbErrorTarget.toString() + "\n" + sbErrorSource.toString());
			}else {
				// success
				result.add(new StringBuilder(sb.toString()));
				result.add(new StringBuilder(sbTarget.toString()));
				return result;
			}
			
		}catch (Exception e) {
			// TODO: handle exception
			result.add(inputTextSource);
			result.add(inpuTextTarget);
			printLog.writeError("RunScriptWithInputStream", e);
		}finally {
			procSource.destroy();
			procTarget.destroy();
		}
		return result;
	}
}
