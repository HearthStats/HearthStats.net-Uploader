package net.hearthstats.game;

import java.util.EnumSet;

import net.hearthstats.game.imageanalysis.Pixel;

/**
 * Represents the screens in Hearthstone.
 */
public enum Screen {

    TITLE (
            "Title Screen",
            ScreenGroup.GENERAL,
            EnumSet.of(
                    Pixel.TITLE_F,
                    Pixel.TITLE_L,
                    Pixel.TITLE_N,
                    Pixel.TITLE_Q
            ),
            EnumSet.of(
                    Pixel.TITLE_C,
                    Pixel.TITLE_I
            )),

    MAIN (
            "Main Menu",
            ScreenGroup.GENERAL,
            EnumSet.of(
                    Pixel.MAIN_C,
                    Pixel.MAIN_D,
                    Pixel.MAIN_E,
                    Pixel.MAIN_M
            ),
            EnumSet.of(
                    Pixel.MAIN_A,
                    Pixel.MAIN_Q,
                    Pixel.MAIN_R
            )),

    MAIN_TODAYSQUESTS (
            "Today's Quests",
            ScreenGroup.GENERAL,
            EnumSet.of(
                    Pixel.MAIN_TODAYSQUESTS_A,
                    Pixel.MAIN_TODAYSQUESTS_F,
                    Pixel.MAIN_TODAYSQUESTS_M
            ),
            EnumSet.of(
                    Pixel.MAIN_TODAYSQUESTS_C,
                    Pixel.MAIN_TODAYSQUESTS_N,
                    Pixel.MAIN_TODAYSQUESTS_Q
            )),

    COLLECTION (
            "Collection",
            ScreenGroup.GENERAL,
            EnumSet.of(
                    Pixel.COLLECTION_A,
                    Pixel.COLLECTION_B,
                    Pixel.COLLECTION_E,
                    Pixel.COLLECTION_M,
                    Pixel.COLLECTION_Q
            ),
            EnumSet.of(
                    Pixel.COLLECTION_C,
                    Pixel.COLLECTION_I
            )),

    COLLECTION_ZOOM (
            "Collection Card Zoom",
            ScreenGroup.GENERAL,
            EnumSet.of(
                    Pixel.COLLECTION_ZOOM_A,
                    Pixel.COLLECTION_ZOOM_O,
                    Pixel.COLLECTION_ZOOM_M,
                    Pixel.COLLECTION_ZOOM_Q
            ),
            EnumSet.of(
                    Pixel.COLLECTION_ZOOM_C,
                    Pixel.COLLECTION_ZOOM_I
            )),

    COLLECTION_DECK (
            "Collection Deck",
            ScreenGroup.GENERAL,
            EnumSet.of(
                    Pixel.COLLECTION_DECK_A,
                    Pixel.COLLECTION_DECK_B,
                    Pixel.COLLECTION_DECK_E,
                    Pixel.COLLECTION_DECK_M,
                    Pixel.COLLECTION_DECK_Q
            ),
            EnumSet.of(
                    Pixel.COLLECTION_DECK_C,
                    Pixel.COLLECTION_DECK_I
            )),

    ARENA_CHOOSE (
            "Arena Lobby",
            ScreenGroup.ARENA,
            EnumSet.of(
                    Pixel.ARENA_CHOOSE_E,
                    Pixel.ARENA_CHOOSE_F,
                    Pixel.ARENA_CHOOSE_L,
                    Pixel.ARENA_CHOOSE_P
            ),
            EnumSet.of(
                    Pixel.ARENA_CHOOSE_C,
                    Pixel.ARENA_CHOOSE_G,
                    Pixel.ARENA_CHOOSE_Q
            )),

    ARENA_END (
            "Arena Lobby",
            ScreenGroup.ARENA,
            EnumSet.of(
                    Pixel.ARENA_END_E,
                    Pixel.ARENA_END_F,
                    Pixel.ARENA_END_G,
                    Pixel.ARENA_END_L,
                    Pixel.ARENA_END_P
            ),
            EnumSet.of(
                    Pixel.ARENA_END_C,
                    Pixel.ARENA_END_Q
            )),

    ARENA_LOBBY (
            "Arena Lobby",
            ScreenGroup.ARENA,
            EnumSet.of(
                    Pixel.ARENA_LOBBY_E,
                    Pixel.ARENA_LOBBY_F,
                    Pixel.ARENA_LOBBY_L,
                    Pixel.ARENA_LOBBY_P
            ),
            EnumSet.of(
                    Pixel.ARENA_LOBBY_C,
                    Pixel.ARENA_LOBBY_G,
                    Pixel.ARENA_LOBBY_Q
            )),

