package jna;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef.RECT;

/**
 * static methods to allow Java to call Windows code. user32.dll code is as
 * specified in the JNA interface User32.java
 * 
 * @author Pete S
 * 
 */
public class JnaUtil {
	private static final User32 user32 = User32.INSTANCE;
	private static Pointer callBackHwnd;

	public static boolean windowExists(final String startOfWindowName) {
		return !user32.EnumWindows(new User32.WNDENUMPROC() {
			@Override
			public boolean callback(Pointer hWnd, Pointer userData) {
				byte[] windowText = new byte[512];
				user32.GetWindowTextA(hWnd, windowText, 512);
				String wText = Native.toString(windowText).trim();

				if (!wText.isEmpty() && wText.startsWith(startOfWindowName)) {
					return false;
				}
				return true;
			}
		}, null);
	}

	public static boolean windowExists(Pointer hWnd) {
		return user32.IsWindow(hWnd);
	}

	public static Pointer getWinHwnd(final String startOfWindowName) {
		callBackHwnd = null;

		user32.EnumWindows(new User32.WNDENUMPROC() {
			@Override
			public boolean callback(Pointer hWnd, Pointer userData) {
				byte[] windowText = new byte[512];
				user32.GetWindowTextA(hWnd, windowText, 512);
				String wText = Native.toString(windowText).trim();

				if (!wText.isEmpty() && wText.startsWith(startOfWindowName)) {
					callBackHwnd = hWnd;
					return false;
				}
				return true;
			}
		}, null);
		return callBackHwnd;
	}

	public static boolean setForegroundWindow(Pointer hWnd) {
		return user32.SetForegroundWindow(hWnd) != 0;
	}

	public static Pointer getForegroundWindow() {
		return user32.GetForegroundWindow();
	}

	public static String getForegroundWindowText() {
		Pointer hWnd = getForegroundWindow();
		int nMaxCount = 512;
		byte[] lpString = new byte[nMaxCount];
		int getWindowTextResult = user32.GetWindowTextA(hWnd, lpString,
				nMaxCount);
		if (getWindowTextResult == 0) {
			return "";
		}

		return Native.toString(lpString);
	}

	public static boolean isForegroundWindow(Pointer hWnd) {
		return user32.GetForegroundWindow().equals(hWnd);
	}

	public static boolean setForegroundWindow(String startOfWindowName) {
		Pointer hWnd = getWinHwnd(startOfWindowName);
		return user32.SetForegroundWindow(hWnd) != 0;
	}

	public static Rectangle getWindowRect(Pointer hWnd) throws JnaUtilException {
		if (hWnd == null) {
			throw new JnaUtilException(
					"Failed to getWindowRect since Pointer hWnd is null");
		}
		Rectangle result = null;
		RECT rect = new RECT();
		boolean rectOK = user32.GetWindowRect(hWnd, rect);
		if (rectOK) {
			int x = rect.left;
			int y = rect.top;
			int width = rect.right - rect.left;
			int height = rect.bottom - rect.top;
			result = new Rectangle(x, y, width, height);
		}

		return result;
	}

	public static Rectangle getWindowRect(String startOfWindowName)
			throws JnaUtilException {
		Pointer hWnd = getWinHwnd(startOfWindowName);
		if (hWnd != null) {
			return getWindowRect(hWnd);
		} else {
			throw new JnaUtilException("Failed to getWindowRect for \""
					+ startOfWindowName + "\"");
		}
	}

	public static Pointer getWindow(Pointer hWnd, int uCmd) {
		return user32.GetWindow(hWnd, uCmd);
	}

	public static String getWindowText(Pointer hWnd) {
		int nMaxCount = 512;
		byte[] lpString = new byte[nMaxCount];
		int result = user32.GetWindowTextA(hWnd, lpString, nMaxCount);
		if (result == 0) {
			return "";
		}
		return Native.toString(lpString);
	}

	public static Pointer getOwnerWindow(Pointer hWnd) {
		return user32.GetWindow(hWnd, User32.GW_OWNER);
	}

	public static String getOwnerWindow(String childTitle) {
		Pointer hWnd = getWinHwnd(childTitle);
		Pointer parentHWnd = getOwnerWindow(hWnd);
		if (parentHWnd == null) {
			return "";
		}
		return getWindowText(parentHWnd);

	}

	public static void main(String[] args) throws InterruptedException {
		String[] testStrs = { "Untitled-Notepad", "Untitled - Notepad",
				"Untitled  -  Notepad", "Java-Epic", "Java - Epic",
				"Fubars rule!", "The First Night", "New Tab", "Citrix X",
				"EHR PROD - SVC" };
		for (String testStr : testStrs) {
			Pointer hWnd = getWinHwnd(testStr);
			boolean isWindow = windowExists(hWnd);
			System.out.printf("%-22s %5b %16s %b%n", testStr,
					windowExists(testStr), hWnd, isWindow);
		}

		String ehrProd = "EHR PROD - SVC";
		Pointer hWnd = getWinHwnd(ehrProd);
		System.out.println("is it foreground window? "
				+ isForegroundWindow(hWnd));
		boolean foo = setForegroundWindow(ehrProd);
		System.out.println("foregroundwindow: " + foo);
		Thread.sleep(400);
		System.out.println("is it foreground window? "
				+ isForegroundWindow(hWnd));
		Thread.sleep(400);

		try {
			Rectangle rect = getWindowRect(ehrProd);
			Robot robot = new Robot();

			BufferedImage img = robot.createScreenCapture(rect);
			ImageIcon icon = new ImageIcon(img);
			JLabel label = new JLabel(icon);
			JOptionPane.showMessageDialog(null, label);

		} catch (AWTException e) {
			e.printStackTrace();
		} catch (JnaUtilException e) {
			e.printStackTrace();
		}
	}

}