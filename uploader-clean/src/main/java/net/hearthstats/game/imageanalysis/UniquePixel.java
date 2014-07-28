package net.hearthstats.game.imageanalysis;

import java.util.Arrays;

/**
 * Defines pixel tests that aren't in the standard locations.
 * They are usually used to identify elements on specific screens,
 * rather than to identify a screen itself.
 */
public enum UniquePixel {

  OPPONENT_DRUID_1          (1161,  185, 175, 225, 212, 235, 255, 255),
  OPPONENT_DRUID_2          (1379,  221, 201, 225, 222, 255, 255, 255),
  OPPONENT_DRUID_3          (1197,  336, 173, 130, 168, 233, 190, 228),

  OPPONENT_DRUID_GOLDEN_1   (1161,  185,  50, 180, 220, 180, 255, 255),
  OPPONENT_DRUID_GOLDEN_2   (1379,  221, 160, 225, 240, 255, 255, 255),
  OPPONENT_DRUID_GOLDEN_3   (1197,  336, 173, 130, 168, 233, 190, 228),

  OPPONENT_HUNTER_1         (1290,  104, 143, 148, 153, 203, 208, 213),
  OPPONENT_HUNTER_2         (1279,  274, 111,   7,   0, 171,  67,  30),
  OPPONENT_HUNTER_3         (1155,  483, 186, 184, 181, 246, 244, 241),

  OPPONENT_HUNTER_GOLDEN_1  (1290,  104, 120, 130, 130, 230, 235, 235),
  OPPONENT_HUNTER_GOLDEN_2  (1279,  274,  85,  15,   0, 135,  55,  20),
  OPPONENT_HUNTER_GOLDEN_3  (1155,  483, 200, 184, 130, 255, 255, 190),

  OPPONENT_MAGE_1           (1235,  107,  58,   0,  69, 118,  53, 129),
  OPPONENT_MAGE_2           (1232,  488, 185, 158, 147, 245, 218, 207),
  OPPONENT_MAGE_3           (1282,  386,  23,  34,  52,  83,  94, 112),

  OPPONENT_MAGE_GOLDEN_1    (1235,  107,  58,   0,  69, 118,  53, 129),
  OPPONENT_MAGE_GOLDEN_2    (1232,  488, 185, 158, 147, 245, 218, 207),
  OPPONENT_MAGE_GOLDEN_3    (1282,  386,  23,  34,  52,  83,  94, 112),

  OPPONENT_PALADIN_1        (1399,  333, 225, 217, 117, 255, 255, 177),
  OPPONENT_PALADIN_2        (1275,  471,  95, 207, 225, 155, 255, 255),
  OPPONENT_PALADIN_3        (1199,  129, 103,  77, 138, 163, 137, 198),

  OPPONENT_PALADIN_GOLDEN_1 (1399,  333, 225, 217, 155, 255, 255, 200),
  OPPONENT_PALADIN_GOLDEN_2 (1275,  471,  95, 170, 225, 155, 220, 255),
  OPPONENT_PALADIN_GOLDEN_3 (1199,  129, 103,  77, 130, 163, 137, 195),

  OPPONENT_PRIEST_1         (1132,  296, 225, 206,  71, 255, 255, 131),
  OPPONENT_PRIEST_2         (1244,  380,  28,  42, 108,  88, 102, 168),
  OPPONENT_PRIEST_3         (1379,  232,   0,   0,   8,  57,  50,  68),

  OPPONENT_PRIEST_GOLDEN_1  (1132,  296, 235, 235, 230, 255, 255, 255),
  OPPONENT_PRIEST_GOLDEN_2  (1244,  380,  40,  50, 120,  90, 100, 170),
  OPPONENT_PRIEST_GOLDEN_3  (1379,  232,  25,   0,  35,  65,  50,  85),

  OPPONENT_ROGUE_1          (1390,  397, 102, 166,  42, 162, 226, 102),
  OPPONENT_ROGUE_2          (1235,  427,  58,   0,   4, 118,  51,  64),
  OPPONENT_ROGUE_3          (1315,  115,  70,  79, 153, 130, 139, 213),