    PLAY_LOBBY (
            "Play Lobby",
            ScreenGroup.PLAY,
            EnumSet.of(
                    Pixel.PLAY_LOBBY_E,
                    Pixel.PLAY_LOBBY_F,
                    Pixel.PLAY_LOBBY_H,
                    Pixel.PLAY_LOBBY_P
            ),
            EnumSet.of(
                    Pixel.PLAY_LOBBY_C,
                    Pixel.PLAY_LOBBY_I,
                    Pixel.PLAY_LOBBY_Q
            )),

    PRACTICE_LOBBY (
            "Practice Lobby",
            ScreenGroup.PRACTICE,
            EnumSet.of(
                    Pixel.PRACTICE_LOBBY_E,
                    Pixel.PRACTICE_LOBBY_F,
                    Pixel.PRACTICE_LOBBY_H,
                    Pixel.PRACTICE_LOBBY_P
            ),
            EnumSet.of(
                    Pixel.PRACTICE_LOBBY_C,
                    Pixel.PRACTICE_LOBBY_I,
                    Pixel.PRACTICE_LOBBY_Q
            )),

    VERSUS_LOBBY (
         "Versus Lobby",
         ScreenGroup.PLAY,
         EnumSet.of(
             Pixel.VERSUS_LOBBY_E,
             Pixel.VERSUS_LOBBY_F,
             Pixel.VERSUS_LOBBY_H,
             Pixel.VERSUS_LOBBY_P
          ),
          EnumSet.of(
                 Pixel.VERSUS_LOBBY_C,
                 Pixel.VERSUS_LOBBY_I,
                 Pixel.VERSUS_LOBBY_Q
          )),

    FINDING_OPPONENT (
            "Finding Opponent",
            ScreenGroup.GENERAL,
            EnumSet.of(
                    Pixel.FINDING_OPPONENT_E,
                    Pixel.FINDING_OPPONENT_F,
                    Pixel.FINDING_OPPONENT_G,
                    Pixel.FINDING_OPPONENT_N
            ),
            EnumSet.of(
                    Pixel.FINDING_OPPONENT_C,
                    Pixel.FINDING_OPPONENT_R
            )),

    MATCH_VS (
            "Match Start",
            ScreenGroup.MATCH_START,
            EnumSet.of(
                    Pixel.MATCH_VS_E,
                    Pixel.MATCH_VS_F,
                    Pixel.MATCH_VS_G,
                    Pixel.MATCH_VS_H
            ),
            EnumSet.of(
                    Pixel.MATCH_VS_C,
                    Pixel.MATCH_VS_Q,
                    Pixel.MATCH_VS_R
            )),

    MATCH_STARTINGHAND (
            "Starting Hand",
            ScreenGroup.MATCH_START,
            EnumSet.of(
                    Pixel.MATCH_STARTINGHAND_E,
                    Pixel.MATCH_STARTINGHAND_O,
                    Pixel.MATCH_STARTINGHAND_Q
            ),
            EnumSet.of(
                    Pixel.MATCH_STARTINGHAND_C,
                    Pixel.MATCH_STARTINGHAND_R
            )),

    MATCH_NAXXRAMAS (
            "Playing",
            ScreenGroup.MATCH_PLAYING,
            EnumSet.of(
                    Pixel.MATCH_NAXXRAMAS_B,
                    Pixel.MATCH_NAXXRAMAS_C,
                    Pixel.MATCH_NAXXRAMAS_K,
                    Pixel.MATCH_NAXXRAMAS_L
            ),
            EnumSet.of(
                    Pixel.MATCH_NAXXRAMAS_D,
                    Pixel.MATCH_NAXXRAMAS_E,
                    Pixel.MATCH_NAXXRAMAS_R
            )),

    MATCH_ORGRIMMAR (
            "Playing",
            ScreenGroup.MATCH_PLAYING,
            EnumSet.of(
                    Pixel.MATCH_ORGRIMMAR_B,
                    Pixel.MATCH_ORGRIMMAR_C,
                    Pixel.MATCH_ORGRIMMAR_K,
                    Pixel.MATCH_ORGRIMMAR_L
            ),
            EnumSet.of(
                    Pixel.MATCH_ORGRIMMAR_D,
                    Pixel.MATCH_ORGRIMMAR_E,
                    Pixel.MATCH_ORGRIMMAR_R
            )),

