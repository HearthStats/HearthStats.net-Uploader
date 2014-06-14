package net.hearthstats;

import net.hearthstats.config.OS;
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

public final class Main {
	private Main() {} // never instanciated

    private static Logger debugLog = LoggerFactory.getLogger(Main.class);

    private static String ocrLanguage = "eng";

    private static Monitor _monitor;

    public static LogPane getLogPane() {
        if (_monitor == null) {
            return null;
        } else {
            return _monitor.getLogPane();
        }
    }

	public static void showErrorDialog(String message, Throwable e) {
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

            logSystemInformation();

            cleanupDebugFiles();

			setupTesseract();

			try {
                switch (Config.os) {
                    case WINDOWS:
                        loadJarDll("liblept168");
                        loadJarDll("libtesseract302");
                        break;
                    case OSX:
                        loadOsxDylib("lept");
                        loadOsxDylib("tesseract");
                        break;
                    default:
                        throw new UnsupportedOperationException("HearthStats.net Uploader only supports Windows and Mac OS X");
                }
			} catch (Throwable e) {
                Log.error("Error loading libraries", e);
                showLibraryErrorMessage(e);
                System.exit(0);
			}

			loadingNotification.close();

            _monitor = new Monitor();
			_monitor.start();
			
		} catch (Throwable e) {
			Main.showErrorDialog("Error in Main", e);
			System.exit(1);
		}
		
	}


    private static void showLibraryErrorMessage(Throwable e) {
        String title;
        Object[] message;
        if (e instanceof UnsatisfiedLinkError) {
            // A library that Tesseract or Leptonica expects to find on this system isn't there
            title = "Expected libraries are not installed";
            if (Config.os == OS.WINDOWS && "amd64".equals(Config.getSystemProperty("os.arch"))) {
                // This is the most common scenario - the user is using 64-bit Windows and there is no 64-bit library installed by default
                message = new Object[] {
                        new JLabel("The HearthStats Uploader requires the Visual C++ Redistributable to be installed."),
                        new JLabel("Please download the 64-bit installer (vcredist_x64.exe) from"),
                        HyperLinkHandler.getUrlLabel("http://www.microsoft.com/en-US/download/details.aspx?id=30679"),
                        new JLabel("and install it before using the HearthStats Uploader.")
                };
            } else if (Config.os == OS.WINDOWS) {
                // There is no known problem with other variants of Windows, but just in case this does occur we show a similar message
                message = new Object[] {
                        new JLabel("The HearthStats Uploader requires the Visual C++ Redistributable to be installed."),
                        new JLabel("Please download the installer from"),
                        HyperLinkHandler.getUrlLabel("http://www.microsoft.com/en-US/download/details.aspx?id=30679"),
                        new JLabel("and install it before using the HearthStats Uploader.")
                };
            } else {
                message = new Object[] {
                        new JLabel("The HearthStats Uploader was unable to start because expected system libraries were not found."),
                        new JLabel("Please check your log.txt file for details."),
                        new JLabel(" "),
                        new JLabel("Exiting...")
                };
            }
        } else {
            title = e.getMessage();
            message = new Object[] {
                    new JLabel("The HearthStats Uploader was unable to start because the OCR libraries could not be read."),
                    new JLabel("Is the app already running?"),
                    new JLabel(" "),
                    new JLabel("Exiting...")
            };
        }

        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }


    private static void logSystemInformation() {
        if (debugLog.isInfoEnabled()) {
            debugLog.info("**********************************************************************");
            debugLog.info("  Starting HearthStats.net Uploader {} on {}", Config.getVersion(), Config.os);
            debugLog.info("  os.name={}", Config.getSystemProperty("os.name"));
            debugLog.info("  os.version={}", Config.getSystemProperty("os.version"));
            debugLog.info("  os.arch={}", Config.getSystemProperty("os.arch"));
            debugLog.info("  java.runtime.version={}", Config.getSystemProperty("java.runtime.version"));
            debugLog.info("  java.class.path={}", Config.getSystemProperty("java.class.path"));
            debugLog.info("  java.library.path={}", Config.getSystemProperty("java.library.path"));
            debugLog.info("  user.language={}", Config.getSystemProperty("user.language"));
            debugLog.info("**********************************************************************");
        }
    }


    private static void cleanupDebugFiles() {
        try {
            File folder = new File(Config.getExtractionFolder());
            if (folder.exists()) {
                File[] files = folder.listFiles();
                for (File file : files) {
                    if (file.isFile() && file.getName().startsWith("class-") && file.getName().endsWith(".png")) {
                        // This is a hero/class image used for debugging, so it should be deleted
                        file.delete();
                    }
                }
            }
        } catch (Exception e) {
            debugLog.warn("Ignoring exception when cleaning up debug files", e);
        }
    }


	public static void setupTesseract() {
        debugLog.debug("Extracting Tesseract data");
		String outPath;
        if (Config.os == OS.OSX) {
            File javaLibraryPath = new File(Config.getJavaLibraryPath());
            outPath = javaLibraryPath.getParentFile().getAbsolutePath() + "/Resources";
        } else {
            outPath = Config.getExtractionFolder() + "/";
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
	
	private static void loadJarDll(String name) throws FileNotFoundException, UnsatisfiedLinkError {
        debugLog.debug("Loading DLL {}", name);
		String resourcePath = "/lib/" + name + "_" + System.getProperty("sun.arch.data.model") + ".dll";
	    InputStream in = Main.class.getResourceAsStream(resourcePath);
	    if (in != null) {
		    byte[] buffer = new byte[1024];
		    int read = -1;
		    
		    File outDir = new File(Config.getExtractionFolder());
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
            } catch (UnsatisfiedLinkError e) {
                debugLog.error("UnsatisfiedLinkError loading DLL " + name, e);
                throw e;
		    } catch (Exception e) {
                debugLog.error("Error loading DLL " + name, e);
		    }
	    } else {
            Main.showErrorDialog("Error loading " + name, new Exception("Unable to load library from " + resourcePath));
	    }
	}


    private static void loadOsxDylib(String name) throws UnsatisfiedLinkError {
        debugLog.debug("Loading dylib {}", name);

       try {
           System.loadLibrary(name);
       } catch (UnsatisfiedLinkError e) {
           debugLog.error("UnsatisfiedLinkError loading dylib " + name, e);
           throw e;
       } catch (Exception e) {
           debugLog.error("Error loading dylib " + name, e);
       }

    }
}
