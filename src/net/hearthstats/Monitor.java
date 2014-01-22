package net.hearthstats;

import jna.*;
import jna.extra.GDI32Extra;
import jna.extra.User32Extra;
import jna.extra.WinGDIExtra;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

	public Monitor() throws JnaUtilException, IOException {

		updateImage();

		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setLocation(0, 0);
		f.setVisible(true);
		f.setTitle("HearthStats.net Monitor");

		poll();

	}
	
	protected int _xOffset = 0;
	protected int _yOffset = 0;
	protected String _gameMode;

	protected ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(5);

	protected JFrame f = new JFrame();

	protected boolean added = false;

	protected BufferedImage image;

	protected JPanel pane = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}
	};
	
	protected PixelGroup _getPixelGroup(int x, int y) {
		return new PixelGroup(image, x + _xOffset, y + _yOffset, 3, 3);
	}

	protected void _testFoorRankedMode() {
		PixelGroup rankedPixels = _getPixelGroup(833, 88);
		PixelGroup casualPixels = _getPixelGroup(698, 118);
		
		if(rankedPixels.rbgGtEq(120, 240, 210) && casualPixels.rbgLtEq(200, 170, 190)) {
			if(_gameMode != "Ranked") {
				System.out.println("Entering Ranked Mode");
			}
			_gameMode = "Ranked";
		}
	}
	protected void _testFoorCasualMode() {
		PixelGroup rankedPixels = _getPixelGroup(833, 88);
		PixelGroup casualPixels = _getPixelGroup(698, 118);
		
		if(casualPixels.rbgGtEq(120, 240, 210) && rankedPixels.rbgLtEq(200, 170, 190)) {
			if(_gameMode != "Casual") {
				System.out.println("Entering Casual Mode");
			}
			_gameMode = "Casual";
		}
	}
	
	protected void _drawImageFrame() {
		if (!added) {
			f.add(pane);
		}
		pane.repaint();
		f.invalidate();
		f.validate();
		f.repaint();
	}
	protected void updateImage() throws JnaUtilException, IOException {
		Pointer hWnd = JnaUtil.getWinHwnd("Hearthstone");
		Rectangle rect = JnaUtil.getWindowRect(hWnd);
		f.setSize(rect.width, rect.height);
		image = capture(User32.INSTANCE.FindWindow(null, "Hearthstone"));
		
		_testFoorRankedMode();
		_testFoorCasualMode();
		
		_drawImageFrame();
	}

	protected void poll() {
		ScheduledFuture scheduledFuture = scheduledExecutorService.schedule(
				new Callable() {
					public Object call() throws Exception {
						updateImage();
						poll();
						return "Called!";
					}
				}, 100, TimeUnit.MILLISECONDS);
	}

	protected BufferedImage capture(HWND hWnd) {

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