  OPPONENT_ROGUE_GOLDEN_1   (1390,  397, 110, 200,  35, 200, 255,  75),
  OPPONENT_ROGUE_GOLDEN_2   (1235,  427,  55,  15,  25,  95,  55,  75),
  OPPONENT_ROGUE_GOLDEN_3   (1315,  115,  40,  50,  85, 110, 100, 160),

  OPPONENT_SHAMAN_1         (1169,  147,   0,  20,  70,  35,  80, 130),
  OPPONENT_SHAMAN_2         (1386,  265, 204,  20,   2, 255,  80,  62),
  OPPONENT_SHAMAN_3         (1146,  322, 156, 225, 225, 216, 255, 255),

  OPPONENT_SHAMAN_GOLDEN_1  (1169,  147,   5,  45, 120,  55, 105, 190),
  OPPONENT_SHAMAN_GOLDEN_2  (1386,  265, 220,  75,  65, 255, 125, 115),
  OPPONENT_SHAMAN_GOLDEN_3  (1146,  322, 155, 230, 230, 215, 255, 255),

  OPPONENT_WARLOCK_1        (1111,  318,  97, 112,   6, 157, 172,  66),
  OPPONENT_WARLOCK_2        (1300,  413, 210, 214, 222, 255, 255, 255),
  OPPONENT_WARLOCK_3        (1300,  102,  68,  99,   0, 128, 159,  25),

  OPPONENT_WARLOCK_GOLDEN_1 (1111,  318,  60,  90,   0, 230, 255,  25),
  OPPONENT_WARLOCK_GOLDEN_2 (1255,  484,  30,  40, 135,  70,  80, 175),
  OPPONENT_WARLOCK_GOLDEN_3 (1300,  112,  60,  90,   0, 250, 255,  25),

  OPPONENT_WARRIOR_1        (1243,  100,   7,   0,   0,  67,  34,  30),
  OPPONENT_WARRIOR_2        (1219,  130, 137,   0,   0, 197,  53,  34),
  OPPONENT_WARRIOR_3        (1265,  144, 225, 217, 197, 255, 255, 255),

  OPPONENT_WARRIOR_GOLDEN_1 (1125,  215, 160, 155, 115, 200, 195, 165),
  OPPONENT_WARRIOR_GOLDEN_2 (1219,  130, 220,   0,   0, 255, 240,  30),
  OPPONENT_WARRIOR_GOLDEN_3 (1265,  148, 210, 195, 155, 245, 235, 205),

  YOUR_DRUID_1              ( 352,  750, 180, 225, 216, 240, 255, 255),
  YOUR_DRUID_2              ( 544,  797, 204, 225, 221, 255, 255, 255),
  YOUR_DRUID_3              ( 311,  888, 145,  40, 165, 200, 100, 205),

  YOUR_DRUID_GOLDEN_1       ( 352,  750,  40, 170, 216, 110, 225, 255),
  YOUR_DRUID_GOLDEN_2       ( 544,  797, 204, 225, 221, 255, 255, 255),
  YOUR_DRUID_GOLDEN_3       ( 311,  888, 145,  40, 165, 200, 100, 205),

  YOUR_HUNTER_1             ( 365,  779,  10,  10,  10,  60,  60,  60),
  YOUR_HUNTER_2             ( 605, 1013,   0,   0,   0,  50,  50,  50),
  YOUR_HUNTER_3             ( 463,  680, 158, 158, 158, 190, 200, 210),

  YOUR_HUNTER_GOLDEN_1      ( 365,  779,  10,  10,  10,  60,  60,  60),
  YOUR_HUNTER_GOLDEN_2      ( 600, 1010,  53,  50,  40, 113, 110, 100),
  YOUR_HUNTER_GOLDEN_3      ( 463,  682, 145, 135, 135, 195, 190, 190),

  YOUR_MAGE_1               ( 405,  686,  66,   1,  72, 126,  61, 132),
  YOUR_MAGE_2               ( 457, 1059, 189, 180, 155, 249, 240, 200),
  YOUR_MAGE_3               ( 338,  924,   0,   0,  26,  30 , 30,  86),

