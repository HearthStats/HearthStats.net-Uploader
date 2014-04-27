package net.hearthstats;

import net.hearthstats.log.Log;
import net.hearthstats.log.LogPane;
import net.hearthstats.notification.DialogNotification;
import net.sourceforge.tess4j.Tesseract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@SuppressWarnings("serial")
public class Main extends JFrame {

    private static Logger debugLog = LoggerFactory.getLogger(Main.class);

    private static String ocrLanguage = "eng";

    private static Monitor _monitor;

	public static String getExtractionFolder() {
        if (Config.os == Config.OS.OSX) {
            File libFolder = new File(Config.getSystemProperty("user.home") + "/Library/Application Support/HearthStatsUploader");
            libFolder.mkdir();
            return libFolder.getAbsolutePath();

        } else {
            String path = "tmp";
            (new File(path)).mkdirs();
            return path;
        }
	}

    public static LogPane getLogPane() {
        if (_monitor == null) {
            return null;
        } else {
            return _monitor.getLogPane();
        }
    }

	public static void showErrorDialog(String message, Exception e) {
        debugLog.error(message, e);
        JFrame frame = new JFrame();
        frame.setFocusableWindowState(true);
        Main.showMessageDialog(null, message + "\n" + e.getMessage() + "\n\nSee log.txt for details");
	}

	public static void showMessageDialog(Component parentComponent, String message) {
		JOptionPane op = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
		JDialog dialog = op.createDialog(parentComponent, "HearthStats.net");
		dialog.setAlwaysOnTop(true);
		dialog.setModal(true);
		dialog.setFocusableWindowState(true);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
	}
	
	protected static ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(5);
	
	protected static JFrame f = new JFrame();
	
	public static void main(String[] args) {
		
		try {

			DialogNotification loadingNotification = new DialogNotification("HearthStats.net Uploader", "Loading ...");
			loadingNotification.show();

			Updater.cleanUp();
			Config.rebuild();

            _logSystemInformation();

			setupTesseract();

			try {
                switch (Config.os) {
                    case WINDOWS:
                        _loadJarDll("liblept168");
                        _loadJarDll("libtesseract302");
                        break;
                    case OSX:
                        _loadOsxDylib("lept");
                        _loadOsxDylib("tesseract");
                        break;
                    default:
                        throw new UnsupportedOperationException("HearthStats.net Uploader only supports Windows and Mac OS X");
                }
			} catch(Exception e) {
                debugLog.error("Error loading required libraries", e);
				JOptionPane.showMessageDialog(null, "Unable to read required libraries.\nIs the app already running?\n\nExiting ...");
				System.exit(0);
			}
			//debugLog.debug(OCR.process("opponentname.jpg"));

			loadingNotification.close();

            _monitor = new Monitor();
			_monitor.start();
			
		} catch(Exception e) {
			Main.showErrorDialog("Error in Main", e);
			System.exit(1);
		}
		
	}

    private static void _logSystemInformation() {
        if (debugLog.isInfoEnabled()) {
            debugLog.info("**********************************************************************");
            debugLog.info("  Starting HearthStats.net Uploader {} on {}", Config.getVersion(), Config.os);
            debugLog.info("  os.name={}", Config.getSystemProperty("os.name"));
            debugLog.info("  os.version={}", Config.getSystemProperty("os.version"));
            debugLog.info("  os.arch={}", Config.getSystemProperty("os.arch"));
            debugLog.info("  java.runtime.version={}", Config.getSystemProperty("java.runtime.version"));
            debugLog.info("  java.class.path={}", Config.getSystemProperty("java.class.path"));
            debugLog.info("  user.language={}", Config.getSystemProperty("user.language"));
            debugLog.info("**********************************************************************");
        }
    }

	public static void setupTesseract() {
        debugLog.debug("Extracting Tesseract data");
		String outPath;
        if (Config.os == Config.OS.OSX) {
            File javaLibraryPath = new File(Config.getJavaLibraryPath());
            outPath = javaLibraryPath.getParentFile().getAbsolutePath() + "/Resources";
        } else {
            outPath = Main.getExtractionFolder() + "/";
            (new File(outPath + "tessdata/configs")).mkdirs();
            copyFileFromJarTo("/tessdata/eng.traineddata", outPath + "tessdata/eng.traineddata");
            copyFileFromJarTo("/tessdata/configs/api_config", outPath + "tessdata/configs/api_config");
            copyFileFromJarTo("/tessdata/configs/digits", outPath + "tessdata/configs/digits");
            copyFileFromJarTo("/tessdata/configs/hocr", outPath + "tessdata/configs/hocr");
        }

        Tesseract instance = Tesseract.getInstance();
        instance.setDatapath(outPath + "tessdata");
        instance.setLanguage(ocrLanguage);
	}

	public static void copyFileFromJarTo(String jarPath, String outPath) {
		InputStream stream = Main.class.getResourceAsStream(jarPath);
	    if (stream == null) {
            Log.error("Exception: Unable to load file from JAR: " + jarPath);
	    	Main.showMessageDialog(null, "Exception: Unable to find " + jarPath + " in .jar file\n\nSee log.txt for details");
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
		        Main.showErrorDialog("Error writing file " + outPath, e);
		    } finally {
		        try {
					stream.close();
					resStreamOut.close();
				} catch (IOException e) {
					Main.showErrorDialog("Error closing stream for " + jarPath, e);
				}
		    }
	    }
	}
	
	private static void _loadJarDll(String name) throws FileNotFoundException {
        debugLog.debug("Loading DLL {}", name);
		String resourcePath = "/lib/" + name + "_" + System.getProperty("sun.arch.data.model") + ".dll";
	    InputStream in = Main.class.getResourceAsStream(resourcePath);
	    if (in != null) {
		    byte[] buffer = new byte[1024];
		    int read = -1;
		    
		    File outDir = new File(Main.getExtractionFolder());
		    outDir.mkdirs();
		    String outPath = outDir.getPath() + "/";
		    
		    String outFileName = name.replace("_32", "").replace("_64",  "") + ".dll";
		    
		    FileOutputStream fos = null;
			fos = new FileOutputStream(outPath + outFileName);
	
		    try {
				while((read = in.read(buffer)) != -1) {
				    fos.write(buffer, 0, read);
				}
				fos.close();
				in.close();
				
			} catch (IOException e) {
                debugLog.error("Error copying DLL " + name, e);
			}
		    try {
		    	System.loadLibrary(outPath + name);
		    } catch(Exception e) {
                debugLog.error("Error loading DLL " + name, e);
		    }
	    } else {
            Main.showErrorDialog("Error loading " + name, new Exception("Unable to load library from " + resourcePath));
	    }
	}


    private static void _loadOsxDylib(String name) {
        debugLog.debug("Loading dylib {}", name);

       try {
           System.loadLibrary(name);
       } catch(Exception e) {
           debugLog.error("Error loading dylib " + name, e);
       }

    }
}
