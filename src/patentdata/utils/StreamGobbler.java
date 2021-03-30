package patentdata.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

public class StreamGobbler extends Thread{
	private StringBuffer sbText;
	private	BufferedReader bufReader ;
	private	OutputStream outputStream;
	private ByteArrayInputStream bInput;
	private StringBuffer sbInput;

	   public StreamGobbler(String name, InputStream inStream) {
	      super(name);
	      this.bufReader = new BufferedReader(new InputStreamReader(
	            inStream));
	   }
	   
	   StreamGobbler(String name, InputStream inputStream, OutputStream outputStream, ByteArrayInputStream bIn)
		  {
			  
			super(name);
			this.outputStream = outputStream;
		    this.bInput = bIn;
		    this.bufReader = new BufferedReader(new InputStreamReader(inputStream));
		  }
	   
	   public StreamGobbler(String name, InputStream inputStream, OutputStream outputStream, StringBuffer sbInput)
		  {
			  
			super(name);
			this.outputStream = outputStream;
		    this.sbInput = sbInput;
		    this.bufReader = new BufferedReader(new InputStreamReader(inputStream));
		  }
	   

	   @Override
	   public void run() {
		   GetInputStream();
	   }

	   private void GetInputStream() {
		   try {

			    String sLine = "";
			    while ((sLine = bufReader.readLine()) != null) {
//			    	System.out.println("sLine:" + sLine);
		            getSbText().append(sLine);
		            getSbText().append("\n");
		         }
		         
		      }  catch (Exception e) {
		    	  // Ignore error
		    	  System.out.println(e.getMessage());
		    	  e.printStackTrace();
			  }
	   }

	   public StringBuffer getSbText() {
			if (null == this.sbText) {
				this.sbText = new StringBuffer();
			}
			return this.sbText;
		}
	   
	   public void SetOutputStream() throws Exception {
			  if (null != bInput && null != outputStream) {
					try {
//						System.out.println(bInput.available());
			            byte[] bytes = new byte[2048];
			            int index;
			            while ((index = bInput.read(bytes)) != -1) {
			                outputStream.write(bytes, 0, index);
			            }
			           
					} catch (IOException e) {
						throw e;
					}finally {
						try {
							this.outputStream.flush();
							this.outputStream.close();
						} catch (IOException e) {
							
						}
			            
					}
			  	}
		  }
	   
	   	  public void SetOutputStreamFromStringBuilder() throws Exception {
//			  System.out.println("Start Set STDIN");

			  if (null != sbInput && null != outputStream) {
					try {
//						System.out.println("sbInput: " + sbInput);
			             outputStream.write(this.sbInput.toString().getBytes(Charset.forName("UTF-8")));
			           
					} catch (IOException e) {
						throw e;
					}finally {
						try {
							this.outputStream.flush();
							this.outputStream.close();
						} catch (IOException e) {
							
						}
			            
					}
			  	}
//			  System.out.println("END Set STDIN");

		  }
	   
	   public void CloseBuffer() {
		   try {
			   if (null != this.bufReader)
				   bufReader.close();
			} catch (IOException e) {
				
			}
		   
		   try {
			   if (null != this.bInput)
				   this.bInput.close();
			} catch (IOException e) {
				
			}
		   
		   try {
			   if (null != this.outputStream)
				   this.outputStream.close();
			} catch (IOException e) {
				
			}
	   }
}