package net.hearthstats.win;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;

import net.hearthstats.ProgramHelper;
import net.hearthstats.win.jna.extra.GDI32Extra;
import net.hearthstats.win.jna.extra.User32Extra;
import net.hearthstats.win.jna.extra.WinGDIExtra;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.PointerByReference;

/**
 * Implementation of {@link ProgramHelper} for Windows.
 */
public class ProgramHelperWindows extends ProgramHelper {

    private final static Logger debugLog = LoggerFactory.getLogger(ProgramHelperWindows.class);

    /**
     * The number of iterations to wait without a window until we assume that Hearthstone has been minimised
     */
    private static final int ITERATIONS_FOR_MINIMISE = 8;

    private final String _processName = "Hearthstone.exe";

	private HWND _windowHandle = null;
	private String _windowHandleId = null;
	private boolean isFullscreen = false;
	private boolean isMinimised = false;
    private int minimisedCount = 0;
	
	public ProgramHelperWindows() {
        debugLog.debug("Initialising ProgramHelperWindows with {}", _processName);
	}

	@Override
    public BufferedImage getScreenCapture() {
		if (foundProgram()) {

			BufferedImage image;
			
			// only supports windows at the moment
			image = _getScreenCaptureWindows(_windowHandle);

			// TODO: implement OSX version
			
			return image;
		}
		return null;
	}
	
	private HWND _getWindowHandle() {
		_windowHandle = null;
		User32.INSTANCE.EnumWindows(new WNDENUMPROC() {
	        @Override
	        public boolean callback(HWND hWnd, Pointer arg1) {
	            
	            int titleLength = User32.INSTANCE.GetWindowTextLength(hWnd) + 1;
	            char[] title = new char[titleLength];
	            User32.INSTANCE.GetWindowText(hWnd, title, titleLength);
	            String wText = Native.toString(title);
	            
	            if (wText.isEmpty()) {
	               return true;
	            }
	
	            	
	      	 	PointerByReference pointer = new PointerByReference();
	      	    User32DLL.GetWindowThreadProcessId(hWnd, pointer);
	      	    Pointer process = Kernel32.OpenProcess(Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ, false, pointer.getValue());
	      	    Psapi.GetModuleBaseNameW(process, null, buffer, MAX_TITLE_LENGTH);
	      	    
	      	    // see https://github.com/JeromeDane/HearthStats.net-Uploader/issues/66#issuecomment-33829132
	      	    char[] className = new char[512];
	      	    User32.INSTANCE.GetClassName(hWnd, className, 512);
	      	    String classNameStr = Native.toString(className);

	      	    if(Native.toString(buffer).equals(_processName) && classNameStr.equals("UnityWndClass")) {
	      	    	_windowHandle = hWnd;
	      	    	if(_windowHandleId == null) {
	      	    		_windowHandleId = _windowHandle.toString();
	      	    		_notifyObserversOfChangeTo("Hearthstone window found with process name " + _processName);
	      	    	}
	      	    }
	            return true;
         	}
		}, null);
		
		// notify of window lost
		if(_windowHandle == null && _windowHandleId != null) {
			_notifyObserversOfChangeTo("Hearthstone window with process name " + _processName + " closed");
			_windowHandleId = null;
		}
		return _windowHandle;
	}
	
	@Override
	public boolean foundProgram() {
		
		// windows version
		if(_getWindowHandle() != null) {
			return true;
		}
		_windowHandleId = null;
		return false;
		
		// TODO: implement OSX version
	}
	
	private boolean _isFullScreen(Rectangle rect) {
		// check to make sure Hearthstone's not in full screen
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		int width = gd.getDisplayMode().getWidth();
		int height = gd.getDisplayMode().getHeight();
		return (rect.width >= width && rect.height >= height);
	}
	
