package net.hearthstats;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.File;
import java.io.IOException;
import java.util.Observable;

import javax.imageio.ImageIO;

public class HearthstoneAnalyzer extends Observable {

	private boolean _coin;
	private BufferedImage _image;
	private String _mode;
	private String _opponentClass;
	private String _opponentName;
	private String _result;
	private String _screen;
	private String _yourClass;
	private int _deckSlot;
	private boolean _isNewArena = false;
	private float _ratio;
	private int _xOffset;
	private int _width;
	private int _height;
	private float _screenRatio;
	private boolean _arenaRunEndDetected = false;

	public HearthstoneAnalyzer() {
	}

	public void analyze(BufferedImage image) {
		
		_image = image;
		
		_calculateResolutionRatios();
		
		if(getScreen() != "Main Menu" && getScreen() != "Playing") {
			_testForMainMenuScreen();
		}

		if(getScreen() != "Play") {
			if(getMode() != "Practice") {
				_testForPracticeScreen();
			}
			_testForPlayScreen();
		}
		
		if(getScreen() != "Arena") {
			_testForArenaModeScreen();
		}
		
		if((getScreen() == "Play" || getScreen() == "Arena") && getMode() != "Practice") {
			_testForFindingOpponent();
			if (getScreen() == "Play") {
				if(getMode() != "Casual")
					_testForCasualMode();
				if(getMode() != "Ranked")
					_testForRankedMode();
				_testForDeckSlot();
			}
		}
		
		if((getScreen() == "Match Start" || getScreen() == "Opponent Name") && getMode() != "Practice") {
			if(getYourClass() == null)
				_testForYourClass();
			if(getOpponentClass() == null)
				_testForOpponentClass();
			if(!getCoin())
				_testForCoin();
			if(getScreen() != "Opponent Name")
				_testForOpponentName();
		} else {
			if(getScreen() != "Playing" && getMode() != "Practice")
				_testForMatchStartScreen();
		}
		
		if((getScreen() == "Result" || getScreen() == "Arena") && !_arenaRunEndDetected ) {
			_testForArenaEnd();
		}
		
		if(getMode() != "Practice") {
			if(getScreen() == "Playing") {
				_testForVictory();
				_testForDefeat();
			} else {
				if(getScreen() != "Result")
					_testForPlayingScreen();
			}
		}
		
		if(getScreen() == "Arena" && !isNewArena()) {
			_testForNewArenaRun();
		}
		
		_image.flush();
	}

	private void _calculateResolutionRatios() {
		// handle 4:3 screen ratios
		_ratio = _image.getHeight() / (float) 768;
		_xOffset = 0;
		_width = _image.getWidth();
		_height = _image.getHeight();
		_screenRatio = (float) _width / _height;
		
		// handle widescreen x offsets
		if(_screenRatio > 1.4) {
			_xOffset = 107;
			_xOffset = (int) (((float) _width - (_ratio * 1024)) / 2);
		}
	}

	public void reset() {
		_coin = false;
		_yourClass = null;
		_opponentClass = null;
		_opponentName = null;
		_screen = null;
		_mode = null;
		_deckSlot = 0;
		_arenaRunEndDetected = false;
	}
	public boolean getCoin() {
		return _coin;
	}
	
	public int getDeckSlot() {
		return _deckSlot;
	}
	
	public String getMode() {
		return _mode;
	}
	
	public String getOpponentClass() {
		return _opponentClass;
	}
	
	public String getOpponentName() {
		return _opponentName;
	}
	
	public String getResult() {
		return _result;
	}
	
	public String getScreen() {
		return _screen;
	}
	
	public String getYourClass() {
		return _yourClass;
	}
	
	public boolean isNewArena() {
		return _isNewArena;
	}

	private void _notifyObserversOfChangeTo(String property) {
		setChanged();
	    notifyObservers(property);
	}
	
	public void setIsNewArena(boolean isNew) {
		_isNewArena = isNew;
		_notifyObserversOfChangeTo("newArena");
	}
	
	private void _setDeckSlot(int deckSlot) {
		_deckSlot = deckSlot;
		_notifyObserversOfChangeTo("deckSlot");
	}
	
	private void _setCoin(boolean coin) {
		_coin = coin;
		_notifyObserversOfChangeTo("coin");
	}
	
