package net.hearthstats;

import javax.swing.*;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings("serial")
public class Main extends JFrame {

	public static String getExtractionFolder() {
        if (Config.os == Config.OS.OSX) {
            File libFolder = new File("/private/tmp/HearthStats.net-Uploader");
            libFolder.mkdir();
            return libFolder.getAbsolutePath();

        } else {
            String path = "tmp";
            (new File(path)).mkdirs();
            return path;
        }
	}
	
	public static String getLogText() {
		String logText = "";
		List<String> lines = null;
		try {
			lines = Files.readAllLines(Paths.get("log.txt"), Charset.defaultCharset());
		} catch (IOException e) {
			Main.logException(e);			
		}
		for (String line : lines) {
			logText += line + "\n";
        }
		return logText;
	}
	public static void log(String str) {
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter("log.txt", true)));
		} catch (IOException e) {
			Main.logException(e);
		}
		out.println(str);
		out.close();
	}
	public static void logException(Exception e) {
		e.printStackTrace();
		Main.log("Exception in Main: " + e.getMessage());
		Main.log(e.getStackTrace().toString());
	}
	
	protected static ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(5);
	
	protected static JFrame f = new JFrame();
	
	public static void main(String[] args) {
		
		try {

			Notification loadingNotification = new Notification("HearthStats.net Uploader", "Loading ...");
			loadingNotification.show();

			Updater.cleanUp();
			Config.rebuild();

			_extractTessData();

			try {
                switch (Config.os) {
                    case WINDOWS:
                        _loadJarDll("liblept168");
                        _loadJarDll("libtesseract302");
                        break;
                    case OSX:
                        _loadOsxDylib("liblept169");
                        _loadOsxDylib("libtesseract302");
                        break;
                    default:
                        throw new UnsupportedOperationException("HearthStats.net Uploader only supports Windows and Mac OS X");
                }
			} catch(Exception e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Unable to read required libraries.\nIs the app already running?\n\nExiting ...");
				System.exit(0);
			}
			//System.out.println(OCR.process("opponentname.jpg"));
			
			loadingNotification.close();
			
			Monitor monitor = new Monitor();
			monitor.start();
			
		} catch(Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Exception (Error): " + e.toString());
			System.exit(1);
		}
		
	}
	
	private static void _extractTessData() {
		String outPath = Main.getExtractionFolder() + "/";
		(new File(outPath + "tessdata/configs")).mkdirs();
		copyFileFromJarTo("/tessdata/eng.traineddata", outPath + "tessdata/eng.traineddata");
		copyFileFromJarTo("/tessdata/configs/api_config", outPath + "tessdata/configs/api_config");
		copyFileFromJarTo("/tessdata/configs/digits", outPath + "/tessdata/configs/digits");
		copyFileFromJarTo("/tessdata/configs/hocr", outPath + "/tessdata/configs/hocr");
		OCR.setTessdataPath(outPath + "tessdata");
	}
	
	public static void copyFileFromJarTo(String jarPath, String outPath) {
		InputStream stream = Main.class.getResourceAsStream(jarPath);
	    if (stream == null) {
	    	JOptionPane.showMessageDialog(null, "Exception: Unable to find " + jarPath + " in .jar file");
	    	System.exit(1);
	    } else {
		    OutputStream resStreamOut = null;
		    int readBytes;
		    byte[] buffer = new byte[4096];
		    try {
		        resStreamOut = new FileOutputStream(new File(outPath));
		        while ((readBytes = stream.read(buffer)) > 0) {
		            resStreamOut.write(buffer, 0, readBytes);
		        }
		    } catch (IOException e) {
		        // TODO Auto-generated catch block
		        e.printStackTrace();
		        JOptionPane.showMessageDialog(null, "Exception in Main: " + e.toString());
		    } finally {
		        try {
					stream.close();
					resStreamOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					JOptionPane.showMessageDialog(null, "Exception in Main: " + e.toString());
				}
		    }
	    }
	}
	
	private static void _loadJarDll(String name) {
		String resourcePath = "/lib/" + name + "_" + System.getProperty("sun.arch.data.model") + ".dll";
	    InputStream in = Main.class.getResourceAsStream(resourcePath);
	    if(in != null) {
		    byte[] buffer = new byte[1024];
		    int read = -1;
		    
		    File outDir = new File(Main.getExtractionFolder());
		    outDir.mkdirs();
		    String outPath = outDir.getPath() + "/";
		    
		    String outFileName = name.replace("_32", "").replace("_64",  "") + ".dll";
		    
		    FileOutputStream fos = null;
			try {
				fos = new FileOutputStream(outPath + outFileName);
			} catch (Exception e) {
				e.printStackTrace();
			}
	
		    try {
				while((read = in.read(buffer)) != -1) {
				    fos.write(buffer, 0, read);
				}
				fos.close();
				in.close();
				
			} catch (IOException e) {
				e.printStackTrace();
				JOptionPane.showMessageDialog(null, "Exception in Main: " + e.toString());
			}
		    try {
		    	System.loadLibrary(outPath + name);
		    } catch(Exception e) {
		    	JOptionPane.showMessageDialog(null, "Exception in Main: " + e.toString());
		    }
	    }
	}


   private static void _loadOsxDylib(String name) {

       String resourcePath = "/lib/" + name + ".dylib";

       InputStream in = Main.class.getResourceAsStream(resourcePath);
       if(in != null) {
           byte[] buffer = new byte[1024];
           int read = -1;

           File outDir = new File(Main.getExtractionFolder());
           outDir.mkdirs();

           String absolutePath = outDir.getAbsolutePath() + "/" + name + ".dylib";

           FileOutputStream fos = null;
           try {
               fos = new FileOutputStream(absolutePath);
           } catch (Exception e) {
               e.printStackTrace();
           }

           try {
               while((read = in.read(buffer)) != -1) {
                   fos.write(buffer, 0, read);
               }
               fos.close();
               in.close();

           } catch (IOException e) {
               e.printStackTrace();
               JOptionPane.showMessageDialog(null, "Exception in Main: " + e.toString());
           }
           try {
               System.load(absolutePath);
           } catch(Exception e) {
               JOptionPane.showMessageDialog(null, "Exception in Main: " + e.toString());
           }
       }

   }
}