	private BufferedImage _getScreenCaptureWindows(HWND hWnd) {

		HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
		HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

		RECT bounds = new RECT();
		User32Extra.INSTANCE.GetClientRect(hWnd, bounds);
		
		// check to make sure the window's not minimized
		if(bounds.toRectangle().width >= 1024) {
			if(isMinimised) {
				_notifyObserversOfChangeTo("Hearthstone window restored");
				isMinimised = false;
			}
			
			if(_isFullScreen(bounds.toRectangle())) {
				if(!isFullscreen) {
					_notifyObserversOfChangeTo("Hearthstone running in fullscreen");
					isFullscreen = true;
				}
				return null;
			} else {
				int width = bounds.right - bounds.left;
				int height = bounds.bottom - bounds.top;
		
				HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow, width, height);
		
				HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
				GDI32Extra.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0, 0, WinGDIExtra.SRCCOPY);
		
				GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
				GDI32.INSTANCE.DeleteDC(hdcMemDC);
		
				BITMAPINFO bmi = new BITMAPINFO();
				bmi.bmiHeader.biWidth = width;
				bmi.bmiHeader.biHeight = -height;
				bmi.bmiHeader.biPlanes = 1;
				bmi.bmiHeader.biBitCount = 32;
				bmi.bmiHeader.biCompression = WinGDI.BI_RGB;
		
				Memory buffer = new Memory(width * height * 4);
				GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi, WinGDI.DIB_RGB_COLORS);
		
				BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
				image.setRGB(0, 0, width, height, buffer.getIntArray(0, width * height), 0, width);
		
				GDI32.INSTANCE.DeleteObject(hBitmap);
				User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);
				return image;
			}
		}
		if (!isMinimised) {
            // Hearthstone has brief periods where its window is not displayed, such as during startup and when changing
            // scree size. We don't want to show a warning for these, so we wait a couple of iterations before assuming
            // that the window has been minimised.
            if (minimisedCount < ITERATIONS_FOR_MINIMISE) {
                minimisedCount++;
            } else {
                _notifyObserversOfChangeTo("Warning! Hearthstone minimized. No detection possible.");
                isMinimised = true;
                minimisedCount = 0;
            }
		}
		return null;
	}

    protected char[] buffer = new char[MAX_TITLE_LENGTH * 2];
	private static final int MAX_TITLE_LENGTH = 1024;
	
	static class Psapi {
	    static { Native.register("psapi"); }
	    public static native int GetModuleBaseNameW(Pointer hProcess, Pointer hmodule, char[] lpBaseName, int size);
	}

	static class Kernel32 {
	    static { Native.register("kernel32"); }
	    public static int PROCESS_QUERY_INFORMATION = 0x0400;
	    public static int PROCESS_VM_READ = 0x0010;
	    public static native int GetLastError();
	    public static native Pointer OpenProcess(int dwDesiredAccess, boolean bInheritHandle, Pointer pointer);
	}

	static class User32DLL {
	    static { Native.register("user32"); }
	    public static native int GetWindowThreadProcessId(HWND hWnd, PointerByReference pref);
	    public static native HWND GetForegroundWindow();
	    public static native int GetWindowTextW(HWND hWnd, char[] lpString, int nMaxCount);
	}

	@Override
	public String hearthstoneConfigFolder() {
		String appdata = System.getenv("LOCALAPPDATA");
		if (StringUtils.isBlank(appdata)) {
			throw new RuntimeException("Cannot find LOCALAPPDATA directory");
		}
		File folder = new File(appdata + "/Blizzard/Hearthstone");
		return folder.getAbsolutePath();
	}

	@Override
	public String hearthstoneLogFile() {
		String programFiles = System.getenv("PROGRAMFILES(X86)");
		if (StringUtils.isBlank(programFiles)) {
			programFiles = System.getenv("PROGRAMFILES");
			if (StringUtils.isBlank(programFiles)) {
				throw new RuntimeException(
						"Cannot find Program Files directory");
			}
		}
		File logFile = new File(programFiles
				+ "\\Hearthstone\\Hearthstone_Data\\output_log.txt");
		return logFile.getAbsolutePath();
	}
}