	private void _setMode(String screen) {
		_mode = screen;
		_notifyObserversOfChangeTo("mode");
	}
	
	private void _setOpponentClass(String opponentClass) {
		_opponentClass = opponentClass;
		_notifyObserversOfChangeTo("opponentClass");
	}
	
	private void _setOpponentName(String opponentName) {
		_opponentName = opponentName;
		_notifyObserversOfChangeTo("opponentName");
	}
	
	private void _setResult(String result) {
		_result = result;
		_notifyObserversOfChangeTo("result");
	}
	
	private void _setScreen(String screen) {
		_screen = screen;
		_notifyObserversOfChangeTo("screen");
	}
	
	private void _setYourClass(String yourClass) {
		_yourClass = yourClass;
		_notifyObserversOfChangeTo("yourClass");
	}
	
	private void _testForArenaEnd() {
		
		int[][] tests = { 
				{ 315, 387, 239, 32, 39 }, 
				{ 399, 404, 237, 41, 33 }, 
				{ 448, 408, 223, 8, 16 }
		};
		if((new PixelGroupTest(_image, tests)).passed()) {
			_screen = "Arena";
			_arenaRunEndDetected = true;
			_notifyObserversOfChangeTo("arenaEnd");
		}
	}
	
	private void _testForArenaModeScreen() {

		int[][] tests = { 
			{ 807, 707, 95, 84, 111 }, 
			{ 324, 665, 77, 114, 169 }, 
			{ 120, 685, 255, 215, 115 }, 
			{ 697, 504, 78, 62, 56 } 
		};
		if((new PixelGroupTest(_image, tests)).passed()) {
			_setScreen("Arena");
			_setMode("Arena");
		}
	}
	
	private void _testForCasualMode() {

		int[][] tests = { 
			{ 833, 94, 100, 22, 16 }, // ranked off
			{ 698, 128, 200, 255, 255 } // casual blue
		};
		PixelGroupTest testOne = new PixelGroupTest(_image, tests);
		
		int[][] testsTwo = { 
				{ 812, 178, 255, 255, 255}, 
				{ 758, 202, 215, 255, 255 } 
		};
		PixelGroupTest testTwo = new PixelGroupTest(_image, testsTwo);
		
		if(testOne.passed() || testTwo.passed())
			_setMode("Casual");
	}
	
	private void _testForCoin() {

		int[][] tests = { 
			// fourth card right edge
			{ 869, 389, 155, 250, 103 }, 
			{ 864, 328, 130, 255, 85 }, 
			{ 870, 340, 127, 255, 83 },
			{ 769, 280, 125, 255, 82 },
			{ 864, 379, 126, 255, 82 }
		};
		if((new PixelGroupTest(_image, tests)).passedOr())
			_setCoin(true);
	}
	