  YOUR_MAGE_GOLDEN_1        ( 405,  686, 120,   1, 135, 165,  61, 175),
  YOUR_MAGE_GOLDEN_2        ( 457, 1059, 199, 190, 155, 242, 240, 200),
  YOUR_MAGE_GOLDEN_3        ( 338,  924,   0,   0,  26,  30 , 30,  86),

  YOUR_PALADIN_1            ( 390,  699, 103,  75, 135, 163, 135, 195),
  YOUR_PALADIN_2            ( 475, 1049,  44, 116, 204, 104, 176, 255),
  YOUR_PALADIN_3            ( 573,  906, 220, 215, 110, 255, 255, 180),

  YOUR_PRIEST_1             ( 358,  768, 150, 148, 136, 210, 208, 196),
  YOUR_PRIEST_2             ( 400,  941,  52,  74, 174, 112, 134, 234),
  YOUR_PRIEST_3             ( 547,  955,   0,   0,   0,  45,  45,  45),

  YOUR_ROGUE_1              ( 483,  697,  61,  77, 145, 121, 137, 205),
  YOUR_ROGUE_2              ( 442,  718, 110,   0,   0, 180,  40,  50),
  YOUR_ROGUE_3              ( 566,  974,  92, 156,  37, 152, 216,  97),

  YOUR_ROGUE_GOLDEN_1       ( 483,  697,  35,  40,  80,  75,  80, 130),
  YOUR_ROGUE_GOLDEN_2       ( 442,  718, 110,   0,   0, 180,  40,  50),
  YOUR_ROGUE_GOLDEN_3       ( 566,  974, 160, 230,  37, 255, 255,  72),

  YOUR_SHAMAN_1             ( 349,  716,   0,  16,  63,  34,  76, 123),
  YOUR_SHAMAN_2             ( 563,  833, 180,  10,   0, 250,  50,  20),
  YOUR_SHAMAN_3             ( 324,  904, 147, 215, 219, 207, 255, 255),

  YOUR_SHAMAN_GOLDEN_1      ( 349,  716,  30,  90, 180,  70, 130, 230),
  YOUR_SHAMAN_GOLDEN_2      ( 563,  833, 220,  40,  10, 255,  90,  60),
  YOUR_SHAMAN_GOLDEN_3      ( 324,  904, 177, 235, 235, 217, 255, 255),

  YOUR_WARLOCK_1            ( 471,  680,  74, 108,   0, 134, 168,  38),
  YOUR_WARLOCK_2            ( 414,  768, 180,  10,   0, 255,  80,  50),
  YOUR_WARLOCK_3            ( 460, 1063,  30,  45, 152,  90, 105, 212),

  YOUR_WARLOCK_GOLDEN_1     ( 471,  680, 135, 190,   0, 175, 230,  20),
  YOUR_WARLOCK_GOLDEN_2     ( 414,  768, 180,   1,   0, 255,  50,  50),
  YOUR_WARLOCK_GOLDEN_3     ( 460, 1063,  20,  35,  55,  70,  95, 105),

  YOUR_WARRIOR_1            ( 394,  707, 133,   0,   0, 193,  40,  30),
  YOUR_WARRIOR_2            ( 455,  905, 204, 162,  25, 255, 222,  90),
  YOUR_WARRIOR_3            ( 438,  721, 225, 215, 195, 255, 255, 255),

  YOUR_WARRIOR_GOLDEN_1     ( 394,  707, 210,  40,   0, 255, 180,  20),
  YOUR_WARRIOR_GOLDEN_2     ( 455,  905, 180, 135,   1, 230, 175,  40),
  YOUR_WARRIOR_GOLDEN_3     ( 438,  721, 215, 200, 185, 250, 245, 235),

  COIN_1              (1358,  608, 125, 220,  73, 185, 255, 133),
  COIN_2              (1350,  513, 100, 225,  55, 160, 255, 115),
  COIN_3              (1360,  532,  97, 225,  53, 157, 255, 113),
  COIN_4              (1202,  438,  95, 225,  52, 155, 255, 112),
  COIN_5              (1350,  593,  96, 225,  52, 156, 255, 112),

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

