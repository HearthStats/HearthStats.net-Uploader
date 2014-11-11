package net.hearthstats.game.imageanalysis;


/**
 * Defines pixel tests that aren't in the standard locations.
 * They are usually used to identify elements on specific screens,
 * rather than to identify a screen itself.
 */
public enum UniquePixel {

  DECK_SLOT_1A        ( 255,  281,  25, 115, 212,  63, 255, 255),
  DECK_SLOT_1B        ( 286,  281,  25, 115, 212,  63, 255, 255),

  DECK_SLOT_2A        ( 544,  281,  25, 115, 212,  66, 255, 255),
  DECK_SLOT_2B        ( 575,  281,  25, 115, 212,  66, 255, 255),

  DECK_SLOT_3A        ( 791,  281,  25, 115, 212,  66, 255, 255),
  DECK_SLOT_3B        ( 822,  281,  25, 115, 212,  66, 255, 255),

  DECK_SLOT_4A        ( 255,  527,  25, 115, 212,  63, 255, 255),
  DECK_SLOT_4B        ( 286,  527,  25, 115, 212,  63, 255, 255),

  DECK_SLOT_5A        ( 544,  527,  25, 115, 212,  66, 255, 255),
  DECK_SLOT_5B        ( 575,  527,  25, 115, 212,  66, 255, 255),

  DECK_SLOT_6A        ( 791,  527,  25, 115, 212,  66, 255, 255),
  DECK_SLOT_6B        ( 822,  527,  25, 115, 212,  66, 255, 255),

  DECK_SLOT_7A(255, 775, 25, 108, 212, 63, 255, 255),
  DECK_SLOT_7B        ( 286,  775,  25, 115, 212,  63, 255, 255),

  DECK_SLOT_8A        ( 544,  775,  25, 115, 212,  66, 255, 255),
  DECK_SLOT_8B        ( 575,  775,  25, 115, 212,  66, 255, 255),

  DECK_SLOT_9A        ( 791,  775,  25, 115, 212,  66, 255, 255),
  DECK_SLOT_9B        ( 822,  775,  25, 115, 212,  66, 255, 255),

  MODE_CASUAL_1A      (1088,  150,  60, 200, 230, 220, 255, 255),
  MODE_CASUAL_1B      (1088,  250,  60, 200, 230, 220, 255, 255),

  MODE_CASUAL_2A      (1093,  120,  60, 200, 230, 220, 255, 255),
  MODE_CASUAL_2B      (1093,  200,  60, 200, 230, 220, 255, 255),

  MODE_CASUAL_3A      (1099,  290,  60, 200, 230, 220, 255, 255),
  MODE_CASUAL_3B      (1266,  290,  60, 200, 230, 220, 255, 255),

  MODE_RANKED_1A      (1298,  150,  60, 200, 230, 220, 255, 255),
  MODE_RANKED_1B      (1298,  250,  60, 200, 230, 220, 255, 255),

  MODE_RANKED_2A      (1303,  120,  60, 200, 230, 220, 255, 255),
  MODE_RANKED_2B      (1303,  200,  60, 200, 230, 220, 255, 255),

  MODE_RANKED_3A      (1309,  290,  60, 200, 230, 220, 255, 255),
  MODE_RANKED_3B      (1476,  290,  60, 200, 230, 220, 255, 255),

  NEW_ARENA_RUN_A     ( 466,  266, 225, 209, 118, 255, 255, 178),
  NEW_ARENA_RUN_B     ( 769,  319,  98,  49,  17, 158, 109,  77),
  NEW_ARENA_RUN_C     ( 597,  460, 225, 225, 225, 255, 255, 255),
  NEW_ARENA_RUN_D     ( 615,  440, 225, 225, 225, 255, 255, 255),
  NEW_ARENA_RUN_E     ( 502,  624, 179, 149,  97, 239, 209, 157),


  BACKGROUND_PLAY_1   ( 260,  434,   0,   0,   0,  30,  30,  30),
  BACKGROUND_PLAY_2   ( 526,  434,   0,   0,   0,  30,  30,  30),
  BACKGROUND_PLAY_3   ( 790,  434,   0,   0,   0,  30,  30,  30),
  BACKGROUND_PLAY_4   ( 260,  680,   0,   0,   0,  40,  40,  40),
  BACKGROUND_PLAY_5   ( 526,  680,   0,   0,   0,  30,  30,  30),
  BACKGROUND_PLAY_6   ( 790,  680,   0,   0,   0,  30,  30,  30),
  BACKGROUND_PLAY_7   ( 260,  927,   0,   0,   0,  30,  30,  30),
  BACKGROUND_PLAY_8   ( 526,  927,   0,   0,   0,  30,  30,  30),
  BACKGROUND_PLAY_9   ( 790,  927,   0,   0,   0,  30,  30,  30),

  // The top-left (tl) and bottom-right (br) points to look for the reference pixel for victory or defeat
  VICTORY_DEFEAT_REFBOX_TL ( 418, 218, 110, 80, 35, 200, 140, 60 ),
  VICTORY_DEFEAT_REFBOX_BR ( 488, 328, 170, 130, 50, 230, 190, 100 ),