	private void _testForDeckSlot() {

		if(getDeckSlot() != 1) {
			int[][] slotOnePixels = { 
				{ 163, 178, 33, 129, 242},
				{ 183, 178, 33, 129, 242}
			};
			if((new PixelGroupTest(_image, slotOnePixels)).passedOr())
				_setDeckSlot(1);
		}

		if(getDeckSlot() != 2) {
			int[][] slotTwoPixels = { 
				{ 348, 178, 36, 144, 247 },
				{ 368, 178, 36, 144, 247 }
			};
			if((new PixelGroupTest(_image, slotTwoPixels)).passedOr())
				_setDeckSlot(2);
		}
		
		if(getDeckSlot() != 3) {
			int[][] slotTwoPixels = { 
					{ 506, 178, 36, 144, 247 },
					{ 526, 178, 36, 144, 247 }
			};
			if((new PixelGroupTest(_image, slotTwoPixels)).passedOr())
				_setDeckSlot(3);
		}
		
		if(getDeckSlot() != 4) {
			int[][] slotOnePixels = { 
					{ 163, 339, 33, 129, 242},
					{ 183, 339, 33, 129, 242}
			};
			if((new PixelGroupTest(_image, slotOnePixels)).passedOr())
				_setDeckSlot(4);
		}
		
		if(getDeckSlot() != 5) {
			int[][] slotTwoPixels = { 
					{ 348, 339, 36, 144, 247 },
					{ 368, 339, 36, 144, 247 }
			};
			if((new PixelGroupTest(_image, slotTwoPixels)).passedOr())
				_setDeckSlot(5);
		}
		
		if(getDeckSlot() != 6) {
			int[][] slotTwoPixels = { 
					{ 506, 339, 36, 144, 247 },
					{ 526, 339, 36, 144, 247 }
			};
			if((new PixelGroupTest(_image, slotTwoPixels)).passedOr())
				_setDeckSlot(6);
		}

		if(getDeckSlot() != 7) {
			int[][] slotOnePixels = { 
					{ 163, 497, 33, 129, 242},
					{ 183, 497, 33, 129, 242}
			};
			if((new PixelGroupTest(_image, slotOnePixels)).passedOr())
				_setDeckSlot(7);
		}
		
		if(getDeckSlot() != 8) {
			int[][] slotTwoPixels = { 
					{ 348, 497, 36, 144, 247 },
					{ 368, 497, 36, 144, 247 }
			};
			if((new PixelGroupTest(_image, slotTwoPixels)).passedOr())
				_setDeckSlot(8);
		}
		
		if(getDeckSlot() != 9) {
			int[][] slotTwoPixels = { 
					{ 506, 497, 36, 144, 247 },
					{ 526, 497, 36, 144, 247 }
			};
			if((new PixelGroupTest(_image, slotTwoPixels)).passedOr())
				_setDeckSlot(9);
		}
		
	}
	
	private void _testForDefeat() {

		int[][] tests = { 
			{ 745, 219, 164, 162, 155 }, 
			{ 344, 383, 134, 153, 239 }, 
			{ 696, 357, 201, 197, 188 } 
		};
		PixelGroupTest pxTest = new PixelGroupTest(_image, tests);

		int[][] testsTwo = { 
			{ 347, 382, 129, 148, 236 }, 
			{ 275, 345, 137, 138, 134 }, 
			{ 537, 155, 235, 226, 67 } 
		};
		PixelGroupTest pxTestTwo = new PixelGroupTest(_image, testsTwo);

		int[][] testsThree = { 
			{ 347, 382, 129, 148, 236 }, 
			{ 275, 345, 137, 138, 134 }, 
			{ 537, 155, 235, 226, 67 } 
		};
		PixelGroupTest pxTestThree = new PixelGroupTest(_image, testsThree);

		if (pxTest.passed() || pxTestTwo.passed() || pxTestThree.passed()) {
			_setScreen("Result");
			_setResult("Defeat");
		}
	}
	
	private void _testForClass(String className, int[][] pixelTests, boolean isYours) {
		if((new PixelGroupTest(_image, pixelTests)).passed()) {
			if(isYours)
				_setYourClass(className);
			else
				_setOpponentClass(className);
		}
	}

	private void _testForFindingOpponent() {

		int[][] tests = { 
			{ 401, 143, 180, 122, 145 }, 	// title bar
			{ 765, 583, 121, 72, 100 } 		// bottom bar
		};
		PixelGroupTest pxTest = new PixelGroupTest(_image, tests);

		int[][] arenaTests = { 
			{ 393, 145, 205, 166, 135 }, 	// title bar
			{ 819, 235, 109, 87, 79 }, 
			{ 839, 585, 139, 113, 77 } 
		};
		PixelGroupTest arenaPxTest = new PixelGroupTest(_image, arenaTests);

		if (pxTest.passed() || arenaPxTest.passed()) {
			_coin = false;
			_yourClass = null;
			_opponentClass = null;
			_opponentName = null;
			_arenaRunEndDetected = false;
			_setScreen("Finding Opponent");
		}
	}
	private void _testForOpponentName() {
		int[][] tests = { 
			{ 383, 116, 187, 147, 79 },		// title banner left
			{ 631, 107, 173, 130, 70 },		// title banner right
			{ 505, 595, 129, 199, 255 }		// confirm button
		};
		PixelGroupTest pxTest = new PixelGroupTest(_image, tests);
		
		int[][] testsTwo = { 
				{ 379, 118, 189, 146, 82 },		// title banner left
				{ 641, 119, 197, 154, 86 },		// title banner right
				{ 515, 622, 121, 191, 255 }		// confirm button
		};
		PixelGroupTest pxTestTwo = new PixelGroupTest(_image, testsTwo);
		
		if(pxTest.passed() || pxTestTwo.passed()) {
			_setScreen("Opponent Name");
			_analyzeOpponnentName();
		}
	}
	
