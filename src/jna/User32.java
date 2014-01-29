package jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.win32.StdCallLibrary;

/**
 * JNA interface with Window's user32.dll
 * 
 * @author Pete S
 * 
 */
public interface User32 extends StdCallLibrary {
	User32 INSTANCE = (User32) Native.loadLibrary("user32", User32.class);

	interface WNDENUMPROC extends StdCallCallback {
		boolean callback(Pointer hWnd, Pointer arg);
	}

	public static final int GW_OWNER = 4; // used with GetWindow to get win
											// owner

	boolean EnumWindows(WNDENUMPROC lpEnumFunc, Pointer userData);

	int GetWindowTextA(Pointer hWnd, byte[] lpString, int nMaxCount);

	int SetForegroundWindow(Pointer hWnd);

	Pointer GetForegroundWindow();

	boolean GetWindowRect(Pointer hWnd, RECT rect);

	boolean IsWindow(Pointer hWnd);

	Pointer GetWindow(Pointer hWnd, int uCmd);
}