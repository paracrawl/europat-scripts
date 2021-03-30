package patentdata.utils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.ThreadContext;



public class LogFourJ {

	private String log4JPropertyFile = "";
	public Level lLevel = Level.ERROR;
	public String debugPath = "/var/www/patent-data/logs/", debugMode = "true";
	public String sServerIP = "";
	String loggerDebug = "log-debug";
	public boolean bVerbose = false;
	
	public LogFourJ(String sXMLPath, String sDebugPath, boolean bVerbose) throws Exception {
		try {

//			String sDebugPath = "/home/ramoslee/work/eclipse-workspace/patent-data/logs/";
			this.bVerbose = bVerbose;
			
			log4JPropertyFile = sXMLPath;
			
			if (log4JPropertyFile == null || log4JPropertyFile.trim().length() == 0)
				log4JPropertyFile = "./log4j.xml";
			
			debugPath = sDebugPath;
			if (debugPath == null || debugPath.trim().length() == 0)
				debugPath = "./logs/";
			
			File dir = new File(debugPath);
			if (!dir.exists()) {
				// create directory
				dir.mkdirs();
			}
			initailizelog4j(log4JPropertyFile);
			
		} catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
	}
	
	private void initailizelog4j(String log4JPropertyFile) throws Exception {
		System.setProperty("log4j.configurationFile", log4JPropertyFile);

	}
	
	public void writeDebugLog(String message) {
		try {
			writeLog(message, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeDebugLog(String message, Exception ex) {
		try {
			if (ex != null)
				message = message + " ExceptionError:" + getStackTrace(ex);
			writeLog(message, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void writeError(String MethodName, String message) {
		try {
			
			message = "MethodName=" + MethodName + "\t" + "Message=" + message;
			
			writeLog(message, true);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeError(String MethodName, Exception ex) {
		try {
			
			String message = "MethodName=" + MethodName + "\t" + "Message=" + getStackTrace(ex);
			
			writeLog(message, true);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void writeError(String message) {
		try {
			
			writeLog(message, true);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void writeLog(String message, boolean isError) {
		try {
			if (isError)
				error(message);
			else if (lLevel != Level.OFF) {
				if (debugMode.equalsIgnoreCase("true") || debugMode.equals("1"))
					debug(message);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// get level from database
		public void debug(String message, String sDebugFolder, String sFileName) throws Exception {
			try {
				if (message != null) {

					Calendar oCal = Calendar.getInstance();
					SimpleDateFormat oDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
					
					String sContent = oDateTimeFormat.format(oCal.getTime());
					sContent += ", " + message + "\r\n";
					
					LogManager.getLogger(loggerDebug).debug(sContent);

				}
			} catch (Exception ex) {
				// TODO: handle exception
				System.out.println("Log debug Error=" + ex.toString());
			}
		}
		
	// get level from database
	public void debug(String message) throws Exception {
		try {
			if (message != null) {
				String sFileName = "debug-" + getFileName("yyyyMMdd-HH") + ".txt";
				reconfiglog4j(debugPath, sFileName);
				Calendar oCal = Calendar.getInstance();
				SimpleDateFormat oDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
				
				String sContent = oDateTimeFormat.format(oCal.getTime());
				sContent += ", " + message + "\r\n";
				
				LogManager.getLogger(loggerDebug).debug(message + "\r\n");
				if (bVerbose) {
					System.out.print(sContent);
				}
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log debug Error=" + ex.toString());

		}
	}

	public void info(String message) throws Exception {
		try {
			if (message != null) {
				String sFileName = "info-" + getFileName("yyyyMMdd-HH") + ".txt";
				reconfiglog4j(debugPath, sFileName);

				LogManager.getLogger(loggerDebug).info(message + "\n");
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log info Error=" + ex.toString());

		}
	}

	public void info(String message, String sFileName) throws Exception {
		try {
			if (message != null) {
				LogManager.getLogger(loggerDebug).info(message + "\n");
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log info Error=" + ex.toString());

		}
	}

	public void warn(String message) throws Exception {
		try {
			if (message != null) {
				String sFileName = "warn-" + getFileName("yyyyMMdd-HH") + ".txt";
				reconfiglog4j(debugPath, sFileName);

				LogManager.getLogger(loggerDebug).warn(message + "\n");
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log warn Error=" + ex.toString());

		}
	}
	public void error(String message) throws Exception {
		try {
			if (message != null) {
				String sFileName = "error-" + getFileName("yyyyMMdd-HH") + ".txt";
				reconfiglog4j(debugPath, sFileName);

				LogManager.getLogger(loggerDebug).error(message + "\n");
				if (bVerbose) {
					System.err.println(message);
				}
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log error Error=" + ex.toString());

		}
	}
	
	public void error(String message, String sDebugFolder, String sFileName) throws Exception {
		try {
			if (message != null) {
//				String sFileName = "error-" + getFileName("yyyyMMdd-HH") + ".txt";

				LogManager.getLogger(loggerDebug).error(message + "\n");
				if (bVerbose) {
					System.err.println(message);
				}
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log error Error=" + ex.toString());

		}
	}

	public void errorRetry(String method, String message)
			throws Exception {
		try {
			if (method != null && message != null) {
				if (!method.equals("null")) {
					String errormessage = "Method: " + method + " Error:" + message;

					String sFileName = "error-retry-" + getFileName("yyyyMMdd-HH") + ".txt";
					reconfiglog4j(debugPath, sFileName);

					LogManager.getLogger(loggerDebug)
							.error(errormessage + "\n");

				}
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log error Error=" + ex.toString());

		}
	}

	public void fatal(String message) throws Exception {
		try {
			if (message != null) {
				String sFileName = "fatal-" + getFileName("yyyyMMdd-HH")
						+ ".txt";
				reconfiglog4j(debugPath, sFileName);

				LogManager.getLogger(loggerDebug).fatal(message + "\n");
			}
		} catch (Exception ex) {
			// TODO: handle exception
			System.out.println("Log fatal Error=" + ex.toString());

		}
	}

	public String getFileName(String pattern) {
		// "yyyyMMdd"
		// "yyyy-MM-dd HH:mm:ss"
		try {
			Calendar oCal = Calendar.getInstance();
			SimpleDateFormat oDateTimeFormat = new SimpleDateFormat(pattern);
			String filename = oDateTimeFormat.format(oCal.getTime());
			return filename;
		} catch (Exception e) {
			// TODO: handle exception

		}
		return "error.txt";
	}


	public void WriteDebugLogs(String sDebugFolder, String sFileName, String sText, boolean isError)
    {
		try {
			if (isError)
				error(sText, sDebugFolder, sFileName);
			else if (lLevel != Level.OFF) {
				if (debugMode.equalsIgnoreCase("true") || debugMode.equals("1"))
					debug(sText, sDebugFolder, sFileName);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	public String getStackTrace(Exception exception) {
		String text = "";
		Writer writer = null;
		try {
			writer = new StringWriter();
			PrintWriter printWriter = new PrintWriter(writer);
			exception.printStackTrace(printWriter);
			text = writer.toString();
		} catch (Exception e) {

		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {

				}
			}
		}
		return text;
	}
	
	private void reconfiglog4j(String debugPath, String filename) {

		ThreadContext.put("logPath", debugPath);
		ThreadContext.put("logName", filename);
	}
}
