package net.hearthstats.game.imageanalysis;

import static net.hearthstats.game.imageanalysis.PixelLocation.*;

/**
 * Represents the key pixels that identify particular screens in Hearthstone.
 */
public enum Pixel {

    // @formatter:off

    TITLE_C                 (C,  36,  21,  25,  56,  41,  45),
    TITLE_F                 (F,  52,  31,  14,  72,  51,  34),
    TITLE_I                 (I,  31,  18,  23,  51,  38,  43),
    TITLE_L                 (L,  23,  10,  14,  43,  30,  34),
    TITLE_N                 (A, 101,  32,   0, 128,  54,  13),
    TITLE_Q                 (Q, 169,  70,  10, 204,  98,  32),

    MAIN_A                  (A, 128,  58,  24, 162,  81,  45),
    MAIN_C                  (C,  44,  56,  77,  65,  77, 100),
    MAIN_D                  (D, 111,  76,  34, 166, 116,  60),
    MAIN_E                  (E,  20,   6,   0,  80,  37,  30),
    MAIN_M                  (M,   4, 104, 144,  36, 135, 176),
    MAIN_Q                  (Q,  16,   7,   4,  41,  29,  25),
    MAIN_R                  (R,  15,  24,  44,  39,  47,  67),

    MAIN_TODAYSQUESTS_A     (A,  60,  24,   7,  84,  45,  27),
    MAIN_TODAYSQUESTS_C     (C,  10,  15,  22,  30,  35,  42),
    MAIN_TODAYSQUESTS_F     (F, 112,  80,  46, 132, 100,  66),
    MAIN_TODAYSQUESTS_M     (M,  38,  65,  54,  58,  85,  74),
    MAIN_TODAYSQUESTS_N     (N,  16,   8,   0,  37,  29,  20),
    MAIN_TODAYSQUESTS_Q     (Q,   5,   0,   0,  28,  20,  18),

    COLLECTION_A            (A,  86,  14,  12, 217, 195, 124),
    COLLECTION_C            (C,  10,   0,   0,  30,  20,  17),
    COLLECTION_E            (E, 217, 203, 154, 259, 234, 183),
    COLLECTION_I            (I,  22,  14,  29,  42,  34,  49),
    COLLECTION_M            (M,  58,  22,  19,  78,  42,  39),
    COLLECTION_Q            (Q,  60,  50,  76,  80,  70,  96),

    COLLECTION_ZOOM_A       (A,  39,   3,   1,  59,  23,  21),
    COLLECTION_ZOOM_C       (C,   0,   0,   0,  19,  15,  13),
    COLLECTION_ZOOM_I       (I, 161, 138,  87, 181, 158, 107),
    COLLECTION_ZOOM_M       (M,   8,   0,   0,  28,  17,  16),
    COLLECTION_ZOOM_O       (O, 245, 226, 212, 255, 246, 232),
    COLLECTION_ZOOM_Q       (Q,  24,  20,  30,  44,  40,  50),

    ARENA_CHOOSE_C          (C,  10,   0,   0,  30,  20,  17),
    ARENA_CHOOSE_E          (E,  63,  53,  62,  87,  76,  85),
    ARENA_CHOOSE_F          (F, 174, 151,  94, 218, 265, 265),
    ARENA_CHOOSE_G          (G,  82,  79,  59, 214, 188, 162),
    ARENA_CHOOSE_L          (L,  47,  41,  38,  67,  61,  58),
    ARENA_CHOOSE_P          (P,  93,  89,  93, 113, 109, 113),
    ARENA_CHOOSE_Q          (Q,  60,  50,  76,  80,  70,  96),

    ARENA_END_C             (C,   0,   0,   0,   5,   5,   5),
    ARENA_END_E             (E,  98,  73,  31, 118,  93,  51),
    ARENA_END_F             (F,  85,  38, 111, 105,  58, 131),
    ARENA_END_G             (G, 196, 172, 122, 216, 192, 142),
    ARENA_END_L             (L,  25,  20,   8,  45,  40,  28),
    ARENA_END_P             (P,  77,  73,  77,  97,  93,  97),
    ARENA_END_Q             (Q,  10,   7,  14,  30,  27,  34),

    ARENA_LOBBY_C           (C,  10,   0,   0,  30,  20,  17),
    ARENA_LOBBY_E           (E, 171, 129,  58, 191, 149,  78),
    ARENA_LOBBY_F           (F, 118,  55, 153, 138,  75, 173),
    ARENA_LOBBY_G           (G, 196, 172, 122, 262, 203, 142),
    ARENA_LOBBY_L           (L, 212, 181, 108, 232, 201, 128),
    ARENA_LOBBY_P           (P,  64,  66,  64, 122, 255, 255),
    ARENA_LOBBY_Q           (Q,  60,  50,  76,  80,  70,  96),

