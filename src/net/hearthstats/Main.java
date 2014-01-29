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
import java.nio.file.Paths;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

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

	protected static ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(5);
	
	protected static JFrame f = new JFrame();
	
	public static void main(String[] args) throws IOException {
		
		try {
		
			_loadJarDll("liblept168");
			_loadJarDll("libtesseract302");
			
			Monitor monitor = new Monitor();
			monitor.start();
			
		} catch(Exception e) {
			JOptionPane.showMessageDialog(null, "Exception: " + e.getStackTrace() + e.toString());
		}
		
	}
	
	private static void _loadJarDll(String name) throws IOException {
		String resourcePath = "/lib/" + name + "_" + System.getProperty("sun.arch.data.model") + ".dll";
	    InputStream in = Main.class.getResourceAsStream(resourcePath);
	    if(in != null) {
		    byte[] buffer = new byte[1024];
		    int read = -1;
		    
		    File outDir = new File("temp");
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
