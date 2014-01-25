package net.hearthstats;

import java.awt.image.BufferedImage;
import java.util.Observable;

public class HearthstoneAnalyzer extends Observable {

	private boolean _coin;
	private BufferedImage _image;
	private String _mode;
	private String _opponentClass;
	private String _result;
	private String _screen;
	private String _yourClass;
	private int _deckSlot;

	public HearthstoneAnalyzer() {

	}

	public void analyze(BufferedImage image) {
		
		_image = image;

		if(getScreen() != "Main Menu") {
			_testForMainMenuScreen();
		}

		if(getScreen() != "Play") {
			_testForPlayScreen();
		}
		
		if(getScreen() != "Arena") {
			_testForArenaModeScreen();
		}
		
		if(getScreen() == "Play" || getScreen() == "Arena") {
			_testForFindingOpponent();
			if (getScreen() == "Play") {
				if(getMode() != "Casual")
					_testForCasualMode();
				if(getMode() != "Ranked")
					_testForRankedMode();
				_testForDeckSlot();
			}
		}
		
		if(getScreen() == "Finding Opponent") {
			_testForMatchStartScreen();
		}
		
		if(getScreen() == "Match Start") {
			if(getYourClass() == null)
				_testForYourClass();
			if(getOpponentClass() == null)
				_testForOpponentClass();
			if(!getCoin())
				_testForCoin();
			
			_testForPlayingScreen();
		}
		
		if(getScreen() == "Playing") {
			_testForVictory();
			_testForDefeat();
		}
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
	
	public String getResult() {
		return _result;
	}
	
	public String getScreen() {
		return _screen;
	}
	
	public String getYourClass() {
		return _yourClass;
	}

	private void _notifyObserversOfChangeTo(String property) {
		setChanged();
	    notifyObservers(property);
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

		if((new PixelGroupTest(_image, tests)).passed())
			_setMode("Casual");
	}
	
	private void _testForCoin() {

		int[][] tests = { 
			{ 869, 389, 155, 250, 103 } // fourth card right edge
		};
		if((new PixelGroupTest(_image, tests)).passed())
			_setCoin(true);
	}
	
	private void _testForDeckSlot() {

		if(getDeckSlot() != 1) {
			int[][] slotOnePixels = { 
				{ 146, 161, 45, 150, 247 } // bottom bar
			};
			if((new PixelGroupTest(_image, slotOnePixels)).passed())
				_setDeckSlot(1);
		}

		if(getDeckSlot() != 2) {
			int[][] slotTwoPixels = { 
				{ 348, 160, 44, 142, 247 } // bottom bar
			};
			if((new PixelGroupTest(_image, slotTwoPixels)).passed())
				_setDeckSlot(2);
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
			_setScreen("Finding Opponent");
		}
	}
	
	private void _testForMainMenuScreen() {

		int[][] tests = { 
			{ 338, 453, 159, 96, 42 }, // box top
			{ 211, 658, 228, 211, 116 } // quest button exclamation mark
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

		if (normalPxTest.passed() || orcPxTest.passed())
			_setScreen("Playing");
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
	

	private void _testForRankedMode() {

		int[][] tests = { 
			{ 833, 88, 220, 255, 255 }, // ranked blue
			{ 698, 120, 56, 16, 8 } // casual off
		};
		if((new PixelGroupTest(_image, tests)).passed())
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
	}

}