    PLAY_LOBBY_C            (C,   0,   0,   0,  10,  10,  10),
    PLAY_LOBBY_E            (E,  56,   9,   3,  76,  29,  23),
    PLAY_LOBBY_F            (F, 117,  24,  10, 214,  84,  58),
    PLAY_LOBBY_H            (H,  57,  12,   6, 109,  38,  34),
    PLAY_LOBBY_I            (I,  33,   3,   3,  96,  75,  47),
    PLAY_LOBBY_P            (P, 102,  70,  40, 122,  90,  60),
    PLAY_LOBBY_Q            (Q,  97,  71,  47, 117,  91,  67),

    PRACTICE_LOBBY_C        (C,   0,   0,   0,  10,  10,  10),
    PRACTICE_LOBBY_E        (E,  43,  46,  12,  67,  68,  37),
    PRACTICE_LOBBY_F        (F,  85,  88,  36, 184, 182, 121),
    PRACTICE_LOBBY_H        (H,  50,  51,  19,  95,  97,  55),
    PRACTICE_LOBBY_I        (I,  35,  40,  12,  55,  60,  32),
    PRACTICE_LOBBY_P        (P, 102,  70,  40, 122,  90,  60),
    PRACTICE_LOBBY_Q        (Q,  97,  71,  47, 117,  91,  67),

    VERSUS_LOBBY_C        (C,   0,   0,   0,  10,  10,  10),
    VERSUS_LOBBY_E        (E,  25,  38,  54,  45,  58,  74),
    VERSUS_LOBBY_F        (F,  59,  85, 106,  79, 105, 126),
    VERSUS_LOBBY_H        (H,  23,  43,  55,  45,  65,  78),
    VERSUS_LOBBY_I        (I,  14,  18,  39,  35,  38,  59),
    VERSUS_LOBBY_P        (P, 102, 183, 100, 125, 205, 121),
    VERSUS_LOBBY_Q        (Q,  95,  69,  45, 117,  91,  67),

    FINDING_OPPONENT_C      (C,   0,   0,   0,  26,  25,  24),
    FINDING_OPPONENT_E      (E,  59,  53,  62,  83,  77,  86),
    FINDING_OPPONENT_F      (F, 176, 142, 117, 208, 173, 145),
    FINDING_OPPONENT_G      (G,  94,   0,   0, 230, 100,  85),
    FINDING_OPPONENT_N      (N,  16,  17,  15,  36,  37,  35),
    FINDING_OPPONENT_R      (R,   1,   1,   1,  29,  28,  27),

    MATCH_VS_C              (C,  10,   0,   0,  32,  28,  24),
    MATCH_VS_E              (E,  12,   0,   0,  36,  22,  24),
    MATCH_VS_F              (F,  17,   0,   0,  41,  22,  24),
    MATCH_VS_G              (G, 176,  33,  14, 200,  56,  37),
    MATCH_VS_H              (H, 168,  27,  14, 191,  49,  37),
    MATCH_VS_Q              (Q,  10,  13,  19,  38,  48,  71),
    MATCH_VS_R              (R,  15,  12,   0,  38,  42,  24),

    MATCH_STARTINGHAND_C    (C,  10,   3,   0,  32,  28,  24),
    MATCH_STARTINGHAND_E    (E, 132,  93,  48, 152, 113,  68),
    MATCH_STARTINGHAND_O    (O, 116, 186, 245, 140, 207, 255),
    MATCH_STARTINGHAND_Q    (Q,  10,  13,  19,  38,  48,  71),
    MATCH_STARTINGHAND_R    (R,  15,  13,   0,  38,  42,  25),

    MATCH_ORGRIMMAR_B       (B, 136,  39,  24, 160,  60,  46),
    MATCH_ORGRIMMAR_C       (C, 122,  54,  15, 142,  77,  39),
    MATCH_ORGRIMMAR_D       (D, 117,  54,  21, 140,  75,  44),
    MATCH_ORGRIMMAR_E       (E,  88,  26,  12, 167,  55,  38),
    MATCH_ORGRIMMAR_R       (R, 128,  76,  16, 198, 149,  45),
    MATCH_ORGRIMMAR_L       (L,  70,   5,   0,  99,  27,  17),
    MATCH_ORGRIMMAR_K       (K,  99,   0,   0, 136,  22,  14),

    MATCH_PANDARIA_B        (B,  88,  48,  33, 122,  74,  53),
    MATCH_PANDARIA_C        (C, 130,  75,  24, 167, 141, 109),
    MATCH_PANDARIA_D        (D, 128,  73,  32, 167, 154, 135),
    MATCH_PANDARIA_E        (E, 152,  39,  24, 185, 129, 119),
    MATCH_PANDARIA_K        (K, 245, 245, 210, 255, 255, 236),
    MATCH_PANDARIA_L        (L, 131,  78,  40, 198, 127,  71),
    MATCH_PANDARIA_R        (R, 146, 120,  24, 190, 175, 109),