    MATCH_PANDARIA (
            "Playing",
            ScreenGroup.MATCH_PLAYING,
            EnumSet.of(
                    Pixel.MATCH_PANDARIA_B,
                    Pixel.MATCH_PANDARIA_C,
                    Pixel.MATCH_PANDARIA_K,
                    Pixel.MATCH_PANDARIA_L
                    ),
            EnumSet.of(
                    Pixel.MATCH_PANDARIA_D,
                    Pixel.MATCH_PANDARIA_E,
                    Pixel.MATCH_PANDARIA_R
            )),

    MATCH_STORMWIND (
            "Playing",
            ScreenGroup.MATCH_PLAYING,
            EnumSet.of(
                    Pixel.MATCH_STORMWIND_B,
                    Pixel.MATCH_STORMWIND_C,
                    Pixel.MATCH_STORMWIND_K,
                    Pixel.MATCH_STORMWIND_L
            ),
            EnumSet.of(
                    Pixel.MATCH_STORMWIND_D,
                    Pixel.MATCH_STORMWIND_E,
                    Pixel.MATCH_STORMWIND_R
            )),

    MATCH_STRANGLETHORN (
            "Playing",
            ScreenGroup.MATCH_PLAYING,
            EnumSet.of(
                    Pixel.MATCH_STRANGLETHORN_B,
                    Pixel.MATCH_STRANGLETHORN_C,
                    Pixel.MATCH_STRANGLETHORN_K,
                    Pixel.MATCH_STRANGLETHORN_L
            ),
            EnumSet.of(
                    Pixel.MATCH_STRANGLETHORN_D,
                    Pixel.MATCH_STRANGLETHORN_E,
                    Pixel.MATCH_STRANGLETHORN_R
            )),

    MATCH_NAXXRAMAS_END (
            "Result",
            ScreenGroup.MATCH_END,
            EnumSet.of(
                    Pixel.MATCH_NAXXRAMAS_END_B,
                    Pixel.MATCH_NAXXRAMAS_END_C,
                    Pixel.MATCH_NAXXRAMAS_END_K,
                    Pixel.MATCH_NAXXRAMAS_END_L
            ),
            EnumSet.of(
                    Pixel.MATCH_NAXXRAMAS_END_D,
                    Pixel.MATCH_NAXXRAMAS_END_E,
                    Pixel.MATCH_NAXXRAMAS_END_R
            )),

    MATCH_ORGRIMMAR_END (
            "Result",
            ScreenGroup.MATCH_END,
            EnumSet.of(
                    Pixel.MATCH_ORGRIMMAR_END_B,
                    Pixel.MATCH_ORGRIMMAR_END_C,
                    Pixel.MATCH_ORGRIMMAR_END_K,
                    Pixel.MATCH_ORGRIMMAR_END_L
            ),
            EnumSet.of(
                    Pixel.MATCH_ORGRIMMAR_END_D,
                    Pixel.MATCH_ORGRIMMAR_END_E,
                    Pixel.MATCH_ORGRIMMAR_END_R
            )),

    MATCH_PANDARIA_END (
            "Result",
            ScreenGroup.MATCH_END,
            EnumSet.of(
                    Pixel.MATCH_PANDARIA_END_B,
                    Pixel.MATCH_PANDARIA_END_C,
                    Pixel.MATCH_PANDARIA_END_K,
                    Pixel.MATCH_PANDARIA_END_L
            ),
            EnumSet.of(
                    Pixel.MATCH_PANDARIA_END_D,
                    Pixel.MATCH_PANDARIA_END_E,
                    Pixel.MATCH_PANDARIA_END_R
            )),

    MATCH_STORMWIND_END (
            "Result",
            ScreenGroup.MATCH_END,
            EnumSet.of(
                    Pixel.MATCH_STORMWIND_END_B,
                    Pixel.MATCH_STORMWIND_END_C,
                    Pixel.MATCH_STORMWIND_END_K,
                    Pixel.MATCH_STORMWIND_END_L
            ),
            EnumSet.of(
                    Pixel.MATCH_STORMWIND_END_D,
                    Pixel.MATCH_STORMWIND_END_E,
                    Pixel.MATCH_STORMWIND_END_R
            )),

    MATCH_STRANGLETHORN_END (
            "Result",
            ScreenGroup.MATCH_END,
            EnumSet.of(
                    Pixel.MATCH_STRANGLETHORN_END_B,
                    Pixel.MATCH_STRANGLETHORN_END_C,
                    Pixel.MATCH_STRANGLETHORN_END_K,
                    Pixel.MATCH_STRANGLETHORN_END_L
            ),
            EnumSet.of(
                    Pixel.MATCH_STRANGLETHORN_END_D,
                    Pixel.MATCH_STRANGLETHORN_END_E,
                    Pixel.MATCH_STRANGLETHORN_END_R
            )),