	private void _analyzeOpponnentName() {
		
		int x = (int) ((getMode() == "Ranked" ? 76 : 6) * _ratio);
		int y = (int) (34 * _ratio);	// with class name underneath
//		int y = (int) (40 * ratio);
		int imageWidth = (int) (150 * _ratio);
		int imageHeight = (int) (19 * _ratio);
	    
		int bigWidth = imageWidth * 3;
		int bigHeight = imageHeight * 3;
		
		// get cropped image of name
		BufferedImage opponentNameImg = _image.getSubimage(x, y, imageWidth, imageHeight);
		
		// to gray scale
		BufferedImage grayscale = new BufferedImage(opponentNameImg.getWidth(), opponentNameImg.getHeight(), BufferedImage.TYPE_INT_RGB); 
		BufferedImageOp grayscaleConv = 
			      new ColorConvertOp(opponentNameImg.getColorModel().getColorSpace(), 
			    		  grayscale.getColorModel().getColorSpace(), null);
			   grayscaleConv.filter(opponentNameImg, grayscale);
			   
		// blow it up for ocr
		BufferedImage newImage = new BufferedImage(bigWidth, bigHeight, BufferedImage.TYPE_INT_RGB);
		Graphics g = newImage.createGraphics();
		g.drawImage(grayscale, 0, 0, bigWidth, bigHeight, null);
		g.dispose();
		
		// invert image
		 for (x = 0; x < newImage.getWidth(); x++) {
            for (y = 0; y < newImage.getHeight(); y++) {
                int rgba = newImage.getRGB(x, y);
                Color col = new Color(rgba, true);
                col = new Color(255 - col.getRed(),
                                255 - col.getGreen(),
                                255 - col.getBlue());
                newImage.setRGB(x, y, col.getRGB());
            }
        }
		 
		// increase contrast
			RescaleOp rescaleOp = new RescaleOp(1.8f, -30, null);
			rescaleOp.filter(newImage, newImage);  // Source and destination are the same.
		
		// save it to a file
		File outputfile = new File(Main.getExtractionFolder() + "/opponentname.jpg");
		try {
			ImageIO.write(newImage, "jpg", outputfile);
		} catch (IOException e) {
			e.printStackTrace();
			_notifyObserversOfChangeTo("Exception trying to write opponent name image:\n" + e.getMessage());
		}
		try {
			_setOpponentName(OCR.process(Main.getExtractionFolder() + "/opponentname.jpg"));
		} catch(Exception e) {
			e.printStackTrace();
			_notifyObserversOfChangeTo("Exception tryint to analyze opponent name image:\n" + e.getMessage());
		}
	}
	
	private void _testForMainMenuScreen() {

		int[][] tests = { 
			{ 338, 453, 159, 96, 42 }, 		// box top
			{ 211, 658, 228, 211, 116 }, 	// quest button exclamation mark
			{ 513, 148, 36, 23, 24 } 		// dark vertical line in top center
		};
		if((new PixelGroupTest(_image, tests)).passed())
			_setScreen("Main Menu");
	}
	
	private void _testForMatchStartScreen() {

		int[][] tests = { 
			{ 403, 487, 201, 173, 94 }, // title bar
			{ 946, 149, 203, 174, 96 } // bottom bar
		};
		if ((new PixelGroupTest(_image, tests)).passed())  {
			_setScreen("Match Start");
		}
	}
	