  // At least one of 1A or 1B must match for a victory. Coordinates are relative to the reference pixel above
  VICTORY_REL_1A ( 740, 5, 110, 80, 35, 230, 190, 90 ),  // Gold
  VICTORY_REL_1B ( 745, -5, 110, 80, 35, 230, 190, 90 ),  // Gold

  // All of 2A, 2B & 2C must match for a victory. Coordinates are relative to the reference pixel above
  VICTORY_REL_2A ( 765, 50, 120, 90, 30, 235, 195, 90 ),  // Gold
  VICTORY_REL_2B ( 680, 305, 40, 50, 90, 70, 80, 180),  // Dark blue
  VICTORY_REL_2C (  43, 255, 40, 50, 90, 90, 110, 210),  // Dark blue

  // At least one of 1A-1E must match for a defeat. Coordinates are relative to the reference pixel above
  DEFEAT_REL_1A ( 760, 120, 60, 40, 20, 115, 95, 55 ),  // Dark gold
  DEFEAT_REL_1B ( 766, 129, 60, 40, 20, 115, 95, 55 ),  // Dark gold
  DEFEAT_REL_1C ( 773, 138, 60, 40, 20, 115, 95, 55 ),  // Dark gold
  DEFEAT_REL_1D ( 779, 147, 60, 40, 20, 115, 95, 55 ),  // Dark gold
  DEFEAT_REL_1E ( 786, 156, 60, 40, 20, 115, 95, 55 ),  // Dark gold

  // 2A must match for a defeat. Coordinates are relative to the reference pixel above
  DEFEAT_REL_2A ( 120, 315, 60, 70, 140, 140, 160, 250 ),  // blue

  // Hero classes on the deck screen
  DECK_DRUID_1(1269, 29, 86, 159, 146, 173, 234, 221), DECK_DRUID_2(1424, 57, 0, 135, 34, 67, 230,
      237),

  DECK_HUNTER_1(1269, 29, 81, 66, 52, 144, 132, 120), DECK_HUNTER_2(1424, 57, 27, 26, 27, 90, 85,
      82),

  DECK_MAGE_1(1269, 29, 21, 0, 30, 67, 36, 77), DECK_MAGE_2(1424, 57, 4, 43, 102, 50, 95, 158),

  DECK_PALADIN_1(1269, 29, 100, 85, 138, 161, 146, 209), DECK_PALADIN_2(1424, 57, 16, 0, 0, 63, 41,
      33),

  DECK_PRIEST_1(1269, 29, 105, 97, 85, 173, 174, 159), DECK_PRIEST_2(1424, 57, 145, 140, 143, 205,
      201, 192),

  DECK_ROGUE_1(1269, 29, 39, 40, 89, 90, 92, 152), DECK_ROGUE_2(1424, 57, 15, 0, 0, 88, 54, 42),

  DECK_SHAMAN_1(1269, 29, 0, 26, 58, 28, 75, 115), DECK_SHAMAN_2(1424, 57, 9, 0, 0, 64, 46, 39),

  DECK_WARLOCK_1(1269, 29, 16, 0, 0, 64, 32, 42), DECK_WARLOCK_2(1424, 57, 19, 0, 28, 85, 41, 95),

  DECK_WARRIOR_1(1269, 29, 155, 135, 106, 226, 209, 184), DECK_WARRIOR_2(1424, 57, 45, 0, 0, 145,
      60, 22),
  ;

  public final int x;
  public final int y;
  public final int minRed;
  public final int minGreen;
  public final int minBlue;
  public final int maxRed;
  public final int maxGreen;
  public final int maxBlue;

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

  UniquePixel(int x, int y, int minRed, int minGreen, int minBlue, int maxRed, int maxGreen, int maxBlue) {
    this.x = x;
    this.y = y;
    this.minRed = minRed;
    this.minGreen = minGreen;
    this.minBlue = minBlue;
    this.maxRed = maxRed;
    this.maxGreen = maxGreen;
    this.maxBlue = maxBlue;
  }

  UniquePixel(PixelLocation pixelLocation, int minRed, int minGreen, int minBlue, int maxRed, int maxGreen, int maxBlue) {
    this.x = pixelLocation.x;
    this.y = pixelLocation.y;
    this.minRed = minRed;
    this.minGreen = minGreen;
    this.minBlue = minBlue;
    this.maxRed = maxRed;
    this.maxGreen = maxGreen;
    this.maxBlue = maxBlue;
  }
  
  public static UniquePixel[] allBackgroundPlay() {
    return new UniquePixel[]{
        BACKGROUND_PLAY_1,
        BACKGROUND_PLAY_2,
        BACKGROUND_PLAY_3,
        BACKGROUND_PLAY_4,
        BACKGROUND_PLAY_5,
        BACKGROUND_PLAY_6,
        BACKGROUND_PLAY_7,
        BACKGROUND_PLAY_8,
        BACKGROUND_PLAY_9,
    };
  }
}
