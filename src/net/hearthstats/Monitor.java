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
	
	protected static int _pollingIntervalInMs = 100;
	protected static String _gameMode;
	protected static String _currentScreen;
	protected static String _yourClass;
	protected static String _opponentClass;
	protected static String _result;
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
		
		// check for normal play boards
		int[][] tests = {
			{336, 203, 231, 198, 124},		
			{763, 440, 234, 198, 124}
		};
		PixelGroupTest normalPxTest = new PixelGroupTest(image, tests);
		
		// check for lighter orc board
		int[][] orcBoardTests = {
			{906, 283, 222, 158, 94},		
			{120, 468, 195, 134, 78}		
		};
		PixelGroupTest orcPxTest = new PixelGroupTest(image, orcBoardTests);
		
		if(normalPxTest.passed() || orcPxTest.passed()) {
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
			{254, 33, 197, 173, 132},	// mode title light brown background
			{956, 553, 24, 8, 8},
			{489, 688, 68, 65, 63}
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
				{869, 389, 155, 250, 103}	// fourth card right edge
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		
		if(pxTest.passed()) {
			_notify("Coin detected");
			_coin = true;
		}
	}
	protected static void _testForDefeat() {
		
		int[][] tests = {
				{745, 219, 164, 162, 155},	
				{344, 383, 134, 153, 239},	
				{696, 357, 201, 197, 188}	
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		
		int[][] testsTwo = {
				{347, 382, 129, 148, 236},	
				{275, 345, 137, 138, 134},	
				{537, 155, 235, 226, 67}	
		};
		PixelGroupTest pxTestTwo = new PixelGroupTest(image, testsTwo);
		
		int[][] testsThree = {
				{347, 382, 129, 148, 236},	
				{275, 345, 137, 138, 134},	
				{537, 155, 235, 226, 67}	
		};
		PixelGroupTest pxTestThree = new PixelGroupTest(image, testsThree);
		
		if(pxTest.passed() || pxTestTwo.passed() || pxTestThree.passed()) {
			_notify("Defeat detected");
			_result = "Defeat";
		}
	}
	protected static void _testForVictory() {
		
		int[][] tests = {
				{334, 504, 88, 101, 192},	
				{683, 510, 74, 88, 173},	
				{549, 162, 255, 224, 119}	
		};
		PixelGroupTest pxTest = new PixelGroupTest(image, tests);
		
		int[][] testsTwo = {
				{347, 469, 85, 102, 203},	
				{737, 339, 63, 76, 148},	
				{774, 214, 253, 243, 185}	
		};
		PixelGroupTest pxTestTwo = new PixelGroupTest(image, testsTwo);
		
		int[][] testsThree = {
				{370, 528, 66, 81, 165},	
				{690, 553, 63, 76, 162},	
				{761, 228, 249, 234, 163}	
		};
		PixelGroupTest pxTestThree = new PixelGroupTest(image, testsThree);
		
		if(pxTest.passed() || pxTestTwo.passed() || pxTestThree.passed()) {
			_notify("Victory detected");
			_result = "Victory";
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
			} else {
				_opponentClass = className;
				_notify("Playing VS. " + _opponentClass);
			}
		}
	}
	
	protected static void _testForYourClass() {
		// Druid Test
		int[][] druidTests = {
				{225, 480, 210, 255, 246},	
				{348, 510, 234, 255, 251},
				{237, 607, 193, 155, 195}	
		};
		_testForClass("Druid", druidTests, true);
		
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
		
		// Paladin Test
		int[][] paladinTests = {
				{249, 447, 133, 105, 165},	
				{304, 671, 74, 146, 234},
				{368, 581, 244, 238, 141}	
		};
		_testForClass("Paladin", paladinTests, true);
		
		// Priest Test
		int[][] priestTests = {
				{229, 491, 180, 178, 166},	
				{256, 602, 82, 104, 204},
				{350, 611, 22, 23, 27}	
		};
		_testForClass("Priest", priestTests, true);
		
		// Rogue Test
		int[][] rogueTests = {
				{309, 446, 91, 107, 175},	
				{291, 468, 187, 37, 25},
				{362, 623, 122, 186, 67}	
		};
		_testForClass("Rogue", rogueTests, true);
		
		// Shaman Test
		int[][] shamanTests = {
				{223, 458, 4, 46, 93},	
				{360, 533, 213, 32, 6},
				{207, 578, 177, 245, 249}	
		};
		_testForClass("Shaman", shamanTests, true);
		
		// Warlock Test
		int[][] warlockTests = {
				{301, 435, 104, 138, 8},	
				{265, 493, 221, 51, 32},
				{294, 680, 60, 75, 182}	
		};
		_testForClass("Warlock", warlockTests, true);
	}
	
	protected static void _testForOpponentClass() {
		// Druid Test
		int[][] druidTests = {
				{743, 118, 205, 255, 242},	
				{882, 141, 231, 255, 252},
				{766, 215, 203, 160, 198}	
		};
		_testForClass("Druid", druidTests, false);
		
		// Hunter Test
		int[][] hunterTests = {
				{825, 66, 173, 178, 183},	
				{818, 175, 141, 37, 0},
				{739, 309, 216, 214, 211}	
		};
		_testForClass("Hunter", hunterTests, false);
		
		// Mage Test
		int[][] mageTests = {
				{790, 68, 88, 23, 99},	
				{788, 312, 215, 188, 177},
				{820, 247, 53, 64, 82}	
		};
		_testForClass("Mage", mageTests, false);
		
		// Paladin Test
		int[][] paladinTests = {
				{895, 213, 255, 247, 147},	
				{816, 301, 125, 237, 255},
				{767, 82, 133, 107, 168}	
		};
		_testForClass("Paladin", paladinTests, false);
		
		// Priest Test
		int[][] priestTests = {
				{724, 189, 255, 236, 101},	
				{796, 243, 58, 72, 138},
				{882, 148, 27, 20, 38}	
		};
		_testForClass("Priest", priestTests, false);
		
		// Rogue Test
		int[][] rogueTests = {
				{889, 254, 132, 196, 72},	
				{790, 273, 88, 21, 34},
				{841, 73, 100, 109, 183}	
		};
		_testForClass("Rogue", rogueTests, false);
		
		// Shaman Test
		int[][] shamanTests = {
				{748, 94, 5, 50, 100},	
				{887, 169, 234, 50, 32},
				{733, 206, 186, 255, 255}	
		};
		_testForClass("Shaman", shamanTests, false);
		
		// Warlock Test
		int[][] warlockTests = {
				{828, 64, 107, 135, 3},	
				{749, 124, 223, 46, 28},
				{835, 266, 247, 251, 255}	
		};
		_testForClass("Warlock", warlockTests, false);
		
		// Warrior Test
		int[][] warriorTests = {
				{795, 64, 37, 4, 0},	
				{780, 83, 167, 23, 4},
				{809, 92, 255, 247, 227}	
		};
		_testForClass("Warrior", warriorTests, false);
	}
	
	protected static void _updateTitle() {
		String title = "HearthStats.net Uploader";
		if(_hearthstoneDetected) {
			if(_currentScreen != null) {
				title += " - " + _currentScreen;
				if(_currentScreen == "Play" && _gameMode != null) {
					title += " " + _gameMode;
				}
				if(_currentScreen == "Finding Opponent") {
					if(_gameMode != null) {
						title += " for " + _gameMode + " Game";
					}
				}
				if(_currentScreen == "Match Start" || _currentScreen == "Playing") {
					if(_gameMode != null) {
						title += " " + _gameMode;
					}
					if(_coin) {
						title += " Coin";
					} else {
						title += " No Coin";
					}
					if(_yourClass != null) {
						title += " " + _yourClass;
					}
					if(_opponentClass != null) {
						title += " VS. " + _opponentClass;
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

	protected static void _submitMatchResult() {
		String header = "Submitting match result";
		String message = _gameMode + " game " + (_coin ? "" : "no ") + "coin " +
						_yourClass + " VS. " + _opponentClass + " " + _result;
		_notify(header, message);
	}
	
	protected static void _detectStates() {
			
		// main menu
		if(_currentScreen != "Main Menu") {
			_testForMainMenuScreen();
		}
		
		// play mode screen
		if(_currentScreen == "Play") {
			if(_currentScreen != "Finding Opponent") {
				_testForFindingOpponent();
				_testForRankedMode();
				_testForCasualMode();
			}
		} else {
			_testForPlayModeScreen();	
		}
		
		// finding opponent window
		if(_currentScreen == "Finding Opponent") {
			_testForMatchStart();
			_coin = false;			// reset to no coin
			_yourClass = null; 		// reset your class to unknown
			_opponentClass = null; 	// reset opponent class to unknown
			_result = null;			// reset result to unknown
		}
		
		// match start and setup (mulligan phase)
		if(_currentScreen == "Match Start") {
			if(!_coin) {
				_testForCoin();
			}
			if(_yourClass == null) {
				_testForYourClass();
			}
			if(_opponentClass == null) {
				_testForOpponentClass();
			}
			_testForPlayingScreen();
		}
		
		// playing a game
		if(_currentScreen == "Playing") {
			// listen for victory or defeat
			if(_result == null) {
				_testForDefeat();
				_testForVictory();
			} else {
				// submit game once result is found
				_currentScreen = "Result";
				_submitMatchResult();
			}
			
		}
	}
	@SuppressWarnings("unchecked")
	protected static void _poll() {
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
					_poll();
					return "Called!";
				}
			}, _pollingIntervalInMs, TimeUnit.MILLISECONDS);
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