	private void _testForNewArenaRun() {
		int[][] tests = { 
				{ 298, 170, 255, 239, 148 },	// key
				//{ 492, 204, 128, 79, 47 }, 		// key stem
				{ 382, 294, 255, 255, 255 }, 	// zero
				{ 393, 281, 255, 255, 255 }, 	// zero
				{ 321, 399, 209, 179, 127 }, 	// no red x
		};
		
		if ((new PixelGroupTest(_image, tests)).passed())  {
			setIsNewArena(true);
		}
	}
	private void _testForOpponentClass() {
		
		// Druid Test
		int[][] druidTests = { 
			{ 743, 118, 205, 255, 242 }, 
			{ 882, 141, 231, 255, 252 }, 
			{ 766, 215, 203, 160, 198 } 
		};
		_testForClass("Druid", druidTests, false);

		// Hunter Test
		int[][] hunterTests = { 
			{ 825, 66, 173, 178, 183 }, 
			{ 818, 175, 141, 37, 0 }, 
			{ 739, 309, 216, 214, 211 } 
		};
		_testForClass("Hunter", hunterTests, false);

		// Mage Test
		int[][] mageTests = { 
			{ 790, 68, 88, 23, 99 }, 
			{ 788, 312, 215, 188, 177 }, 
			{ 820, 247, 53, 64, 82 }
		};
		_testForClass("Mage", mageTests, false);

		// Paladin Test
		int[][] paladinTests = { 
			{ 895, 213, 255, 247, 147 }, 
			{ 816, 301, 125, 237, 255 }, 
			{ 767, 82, 133, 107, 168 } 
		};
		_testForClass("Paladin", paladinTests, false);

		// Priest Test
		int[][] priestTests = { 
			{ 724, 189, 255, 236, 101 }, 
			{ 796, 243, 58, 72, 138 }, 
			{ 882, 148, 27, 20, 38 } 
		};
		_testForClass("Priest", priestTests, false);

		// Rogue Test
		int[][] rogueTests = { 
			{ 889, 254, 132, 196, 72 }, 
			{ 790, 273, 88, 21, 34 }, 
			{ 841, 73, 100, 109, 183 } 
		};
		_testForClass("Rogue", rogueTests, false);

		// Shaman Test
		int[][] shamanTests = { 
			{ 748, 94, 5, 50, 100 }, 
			{ 887, 169, 234, 50, 32 }, 
			{ 733, 206, 186, 255, 255 } 
		};
		_testForClass("Shaman", shamanTests, false);

		// Warlock Test
		int[][] warlockTests = { 
			{ 711, 203, 127, 142, 36 }, 
			{ 832, 264, 240, 244, 252 }, 
			{ 832, 65, 98, 129, 0 } 
		};
		_testForClass("Warlock", warlockTests, false);

		// Warrior Test
		int[][] warriorTests = { 
			{ 795, 64, 37, 4, 0 }, 
			{ 780, 83, 167, 23, 4 }, 
			{ 809, 92, 255, 247, 227 } 
		};
		_testForClass("Warrior", warriorTests, false);
	}
	
	private void _testForPlayingScreen() {

		// check for normal play boards
		int[][] tests = { 
			{ 336, 203, 231, 198, 124 }, 
			{ 763, 440, 234, 198, 124 } 
		};
		PixelGroupTest normalPxTest = new PixelGroupTest(_image, tests);

		// check for lighter orc board
		int[][] orcBoardTests = { 
			{ 906, 283, 222, 158, 94 }, 
			{ 120, 468, 195, 134, 78 } 
		};
		PixelGroupTest orcPxTest = new PixelGroupTest(_image, orcBoardTests);

		if (normalPxTest.passed() || orcPxTest.passed()) {
			_setScreen("Playing");
		//	_analyzeOpponnentName();
		}
	}
	
	private void _testForPlayScreen() {

		int[][] tests = { 
			{ 543, 130, 121, 32, 22 }, // play mode red background
			{ 254, 33, 197, 173, 132 }, // mode title light brown background
			{ 956, 553, 24, 8, 8 }, 
			{ 489, 688, 68, 65, 63 } 
		};
		if((new PixelGroupTest(_image, tests)).passed())
			_setScreen("Play");
	}
	
	private void _testForPracticeScreen() {
		
		int[][] tests = { 
				{ 583, 120, 100, 99, 50 }, 	// practice mode green background
				{ 255, 628, 87, 91, 45 }, 	// practice mode green background
				{ 244, 28, 222, 199, 157 },	// section heading  
				{ 262, 695, 217, 193, 151 }	// bottom label 
		};
		if((new PixelGroupTest(_image, tests)).passed()) {
			_setScreen("Practice");
			_setMode("Practice");
		}
	}

