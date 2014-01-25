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
import java.util.Observable;
import java.util.Observer;
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
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinUser.WNDENUMPROC;

public class Monitor extends JFrame implements Observer {

	protected HearthstoneAnalyzer _analyzer = new HearthstoneAnalyzer();
	
	public void start() throws JnaUtilException, IOException {

		Image icon = new ImageIcon("images/icon.png").getImage();

		f.setIconImage(icon);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setLocation(0, 0);
		f.setVisible(true);

		_analyzer.addObserver(this);
		_pollHearthstone();

	}

	protected ProgramHelper _hsHelper = new ProgramHelper("Hearthstone");
	protected int _pollingIntervalInMs = 100;
	protected boolean _hearthstoneDetected;
	

	protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

	protected JFrame f = new JFrame();

	protected boolean _drawPaneAdded = false;

	protected BufferedImage image;

	protected JPanel _drawPane = new JPanel() {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawImage(image, 0, 0, null);
		}
	};

	protected NotificationQueue _notificationQueue = new NotificationQueue();

	protected void _notify(String header) {
		_notify(header, "");
	}

	protected void _notify(String header, String message) {
		_notificationQueue.add(new net.hearthstats.Notification(header, message));

	}

	protected void _updateTitle() {
		String title = "HearthStats.net Uploader";
		if (_hearthstoneDetected) {
			if (_analyzer.getScreen() != null) {
				title += " - " + _analyzer.getScreen();
				if (_analyzer.getScreen() == "Play" && _analyzer.getMode() != null) {
					title += " " + _analyzer.getMode();
				}
				if (_analyzer.getScreen() == "Finding Opponent") {
					if (_analyzer.getMode() != null) {
						title += " for " + _analyzer.getMode() + " Game";
					}
				}
				if (_analyzer.getScreen() == "Match Start" || _analyzer.getScreen() == "Playing") {
					if (_analyzer.getMode() != null) {
						title += " " + _analyzer.getMode();
					}
					if (_analyzer.getCoin()) {
						title += " Coin";
					} else {
						title += " No Coin";
					}
					if (_analyzer.getYourClass() != null) {
						title += " " + _analyzer.getYourClass();
					}
					if (_analyzer.getOpponentClass() != null) {
						title += " VS. " + _analyzer.getOpponentClass();
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

	protected void _updateImageFrame() {
		if (!_drawPaneAdded) {
			f.add(_drawPane);
		}
		if (image.getWidth() >= 1024) {
			f.setSize(image.getWidth(), image.getHeight());
		}
		_drawPane.repaint();
		f.invalidate();
		f.validate();
		f.repaint();
	}

	protected void _submitMatchResult() {
		String header = "Submitting match result";
		String message = _analyzer.getMode() + " game " + 
						(_analyzer.getCoin() ? "" : "no ") + "coin " + 
						_analyzer.getYourClass() + " VS. " + 
						_analyzer.getOpponentClass() + " " + 
						_analyzer.getResult();
		_notify(header, message);
	}
	
	protected void _handleHearthstoneFound() throws JnaUtilException {
		
		// mark hearthstone found if necessary
		if (_hearthstoneDetected != true) {
			_hearthstoneDetected = true;
			_notify("Hearthstone found");
		}
		
		// grab the image from Hearthstone
		image = _hsHelper.getScreenCapture();
		
		// detect image stats 
		if (image.getWidth() >= 1024)
			_analyzer.analyze(image);
		
		_updateImageFrame();
	}
	
	protected void _handleHearthstoneNotFound() {
		
		// mark hearthstone not found if necessary
		if (_hearthstoneDetected) {
			_hearthstoneDetected = false;
			_notify("Hearthstone closed");
			
			f.getContentPane().removeAll();	// empty out the content pane
			_drawPaneAdded = false;
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void _pollHearthstone() {
		scheduledExecutorService.schedule(new Callable() {
			public Object call() throws Exception {
				
				if (_hsHelper.foundProgram())
					_handleHearthstoneFound();
				else
					_handleHearthstoneNotFound();
				
				_updateTitle();
				
				_pollHearthstone();		// repeat the process
				
				return "";
			}
		}, _pollingIntervalInMs, TimeUnit.MILLISECONDS);
	}

	@Override
	public void update(Observable analyzer, Object changed) {
		switch(changed.toString()) {
			case "coin":
				_notify("Coin Detected");
				break;
			case "deckSlot":
				_notify("Deck Slot " + _analyzer.getDeckSlot() + " Detected");
				break;
			case "mode":
				_notify(_analyzer.getMode() + " Mode Detected");
				break;
			case "opponentClass":
				_notify("Playing vs " + _analyzer.getOpponentClass());
				break;
			case "result":
				_notify(_analyzer.getResult() + " Detected");
				_submitMatchResult();
				break;
			case "screen":
				if(_analyzer.getScreen() != "Result")
					_notify(_analyzer.getScreen() + " Screen Detected");
				break;
			case "yourClass":
				_notify("Playing as " + _analyzer.getYourClass());
				break;
		}
	}


}
