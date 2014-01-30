package net.hearthstats;

import jna.*;
import jna.extra.GDI32Extra;
import jna.extra.User32Extra;
import jna.extra.WinGDIExtra;

import java.io.File;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JApplet;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import net.sourceforge.tess4j.Tesseract;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;

@SuppressWarnings("serial")
public class Main extends JFrame {

	public static String getExtractionFolder() {
		return "tmp";
	}
	
	protected static ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(5);
	
	protected static JFrame f = new JFrame();
	
	public static void main(String[] args) throws IOException {
		
		try {
			
			Notification notification = new Notification("HearthStats.net Uploader", "Loading ...");
			notification.show();
			
			_extractTessData();
			
			_loadJarDll("liblept168");
			_loadJarDll("libtesseract302");
			
			//System.out.println(OCR.process("opponentname.jpg"));
			
			Monitor monitor = new Monitor();
			notification.close();
			monitor.start();
			
		} catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Exception: " + e.toString());
			System.exit(1);
		}
		
	}
	
	private static void _extractTessData() {
		String outPath = Main.getExtractionFolder() + "/";
		(new File(outPath + "tessdata/configs")).mkdirs();
		_copyFileFromJarTo("/tessdata/eng.traineddata", outPath + "tessdata/eng.traineddata");
		_copyFileFromJarTo("/tessdata/configs/api_config", outPath + "tessdata/configs/api_config");
		_copyFileFromJarTo("/tessdata/configs/digits", outPath + "/tessdata/configs/digits");
		_copyFileFromJarTo("/tessdata/configs/hocr", outPath + "/tessdata/configs/hocr");
		OCR.setTessdataPath(outPath + "tessdata");
	}
	
	private static void _copyFileFromJarTo(String jarPath, String outPath) {
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
		    } catch (IOException e1) {
		        // TODO Auto-generated catch block
		        e1.printStackTrace();
		    } finally {
		        try {
					stream.close();
					resStreamOut.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		    }
	    }
	}
	
	private static void _loadJarDll(String name) throws IOException {
		String resourcePath = "/lib/" + name + "_" + System.getProperty("sun.arch.data.model") + ".dll";
	    InputStream in = Main.class.getResourceAsStream(resourcePath);
	    if(in != null) {
		    byte[] buffer = new byte[1024];
		    int read = -1;
		    
		    File outDir = new File(Main.getExtractionFolder());
		    outDir.mkdirs();
		    String outPath = outDir.getPath() + "/";
		    
		    String outFileName = name.replace("_32", "").replace("_64",  "") + ".dll";
		    FileOutputStream fos = new FileOutputStream(outPath + outFileName);
	
		    while((read = in.read(buffer)) != -1) {
		        fos.write(buffer, 0, read);
		    }
		    fos.close();
		    in.close();
	
		    System.loadLibrary(outPath + name);
	    }
	}
}