  DECK_SLOT_7A        ( 255,  775,  25, 115, 212,  63, 255, 255),
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

  NAME_OPPONENT_1A    ( 599,  182, 157, 117,  49, 217, 177, 109),
  NAME_OPPONENT_1B    ( 986,  168, 143, 100,  40, 203, 160, 100),
  NAME_OPPONENT_1C    ( 790,  930,  99, 169, 225, 159, 229, 255),

  NAME_OPPONENT_2A    ( 593,  185, 159, 116,  52, 219, 176, 112),
  NAME_OPPONENT_2B    (1002,  186, 167, 124,  56, 227, 184, 116),
  NAME_OPPONENT_2C    ( 805,  972,  91, 161, 225, 151, 221, 255),

  NEW_ARENA_RUN_A     ( 466,  266, 225, 209, 118, 255, 255, 178),
  NEW_ARENA_RUN_B     ( 769,  319,  98,  49,  17, 158, 109,  77),
  NEW_ARENA_RUN_C     ( 597,  460, 225, 225, 225, 255, 255, 255),
  NEW_ARENA_RUN_D     ( 615,  440, 225, 225, 225, 255, 255, 255),
  NEW_ARENA_RUN_E     ( 502,  624, 179, 149,  97, 239, 209, 157),

  VICTORY_1A          ( 522,  788,  58,  71, 162, 118, 131, 222),
  VICTORY_1B          (1068,  797,  44,  58, 143, 104, 118, 203),
  VICTORY_1C          ( 858,  254, 225, 194,  89, 255, 254, 149),

  VICTORY_2A          ( 543,  733,  55,  72, 173, 115, 132, 233),
  VICTORY_2B          (1152,  530,  33,  46, 118,  93, 106, 178),
  VICTORY_2C          (1210,  335, 223, 213, 155, 255, 255, 215),

  VICTORY_3A          ( 579,  825,  36,  51, 135,  96, 111, 195),
  VICTORY_3B          (1079,  865,  33,  46, 132,  93, 106, 192),
  VICTORY_3C          (1190,  357, 219, 204, 133, 255, 255, 193),

  DEFEAT_1A           (1165,  343, 134, 132, 125, 194, 192, 185),
  DEFEAT_1B           ( 538,  599, 104, 123, 209, 164, 183, 255),
  DEFEAT_1C           (1088,  558, 171, 167, 158, 231, 227, 218),

  DEFEAT_2A           ( 543,  597,  99, 118, 206, 159, 178, 255),
  DEFEAT_2B           ( 430,  540, 107, 108, 104, 167, 168, 164),
  DEFEAT_2C           ( 840,  243, 205, 196,  37, 255, 255,  97),

  DEFEAT_3A           ( 543,  597,  99, 118, 206, 159, 178, 255),
  DEFEAT_3B           ( 430,  540, 107, 108, 104, 167, 168, 164),
  DEFEAT_3C           ( 840,  243, 205, 196,  37, 255, 255,  97),

  TURN_OPPONENT_1A    (PixelLocation.I,  66,  61,  52, 129, 135, 136),
  TURN_OPPONENT_1B    (PixelLocation.J, 106,  93,  73, 176, 175, 168),

  TURN_OPPONENT_2A    (1444,  535, 104, 107, 111, 164, 167, 171),
  TURN_OPPONENT_2B    (1468,  563, 104, 107, 110, 164, 167, 170),

  TURN_OPPONENT_3A    (1457,  525,  76,  60,  50, 136, 120, 110),
  TURN_OPPONENT_3B    (1449,  561, 163,  88,  66, 223, 148, 126),

  TURN_YOUR_1A        (PixelLocation.I,  15, 180,   0,  76, 255,  37),
  TURN_YOUR_1B        (PixelLocation.J,  45, 170,  10, 100, 255,  70),

  TURN_YOUR_2A        (PixelLocation.I, 210, 180,   0, 255, 255,  10),
  TURN_YOUR_2B        (PixelLocation.J, 195, 160,  10, 255, 255,  80),

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