	private void _testForRankedMode() {

		int[][] tests = { 
			{ 833, 88, 220, 255, 255 }, // ranked blue
			{ 698, 120, 56, 16, 8 } // casual off
		};
		PixelGroupTest testOne = new PixelGroupTest(_image, tests);
		
		int[][] testsTwo = { 
				{ 840, 184, 199, 255, 255 }, 
				{ 948, 167, 192, 255, 255 } 	
		};
		PixelGroupTest testTwo = new PixelGroupTest(_image, testsTwo);
		
		if(testOne.passed() || testTwo.passed())
			_setMode("Ranked");
	}
	
	private void _testForVictory() {

		int[][] tests = { 
			{ 334, 504, 88, 101, 192 }, 
			{ 683, 510, 74, 88, 173 }, 
			{ 549, 162, 255, 224, 119 } 
		};
		PixelGroupTest pxTest = new PixelGroupTest(_image, tests);

		int[][] testsTwo = { 
			{ 347, 469, 85, 102, 203 }, 
			{ 737, 339, 63, 76, 148 }, 
			{ 774, 214, 253, 243, 185 } 
		};
		PixelGroupTest pxTestTwo = new PixelGroupTest(_image, testsTwo);

		int[][] testsThree = { 
			{ 370, 528, 66, 81, 165 }, 
			{ 690, 553, 63, 76, 162 }, 
			{ 761, 228, 249, 234, 163 } 
		};
		PixelGroupTest pxTestThree = new PixelGroupTest(_image, testsThree);

		if(pxTest.passed() || pxTestTwo.passed() || pxTestThree.passed()) {
			_setScreen("Result");
			_setResult("Victory");
		}
	}
	
	private void _testForYourClass() {
		
		// Druid Test
		int[][] druidTests = { 
			{ 225, 480, 210, 255, 246 }, 
			{ 348, 510, 234, 255, 251 }, 
			{ 237, 607, 193, 155, 195 } 
		};
		_testForClass("Druid", druidTests, true);

		// Hunter Test
		int[][] hunterTests = { 
			{ 289, 438, 173, 161, 147 }, 
			{ 366, 554, 250, 200, 81 }, 
			{ 210, 675, 209, 209, 211 } 
		};
		_testForClass("Hunter", hunterTests, true);

		// Mage Test
		int[][] mageTests = { 
			{ 259, 439, 96, 31, 102 }, 
			{ 294, 677, 219, 210, 193 }, 
			{ 216, 591, 0, 0, 56 } 
		};
		_testForClass("Mage", mageTests, true);

		// Paladin Test
		int[][] paladinTests = { 
			{ 249, 447, 133, 105, 165 }, 
			{ 304, 671, 74, 146, 234 }, 
			{ 368, 581, 244, 238, 141 } 
		};
		_testForClass("Paladin", paladinTests, true);

		// Priest Test
		int[][] priestTests = { 
			{ 229, 491, 180, 178, 166 }, 
			{ 256, 602, 82, 104, 204 }, 
			{ 350, 611, 22, 23, 27 } 
		};
		_testForClass("Priest", priestTests, true);

		// Rogue Test
		int[][] rogueTests = { 
			{ 309, 446, 91, 107, 175 }, 
			{ 291, 468, 187, 37, 25 }, 
			{ 362, 623, 122, 186, 67 } 
		};
		_testForClass("Rogue", rogueTests, true);

		// Shaman Test
		int[][] shamanTests = { 
			{ 223, 458, 4, 46, 93 }, 
			{ 360, 533, 213, 32, 6 }, 
			{ 207, 578, 177, 245, 249 } 
		};
		_testForClass("Shaman", shamanTests, true);

		// Warlock Test
		int[][] warlockTests = { 
			{ 301, 435, 104, 138, 8 }, 
			{ 265, 493, 221, 51, 32 }, 
			{ 294, 680, 60, 75, 182 } 
		};
		_testForClass("Warlock", warlockTests, true);
		
		// Warrior Test
		int[][] warriorTests = { 
				{ 252, 452, 163, 10, 0 }, 
				{ 291, 579, 234, 192, 53 }, 
				{ 280, 461, 255, 245, 225 } 
		};
		_testForClass("Warrior", warriorTests, true);
	}
	

}