    ;


    /*
     * Enums can't refer to themselves during construction, so the set of 'next' screens cannot be defined
     * directly in the enums above. The following code adds that set to each screen immediately after it
     * is constructed.
     */
    static {

        TITLE.nextScreens = EnumSet.of(
                Screen.MAIN
        );

        MAIN.nextScreens = EnumSet.of(
                Screen.COLLECTION,
                Screen.PLAY_LOBBY,
                Screen.PRACTICE_LOBBY,
                Screen.ARENA_CHOOSE,
                Screen.ARENA_LOBBY,
                Screen.VERSUS_LOBBY
        );

        MAIN_TODAYSQUESTS.nextScreens = EnumSet.of(
                Screen.MAIN
        );

        COLLECTION.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.COLLECTION_DECK,
                Screen.COLLECTION_ZOOM
        );

        COLLECTION_DECK.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.COLLECTION,
                Screen.COLLECTION_ZOOM
        );

        COLLECTION_ZOOM.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.COLLECTION,
                Screen.COLLECTION_DECK
        );

        ARENA_CHOOSE.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.ARENA_LOBBY
        );

        ARENA_END.nextScreens = EnumSet.of(
                Screen.MAIN
        );

        ARENA_LOBBY.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.FINDING_OPPONENT
        );

        PLAY_LOBBY.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.FINDING_OPPONENT
        );

        PRACTICE_LOBBY.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.FINDING_OPPONENT
        );

        VERSUS_LOBBY.nextScreens = EnumSet.of(
            Screen.MAIN,
            Screen.MATCH_VS
        );
        
        FINDING_OPPONENT.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.PLAY_LOBBY,
                Screen.PRACTICE_LOBBY,
                Screen.ARENA_LOBBY,
                Screen.MATCH_VS
        );

        MATCH_VS.nextScreens = EnumSet.of(
                Screen.MATCH_STARTINGHAND
        );

        MATCH_STARTINGHAND.nextScreens = EnumSet.of(
                Screen.MATCH_NAXXRAMAS,
                Screen.MATCH_ORGRIMMAR,
                Screen.MATCH_PANDARIA,
                Screen.MATCH_STORMWIND,
                Screen.MATCH_STRANGLETHORN
        );

        MATCH_NAXXRAMAS.nextScreens = EnumSet.of(
                Screen.MATCH_NAXXRAMAS_END
        );

        MATCH_ORGRIMMAR.nextScreens = EnumSet.of(
                Screen.MATCH_ORGRIMMAR_END
        );

        MATCH_PANDARIA.nextScreens = EnumSet.of(
                Screen.MATCH_PANDARIA_END
        );

        MATCH_STORMWIND.nextScreens = EnumSet.of(
                Screen.MATCH_STORMWIND_END
        );

        MATCH_STRANGLETHORN.nextScreens = EnumSet.of(
                Screen.MATCH_STRANGLETHORN_END
        );

        MATCH_ORGRIMMAR_END.nextScreens = EnumSet.of(
                Screen.MAIN,
                Screen.PLAY_LOBBY,
                Screen.PRACTICE_LOBBY,
                Screen.ARENA_LOBBY,
                Screen.VERSUS_LOBBY,
                Screen.ARENA_END
        );

    MATCH_NAXXRAMAS_END.nextScreens = MATCH_ORGRIMMAR_END.nextScreens;
		MATCH_PANDARIA_END.nextScreens = MATCH_ORGRIMMAR_END.nextScreens;
		MATCH_STORMWIND_END.nextScreens = MATCH_ORGRIMMAR_END.nextScreens;
		MATCH_STRANGLETHORN_END.nextScreens = MATCH_ORGRIMMAR_END.nextScreens;

    }


    public final String title;
    public final ScreenGroup group;
    public final EnumSet<Pixel> primary;
    public final EnumSet<Pixel> secondary;
    public final EnumSet<Pixel> primaryAndSecondary;
    public EnumSet<Screen> nextScreens;

    Screen(String title, ScreenGroup group, EnumSet<Pixel> primary, EnumSet<Pixel> secondary) {
        this.title = title;
        this.group = group;
        this.primary = primary;
        this.secondary = secondary;

        // primaryAndSecondary is a convenience property that makes it easier to iterate through both primary and secondary in one loop
        this.primaryAndSecondary = EnumSet.copyOf(primary);
        this.primaryAndSecondary.addAll(secondary);
    }

}
