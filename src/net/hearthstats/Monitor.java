package net.hearthstats;

import jna.*;
import jna.extra.GDI32Extra;
import jna.extra.User32Extra;
import jna.extra.WinGDIExtra;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.imageio.ImageIO;
import javax.management.Notification;
import javax.swing.ImageIcon;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;

import sun.java2d.pipe.PixelFillPipe;

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

public class Monitor extends JFrame {

	public static void start() throws JnaUtilException, IOException {

		Image icon = new ImageIcon("images/icon.png").getImage();

        f.setIconImage(icon);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setLocation(0, 0);
		f.setVisible(true);

		_poll();

	}
	
	protected static String _gameMode;
	protected static String _currentScreen;
	protected static String _yourClass;
	protected static boolean _coin = false;
	protected static boolean _hearthstoneDetected;

	protected static ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(5);

	protected static JFrame f = new JFrame();

	protected static boolean _drawPaneAdded = false;

	protected static BufferedImage image;

	protected static JPanel _drawPane = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}
	};

	protected static boolean _testForMatchStart() {
		
		boolean passed = false;
		int[][] tests = {
				{403, 487, 201, 173, 94},	// title bar
				{946, 149, 203, 174, 96}	// bottom bar
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		if(pxTest.passed()) {
			if(_currentScreen != "Match Start") {
				_coin = false;
				_notify("Match Start detected");
				passed = true;
			}
			_currentScreen = "Match Start";
		}
		return passed;
	}
	protected static boolean _testForFindingOpponent() {
		
		boolean passed = false;
		int[][] tests = {
			{401, 143, 180, 122, 145},	// title bar
			{765, 583, 121, 72, 100}	// bottom bar
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		if(pxTest.passed()) {
			if(_currentScreen != "Finding Opponent") {
				_coin = false;
				_notify("Finding Opponent detected");
				passed = true;
			}
			_currentScreen = "Finding Opponent";
		}
		return passed;
	}
	protected static void _testForPlayingScreen() {
		int[][] tests = {
			{336, 203, 231, 198, 124},		
			{763, 440, 234, 198, 124}		
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		if(pxTest.passed()) {
			if(_currentScreen != "Playing") {
				_notify("Playing detected");
			}
			_currentScreen = "Playing";
		}
	}
	protected static boolean _testForPlayModeScreen() {
		
		boolean passed = false;
		int[][] tests = {
			{543, 130, 121, 32, 22},	// play mode red background
			{254, 33, 197, 173, 132}		// mode title light brown background
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		if(pxTest.passed()) {
			if(_currentScreen != "Play") {
				_notify("Play mode detected");
				passed = true;
			}
			_currentScreen = "Play";
		}
		return passed;
	}
	protected static boolean _testForMainMenuScreen() {
		
		boolean passed = false;
		int[][] tests = {
				{338, 453, 159, 96, 42},	// box top
				{211, 658, 228, 211, 116}	// quest button exclamation mark
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		if(pxTest.passed()) {
			if(_currentScreen != "Main Menu") {
				_notify("Main menu detected");
				passed = true;
			}
			_currentScreen = "Main Menu";
		}
		return passed;
	}
	protected static void _testForRankedMode() {
		
		int[][] tests = {
			{833, 88, 220, 255, 255},	// ranked blue
			{698, 120, 56, 16, 8}	// casual off
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		if(pxTest.passed()) {
			if(_gameMode != "Ranked") {
				_notify("Rank mode detected");
			}
			_gameMode = "Ranked";
		}
	}
	
	protected static NotificationQueue _notificationQueue = new NotificationQueue();
	protected static void _notify(String header) {
		_notify(header, "");
	}
	protected static void _notify(String header, String message) {
		_notificationQueue.add(new net.hearthstats.Notification(header, message));
		
	}
	protected static void _testForCoin() {
		
		int[][] tests = {
				{709, 317, 110, 254, 70}	// fourth card left edge
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		
		if(pxTest.passed()) {
			_notify("Coin detected");
			_coin = true;
		}
	}
	protected static void _testForCasualMode() {
		
		int[][] tests = {
			{833, 94, 100, 22, 16},	// ranked off
			{698, 128, 200, 255, 255}	// casual blue
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		
		if(pxTest.passed()) {
			if(_gameMode != "Casual") {
				_notify("Casual mode detected");
			}
			_gameMode = "Casual";
		}
	}
	
	protected static void _testForClass(String className, int[][] pixelTests, boolean isYours) {
		PixelGroupTest pxTest = new PixelGroupTest(image, pixelTests);
		if(pxTest.passed()) {
			if(isYours) {
				_yourClass = className;
				_notify("Playing as " + _yourClass);
			}
		}
	}
	
	protected static void _testForYourClass() {
		// Hunter Test
		int[][] hunterTests = {
				{289, 438, 173, 161, 147},	
				{366, 554, 250, 200, 81},
				{210, 675, 209, 209, 211}	
		};
		_testForClass("Hunter", hunterTests, true);
		
		// Mage Test
		int[][] mageTests = {
			{259, 439, 96, 31, 102},	
			{294, 677, 219, 210, 193},
			{216, 591, 0, 0, 56}	
		};
		_testForClass("Mage", mageTests, true);
	}
	
	protected static void _updateTitle() {
		String title = "HearthStats.net Uploader";
		if(_hearthstoneDetected) {
			if(_currentScreen != null) {
				title += " - " + _currentScreen;
				if((_currentScreen == "Play" || _currentScreen == "Playing")&& _gameMode != null) {
					title += " " + _gameMode;
				}
				if(_currentScreen == "Match Start") {
					if(_coin) {
						title += " Coin";
					} else {
						title += " No Coin";
					}
					if(_yourClass != null) {
						title += " " + _yourClass;
					}
				}
				if(_currentScreen == "Finding Opponent") {
					if(_gameMode != null) {
						title += " for " + _gameMode + " game";
					}
				}
				if(_currentScreen == "Playing") {
					if(_coin) {
						title += " Coin";
					} else {
						title += " No Coin";
					}
					if(_yourClass != null) {
						title += _yourClass;
					}
				}
			}
		} else {
			title += " - Waiting for Hearthstone ";
			title += Math.random() > 0.33 ? ".." : "...";
			f.setSize(600, 200);
		}
		f.setTitle(title);
	}
	
	protected static void _drawImageFrame() {
		if (!_drawPaneAdded) {
			f.add(_drawPane);
		}
		_drawPane.repaint();
		f.invalidate();
		f.validate();
		f.repaint();
	}
	protected static boolean _updateImage() throws JnaUtilException, IOException {
		Pointer hWnd = JnaUtil.getWinHwnd("Hearthstone");
		String windowText = JnaUtil.getWindowText(hWnd).toString();
		if(windowText.matches("Hearthstone")) {
			Rectangle rect = JnaUtil.getWindowRect(hWnd);
			// make sure the window is completely open before trying to capture the image
			if(rect.width >= 1024) {
				f.setSize(rect.width, rect.height);
				image = capture(User32.INSTANCE.FindWindow(null, "Hearthstone"));
				return true;
			}
		}
		return false;
	}

	protected static void _detectStates() {

		if(_currentScreen != "Main Menu") {
			_testForMainMenuScreen();
		}
		if(_currentScreen == "Play") {
			if(_currentScreen != "Finding Opponent") {
				_testForFindingOpponent();
				_testForRankedMode();
				_testForCasualMode();
			}
		} else {
			_testForPlayModeScreen();	
		}
		
		if(_currentScreen == "Finding Opponent") {
			_testForMatchStart();
			_coin = false;			// reset to no coin
			_yourClass = null; 		// reset your class to unknown
		}
		if(_currentScreen == "Match Start") {
			if(!_coin) {
				_testForCoin();
			}
			if(_yourClass == null) {
				_testForYourClass();
			}
			_testForPlayingScreen();
		}
		if(_currentScreen == "Playing") {
			// listen for victory or defeat
		}
	}
	@SuppressWarnings("unchecked")
	protected static void _poll() {
		try {
			scheduledExecutorService.schedule(
				new Callable() {
					public Object call() throws Exception {
						if(_updateImage()) {
							if(_hearthstoneDetected != true) {
								_hearthstoneDetected = true;
								_notify("Hearthstone found");
							}
							_detectStates();
							_drawImageFrame();
						} else {
							if(_hearthstoneDetected) {
								_hearthstoneDetected = false;
								_notify("Hearthstone closed");
								f.getContentPane().removeAll();
								_drawPaneAdded = false;
							}
						}
						_updateTitle();
						try {
							_poll();
						} catch(Exception e) {
							boolean foo = true;
						}
						return "Called!";
					}
				}, 200, TimeUnit.MILLISECONDS);
		} catch(Exception e) {
			boolean foo = true;
		}
	}

	protected static BufferedImage capture(HWND hWnd) {

		HDC hdcWindow = User32.INSTANCE.GetDC(hWnd);
		HDC hdcMemDC = GDI32.INSTANCE.CreateCompatibleDC(hdcWindow);

		RECT bounds = new RECT();
		User32Extra.INSTANCE.GetClientRect(hWnd, bounds);

		int width = bounds.right - bounds.left;
		int height = bounds.bottom - bounds.top;

		HBITMAP hBitmap = GDI32.INSTANCE.CreateCompatibleBitmap(hdcWindow,
				width, height);

		HANDLE hOld = GDI32.INSTANCE.SelectObject(hdcMemDC, hBitmap);
		GDI32Extra.INSTANCE.BitBlt(hdcMemDC, 0, 0, width, height, hdcWindow, 0,
				0, WinGDIExtra.SRCCOPY);

		GDI32.INSTANCE.SelectObject(hdcMemDC, hOld);
		GDI32.INSTANCE.DeleteDC(hdcMemDC);

		BITMAPINFO bmi = new BITMAPINFO();
		bmi.bmiHeader.biWidth = width;
		bmi.bmiHeader.biHeight = -height;
		bmi.bmiHeader.biPlanes = 1;
		bmi.bmiHeader.biBitCount = 32;
		bmi.bmiHeader.biCompression = WinGDI.BI_RGB;

		Memory buffer = new Memory(width * height * 4);
		GDI32.INSTANCE.GetDIBits(hdcWindow, hBitmap, 0, height, buffer, bmi,
				WinGDI.DIB_RGB_COLORS);

		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_RGB);
		image.setRGB(0, 0, width, height,
				buffer.getIntArray(0, width * height), 0, width);

		GDI32.INSTANCE.DeleteObject(hBitmap);
		User32.INSTANCE.ReleaseDC(hWnd, hdcWindow);

		return image;

	}

}
