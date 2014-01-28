package net.hearthstats;

import java.awt.image.BufferedImage;
import jna.extra.GDI32Extra;
import jna.extra.User32Extra;
import jna.extra.WinGDIExtra;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinGDI;
import com.sun.jna.platform.win32.WinDef.HBITMAP;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;
import com.sun.jna.ptr.PointerByReference;

public class ProgramHelper {

	protected String _programName;
	private String _processName;
	private HWND _windowHandle = null;
	
	public ProgramHelper(String programName, String processName) {
		_programName = programName;
		_processName = processName;
	}

	public BufferedImage getScreenCapture() {
		if (foundProgram()) {

			BufferedImage image;
			
			// only supports windows at the moment
			image = _getScreenCaptureWindows(_windowHandle);

			return image;
		}
		return null;
	}
	
	private HWND _getWindowHandle() {
		_windowHandle = null;
		User32.INSTANCE.EnumWindows(new WNDENUMPROC() {
        int count = 0;
        @Override
        public boolean callback(HWND hWnd, Pointer arg1) {
            byte[] windowText = new byte[512];
            
            int titleLength = User32.INSTANCE.GetWindowTextLength(hWnd) + 1;
            char[] title = new char[titleLength];
            User32.INSTANCE.GetWindowText(hWnd, title, titleLength);
            String wText = Native.toString(title);
            
            if (wText.isEmpty()) {
               return true;
            }

            if(wText.matches(".*" + _programName + ".*")) {
            	
	      	 	PointerByReference pointer = new PointerByReference();
	      	    User32DLL.GetWindowThreadProcessId(hWnd, pointer);
	      	    Pointer process = Kernel32.OpenProcess(Kernel32.PROCESS_QUERY_INFORMATION | Kernel32.PROCESS_VM_READ, false, pointer.getValue());
	      	    Psapi.GetModuleBaseNameW(process, null, buffer, MAX_TITLE_LENGTH);
	      	    if(Native.toString(buffer).matches(_processName)) {
	      	    	_windowHandle = hWnd;
	      	    }
            }
            return true;
         	}
		}, null);
		return _windowHandle;
	}
	
	/**
	 * Is the program found?
	 * 
	 * @return Whether or not the program is found
	 */
	public boolean foundProgram() {
		
		return _getWindowHandle() != null;
	}
	
	protected static BufferedImage _getScreenCaptureWindows(HWND hWnd) {

		HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
		HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

		RECT bounds = new RECT();
		User32Extra.INSTANCE.GetClientRect(hWnd, bounds);

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
}