    MATCH_STORMWIND_B       (B,  38,  30,  78,  58,  50,  98),
    MATCH_STORMWIND_C       (C, 119,  68,  27, 139,  89,  49),
    MATCH_STORMWIND_D       (D, 116,  66,  36, 137,  86,  58),
    MATCH_STORMWIND_E       (E, 139,  36,  28, 163,  58,  49),
    MATCH_STORMWIND_K       (K,  66, 108, 136,  90, 133, 161),
    MATCH_STORMWIND_L       (L,  86, 139, 175, 110, 164, 200),
    MATCH_STORMWIND_R       (R, 135, 109,  28, 171, 147,  52),

    MATCH_STRANGLETHORN_B   (B,  33,  45,  33,  57,  65,  57),
    MATCH_STRANGLETHORN_C   (C, 133,  76,  25, 153,  96,  45),
    MATCH_STRANGLETHORN_D   (D, 131,  73,  31, 151,  93,  51),
    MATCH_STRANGLETHORN_E   (E, 158,  42,  24, 178,  62,  44),
    MATCH_STRANGLETHORN_K   (K, 150, 115,  45, 203, 144,  75),
    MATCH_STRANGLETHORN_L   (L, 191, 156,  63, 211, 176,  83),
    MATCH_STRANGLETHORN_R   (R, 168, 139,  28, 188, 159,  48),

    MATCH_ORGRIMMAR_END_B   (B,  67,  56,  54, 106,  96,  95),
    MATCH_ORGRIMMAR_END_C   (C,  71,  63,  58, 104,  96,  92),
    MATCH_ORGRIMMAR_END_D   (D,  71,  64,  60, 107, 101,  99),
    MATCH_ORGRIMMAR_END_E   (E,  59,  50,  47, 135, 120, 125),
    MATCH_ORGRIMMAR_END_K   (K,  39,  26,  25,  85,  70,  65),
    MATCH_ORGRIMMAR_END_L   (L,  28,  20,  19,  65,  55,  55),
    MATCH_ORGRIMMAR_END_R   (R,  97,  91,  82, 157, 149, 136),

    MATCH_PANDARIA_END_B    (B,  64,  58,  56,  98,  91,  87),
    MATCH_PANDARIA_END_C    (C,  89,  82,  76, 153, 151, 148),
    MATCH_PANDARIA_END_D    (D,  97,  91,  86, 171, 170, 167),
    MATCH_PANDARIA_END_E    (E,  76,  63,  61, 155, 150, 145),
    MATCH_PANDARIA_END_K    (K, 214, 213, 209, 255, 255, 250),
    MATCH_PANDARIA_END_L    (L,  94,  89,  84, 130, 125, 120),
    MATCH_PANDARIA_END_R    (R, 120, 117, 106, 170, 169, 160),

    MATCH_STORMWIND_END_B   (B,  38,  37,  43,  82,  79,  87),
    MATCH_STORMWIND_END_C   (C,  83,  76,  71, 112, 106, 104),
    MATCH_STORMWIND_END_D   (D,  83,  76,  73, 113, 108, 107),
    MATCH_STORMWIND_END_E   (E,  74,  62,  61, 185, 180, 195),
    MATCH_STORMWIND_END_K   (K,  92,  97, 100, 130, 130, 130),
    MATCH_STORMWIND_END_L   (L, 121, 128, 132, 150, 160, 165),
    MATCH_STORMWIND_END_R   (R,  96,  93,  84, 133, 130, 120),

    MATCH_STRANGLETHORN_END_B(B, 35,  36,  35,  55,  57,  55),
    MATCH_STRANGLETHORN_END_C(C, 91,  84,  77, 111, 104, 100),
    MATCH_STRANGLETHORN_END_D(D, 84,  78,  73, 109, 103,  97),
    MATCH_STRANGLETHORN_END_E(E, 81,  68,  66, 110,  95,  90),
    MATCH_STRANGLETHORN_END_K(K, 99,  96,  90, 145, 140, 130),
    MATCH_STRANGLETHORN_END_L(L,115, 115, 105, 170, 167, 160),
    MATCH_STRANGLETHORN_END_R(R,122, 118, 105, 145, 141, 129),

    ;

    // @formatter:on

    public final PixelLocation pixelLocation;
    public final int minRed;
    public final int minGreen;
    public final int minBlue;
    public final int maxRed;
    public final int maxGreen;
    public final int maxBlue;

    Pixel(PixelLocation pixelLocation, int minRed, int minGreen, int minBlue, int maxRed, int maxGreen, int maxBlue) {
        this.pixelLocation = pixelLocation;
        this.minRed = minRed;
        this.minGreen = minGreen;
        this.minBlue = minBlue;
        this.maxRed = maxRed;
        this.maxGreen = maxGreen;
        this.maxBlue = maxBlue;
    }
}
