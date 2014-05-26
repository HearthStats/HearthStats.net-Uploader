package net.hearthstats.analysis;

import java.awt.image.BufferedImage;
import java.text.MessageFormat;
import java.util.Observable;
import java.util.ResourceBundle;

import net.hearthstats.BackgroundImageSave;
import net.hearthstats.Config;
import net.hearthstats.HearthstoneMatch;
import net.hearthstats.Main;
import net.hearthstats.log.Log;
import net.hearthstats.ocr.OcrException;
import net.hearthstats.ocr.OpponentNameRankedOcr;
import net.hearthstats.ocr.OpponentNameUnrankedOcr;
import net.hearthstats.ocr.RankLevelOcr;
import net.hearthstats.state.PixelLocation;
import net.hearthstats.state.Screen;
import net.hearthstats.state.ScreenGroup;
import net.hearthstats.state.UniquePixel;
import net.hearthstats.util.Coordinate;
import net.hearthstats.util.MatchOutcome;
import net.hearthstats.util.Rank;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The main analyser for Hearthstone. Uses screenshots to determine what state the game is in,
 * and updates the match records appropriately.
 *
 * @author gtch
 */
public class HearthstoneAnalyser extends Observable {

    private final static Logger debugLog = LoggerFactory.getLogger(HearthstoneAnalyser.class);

    private final ResourceBundle bundle = ResourceBundle.getBundle("net.hearthstats.resources.Main");

    private final ScreenAnalyser screenAnalyser;
    private final IndividualPixelAnalyser individualPixelAnalyser;
    private final RelativePixelAnalyser relativePixelAnalyser;

    private final OpponentNameRankedOcr opponentNameRankedOcr = new OpponentNameRankedOcr();
    private final OpponentNameUnrankedOcr opponentNameUnrankedOcr = new OpponentNameUnrankedOcr();
    private final RankLevelOcr rankLevelOcr = new RankLevelOcr();

    private BufferedImage lastImage;

    private Screen screen = null;

    private boolean isNewArena = false;

    private boolean arenaRunEndDetected = false;
    private boolean victoryOrDefeatDetected = false;
    private boolean isYourTurn = true;
    private HearthstoneMatch match = new HearthstoneMatch();
    private String mode;
    private int deckSlot;
    private Rank rankLevel;
    private long startTime;

    private int iterationsSinceFindingOpponent = 0;
    private int iterationsSinceClassCheckingStarted = 0;
    private int iterationsSinceScreenMatched = 0;
    private int iterationsSinceYourTurn = 0;
    private int iterationsSinceOpponentTurn = 0;


    public HearthstoneAnalyser() {
        this.screenAnalyser = new ScreenAnalyser();
        this.individualPixelAnalyser = new IndividualPixelAnalyser();
        this.relativePixelAnalyser = new RelativePixelAnalyser();
    }




    public void analyze(BufferedImage image) {

        // Cleanup the last image because it's no longer needed
        if (lastImage != null) {
            lastImage.flush();
        }
        lastImage = image;

        Screen matchedScreen;
        if (iterationsSinceScreenMatched < 10) {
            // We've recently matched a screen, so only consider screens that are likely to follow the last screen.
            matchedScreen = screenAnalyser.identifyScreen(image, screen);
        } else {
            // It's been many iterations since we've matched anything, so maybe we've moved on and missed a key screen.
            // Perform a full analysis against all screens instead of the limited range used above.
            matchedScreen = screenAnalyser.identifyScreen(image, null);
        }

        if (matchedScreen == null) {
            // No screen was detected
            iterationsSinceScreenMatched++;

        } else {
            // A screen was detected, so process any screen-specific tests now
            iterationsSinceScreenMatched = 0;
            boolean screenChangedOK = handleScreenChange(image, screen, matchedScreen);
            if (screenChangedOK) {
                handleScreenActions(image, matchedScreen);
            } else {
                debugLog.debug("Ignored screen {} because it was determined to be invalid", matchedScreen);
            }
        }

    }


    private void handleScreenActions(BufferedImage image, Screen newScreen) {

        if (newScreen != null) {
            debugLog.debug("Screen being processed {}", newScreen);

            // Only handle known screens; if the screen cannot be identified then ignore it
            switch (newScreen) {

                case PLAY_LOBBY:
                    testForCasualOrRanked(image);
                    testForDeckSlot(image);
                    break;

                case PRACTICE_LOBBY:
                    setMode("Practice");
                    break;

                case ARENA_LOBBY:
                    setMode("Arena");
                    testForNewArenaRun(image);
                    break;

                case MATCH_VS:
                    testForYourClass(image);
                    testForOpponentClass(image);
                    testForCoin(image);
                    testForOpponentName(image);
                    iterationsSinceClassCheckingStarted++;
                    break;

                case MATCH_STARTINGHAND:
                    testForCoin(image);
                    testForOpponentName(image);
                    break;

            }

            if (!"Practice".equals(getMode())) {
                switch (newScreen.group) {

                    case MATCH_PLAYING:
                        testForOpponentOrYourTurn(image);
                        break;

                    case MATCH_END:
                        testForVictoryOrDefeat(image);
                        break;

                }
            }

            if (victoryOrDefeatDetected && newScreen.group != ScreenGroup.MATCH_END) {
                // We have moved on from the match end screen, so it's now safe to reset the victoryOrDefeatDetected flag
                victoryOrDefeatDetected = false;
            }

        }


    }


    /**
     * <p>Handles screen changes - determines if the screen has changes since the last iteration, and if so it performs
     * any actions that need to occur on the transition to a new screen.</p>
     * <p>If the screen has not changed then no action is taken.</p>
     * <p>This method may determine that a screen is actually not suitable and should be rejected. This can occur when
     * a screenshot has occured during compositing and so we have an incomplete screen. If this method returns false
     * then the current iteration should be skipped because results could be invalid.</p>
     *
     * @param image The screenshot of the new screen
     * @param previousScreen The previous screen, if known
     * @param newScreen The new screen
     * @return true if handled OK, false if the screen was rejected and should be skipped.
     */
    private boolean handleScreenChange(BufferedImage image, Screen previousScreen, Screen newScreen) {

        if (newScreen != null && newScreen != previousScreen) {
            debugLog.debug("Screen changed from {} to {}", previousScreen, newScreen);

            if (newScreen == Screen.PLAY_LOBBY) {
                // Hearthstone screenshots will sometimes display the play lobby instead of 'Finding Opponent' popup
                // presumably because the screenshot catches Hearthstone before it's finished drawing the screen.
                // The follow two methods attempt to detect this and ignore it.
                if (imageShowsPlayBackground(image)) {
                    // This isn't the real play screen because the hero buttons are missing. That confirms that it
                    // is a background image behind the 'Finding Opponent' screen
                    return false;
                }
                if (previousScreen == Screen.FINDING_OPPONENT) {
                    // The last screen was 'Finding Opponent' so wait a couple of iterations to be sure this isn't
                    // just Hearthstone briefly flashing the background image to us.
                    if (iterationsSinceFindingOpponent < 5) {
                        iterationsSinceFindingOpponent++;
                        return false;
                    } else {
                        // It has been the play screen for several iterations now so we probably are genuinely on the screen
                        iterationsSinceFindingOpponent = 0;
                    }
                }
            } else {
                iterationsSinceFindingOpponent = 0;
            }

            if (newScreen == Screen.ARENA_END) {
                setArenaRunEnd();
            }

            switch (newScreen.group) {

                case MATCH_START:
                    match = new HearthstoneMatch();
                    match.mode_$eq(mode);
                    match.deckSlot_$eq(deckSlot);
                    match.rankLevel_$eq(rankLevel);
                    arenaRunEndDetected = false;
                    isYourTurn = false;
                    iterationsSinceClassCheckingStarted = 0;
                    iterationsSinceYourTurn = 0;
                    iterationsSinceOpponentTurn = 0;
                    break;

                case MATCH_PLAYING:
                    startTimer();
				if ((previousScreen != null && previousScreen.group == ScreenGroup.MATCH_START)
						&& (match.opponentClass() == null || match
								.userClass() == null)) {
                        // Failed to detect classes, so ask the user to submit screenshots of the problem
                        Log.warn(t("warning.classdetection", Config.getExtractionFolder()));
                    }
                    break;

                case MATCH_END:
                    break;

            }

            setScreen(newScreen);
        }

        return true;
    }


    private void testForNewArenaRun(BufferedImage image) {
        if (!isNewArena()) {
            debugLog.debug("Testing for new arena run");
            if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                    UniquePixel.NEW_ARENA_RUN_A, UniquePixel.NEW_ARENA_RUN_B, UniquePixel.NEW_ARENA_RUN_C, UniquePixel.NEW_ARENA_RUN_D, UniquePixel.NEW_ARENA_RUN_E })) {
                setIsNewArena(true);
            }
        }
    }


    private void testForCasualOrRanked(BufferedImage image) {
        if (!"Casual".equals(getMode())) {
            // Last time we checked we were not in casual mode, check if we are now
            debugLog.debug("Testing for casual mode");
            if (imageShowsCasualPlaySelected(image)) {
                // Matched casual mode
                setMode("Casual");
            }
        }
        if (!"Ranked".equals(getMode())) {
            // Last time we checked we were not in ranked mode, check if we are now
            debugLog.debug("Testing for ranked mode");
            if (imageShowsRankedPlaySelected(image)) {
                // Matched ranked mode
                analyzeRankLevel(image);
                setMode("Ranked");
            }
        }
    }


    private void testForDeckSlot(BufferedImage image) {
        debugLog.debug("Testing for deck slot");
        Integer newDeckSlot = imageIdentifyDeckSlot(image);
        if (newDeckSlot != null) {
            if (newDeckSlot.intValue() != getDeckSlot()) {
                // The deck slot has changed
                setDeckSlot(newDeckSlot);
            }
        }
    }


    private void testForYourClass(BufferedImage image) {
        if (getYourClass() == null) {
            debugLog.debug("Testing for your class");
            String newClass = imageIdentifyYourClass(image);
            if (newClass != null) {
                setYourClass(newClass);
            }

            if (iterationsSinceClassCheckingStarted > 3 && ((iterationsSinceClassCheckingStarted & 3) == 0)) {
                // There have been four iterations since checking for the class, and no class was detected
                // so save a screenshot of every fourths iteration that the user can send us for review
                String filename = "class-yours-" + ((iterationsSinceClassCheckingStarted >> 2));

                BackgroundImageSave.saveCroppedPngImage(image, filename, 204, 600, 478, 530);
            }
        }
    }


    private void testForOpponentClass(BufferedImage image) {
        if (getOpponentClass() == null) {
            debugLog.debug("Testing for opponent class");
            String newClass = imageIdentifyOpponentClass(image);
            if (newClass != null) {
                setOpponentClass(newClass);
            }

            if (iterationsSinceClassCheckingStarted > 3 && ((iterationsSinceClassCheckingStarted & 3) == 0)) {
                // There have been four iterations since checking for the class, and no class was detected
                // so save a screenshot of every fourth iteration that the user can send us for review
                String filename = "class-opponent-" + ((iterationsSinceClassCheckingStarted >> 2));
                BackgroundImageSave.saveCroppedPngImage(image, filename, 1028, 28, 478, 530);
            }
        }

    }


    private void testForOpponentName(BufferedImage image) {
        if (getOpponentName() == null) {
            debugLog.debug("Testing for opponent name");
            if (imageShowsOpponentName(image)) {
                analyseOpponentName(image);
            }
        }
    }


    private void testForCoin(BufferedImage image) {
        if (!getCoin()) {
            debugLog.debug("Testing for coin");
            if (imageShowsCoin(image)) {
                setCoin(true);
            }
        }
    }


    private void testForOpponentOrYourTurn(BufferedImage image) {

        if (isYourTurn()) {
            // Last time we checked it was your turn, check whether it's now the opponent's turn
            debugLog.debug("Testing for opponent turn");
            if (imageShowsOpponentTurn(image)) {
                iterationsSinceYourTurn++;
                // Skip two iterations before actually switching, to reduce false detection as a card flies over the turn button
                if (iterationsSinceYourTurn > 2) {
                    setYourTurn(false);
                    iterationsSinceYourTurn = 0;
                }
            } else {
                iterationsSinceYourTurn = 0;
            }
        } else {
            // Last time we checked it was the opponent's turn, check whether it's now your turn
            debugLog.debug("Testing for your turn");
            if (imageShowsYourTurn(image)) {
                iterationsSinceOpponentTurn++;
                // Skip two iterations before actually switching, to reduce false detection as a card flies over the turn button
                if (iterationsSinceOpponentTurn > 2) {
                    setYourTurn(true);
                    iterationsSinceOpponentTurn = 0;
                }
            } else {
                iterationsSinceOpponentTurn = 0;
            }
        }
    }


    private void testForVictoryOrDefeat(BufferedImage image) {
        if (!victoryOrDefeatDetected) {
            debugLog.debug("Testing for victory or defeat");

            MatchOutcome result = imageShowsVictoryOrDefeat(image);

            if (result == MatchOutcome.VICTORY) {
                endTimer();
                victoryOrDefeatDetected = true;     // Set to true to prevent match results being submitted multiple times
                setResult("Victory");
            } else if (result == MatchOutcome.DEFEAT) {
                endTimer();
                victoryOrDefeatDetected = true;     // Set to true to prevent match results being submitted multiple times
                setResult("Defeat");
            }
        }

    }


    private void setArenaRunEnd() {
        if (!arenaRunEndDetected) {
            debugLog.debug("Setting end of arena run");
            arenaRunEndDetected = true;
            notifyObserversOfChangeTo(AnalyserEvent.ARENA_END);
        }
    }


    boolean imageShowsCoin(BufferedImage image) {
        return individualPixelAnalyser.testAnyPixelsMatch(image, new UniquePixel[]{
                UniquePixel.COIN_1, UniquePixel.COIN_2, UniquePixel.COIN_3, UniquePixel.COIN_4, UniquePixel.COIN_5 });
    }


    boolean imageShowsCasualPlaySelected(BufferedImage image) {
        return individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.MODE_CASUAL_1A, UniquePixel.MODE_CASUAL_1B })
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.MODE_CASUAL_2A, UniquePixel.MODE_CASUAL_2B })
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.MODE_CASUAL_3A, UniquePixel.MODE_CASUAL_3B });

    }


    boolean imageShowsRankedPlaySelected(BufferedImage image) {
        return individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.MODE_RANKED_1A, UniquePixel.MODE_RANKED_1B })
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.MODE_RANKED_2A, UniquePixel.MODE_RANKED_2B })
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.MODE_RANKED_3A, UniquePixel.MODE_RANKED_3B });
    }


    boolean imageShowsOpponentTurn(BufferedImage image) {
        return individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.TURN_OPPONENT_1A, UniquePixel.TURN_OPPONENT_1B})
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.TURN_OPPONENT_2A, UniquePixel.TURN_OPPONENT_2B})
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.TURN_OPPONENT_3A, UniquePixel.TURN_OPPONENT_3B});
    }


    boolean imageShowsYourTurn(BufferedImage image) {
        return individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.TURN_YOUR_1A, UniquePixel.TURN_YOUR_1B})
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.TURN_YOUR_2A, UniquePixel.TURN_YOUR_2B});
    }


    boolean imageShowsOpponentName(BufferedImage image) {
        return
            individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.NAME_OPPONENT_1A, UniquePixel.NAME_OPPONENT_1B, UniquePixel.NAME_OPPONENT_1C})
            &&
            individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.NAME_OPPONENT_2A, UniquePixel.NAME_OPPONENT_2B, UniquePixel.NAME_OPPONENT_2C});
    }


    MatchOutcome imageShowsVictoryOrDefeat(BufferedImage image) {

        // Try to find the reference coordinate: the top of the horn on the left of the victory or defeat popup
        Coordinate referenceCoordinate = relativePixelAnalyser.findRelativePixel(image, UniquePixel.VICTORY_DEFEAT_REFBOX_TL, UniquePixel.VICTORY_DEFEAT_REFBOX_BR, 8, 11);

        // Only check if the top of the horn could be found
        if (referenceCoordinate != null) {

            // At least one of victory group 1 must match
            int victory1Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate, new UniquePixel[] {
                    UniquePixel.VICTORY_REL_1A, UniquePixel.VICTORY_REL_1B
            });
            // All three of victory group 2 must match
            int victory2Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate, new UniquePixel[] {
                    UniquePixel.VICTORY_REL_2A, UniquePixel.VICTORY_REL_2B, UniquePixel.VICTORY_REL_2C
            });
            // At least one of defeat group 1 must match
            int defeat1Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate, new UniquePixel[] {
                    UniquePixel.DEFEAT_REL_1A, UniquePixel.DEFEAT_REL_1B, UniquePixel.DEFEAT_REL_1C, UniquePixel.DEFEAT_REL_1D, UniquePixel.DEFEAT_REL_1E
            });
            // All of defeat group 2 must match
            int defeat2Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate, new UniquePixel[] {
                    UniquePixel.DEFEAT_REL_2A
            });

            boolean matchedVictory = victory1Matches > 0 && victory2Matches == 3 && defeat1Matches == 0  && defeat2Matches == 0;
            boolean matchedDefeat = victory1Matches == 0 && victory2Matches == 0 && defeat1Matches > 0  && defeat2Matches == 1;

            if (matchedVictory && matchedDefeat) {
                // Shouldn't be possible, but just in case...
                debugLog.warn("Matched both victory and defeat, which shouldn't be possible. Will try again next iteration.");
            } else if (matchedVictory) {
                return MatchOutcome.VICTORY;
            } else if (matchedDefeat) {
                return MatchOutcome.DEFEAT;
            }
        }

        return null;
    }


    /**
     * <p>Sometimes the OS X version captures a screenshot where, apparently, Hearthstone hasn't finished compositing the screen
     * and so we only get the background. This can happen whenever there is something layered over the main screen, for example
     * during the 'Finding Opponent', 'Victory' and 'Defeat' screens.</p>
     * <p>At the moment I haven't worked out how to ensure we always get the completed screen. So this method detects when
     * we've receive and incomplete play background instead of the 'Finding Opponent' screen, so we can reject it and try again.</p>
     * @param image
     * @return true if this screenshot shows a background image that should be ignored
     */
    boolean imageShowsPlayBackground(BufferedImage image) {
        return individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.BACKGROUND_PLAY_1, UniquePixel.BACKGROUND_PLAY_2, UniquePixel.BACKGROUND_PLAY_3,
                UniquePixel.BACKGROUND_PLAY_4, UniquePixel.BACKGROUND_PLAY_5, UniquePixel.BACKGROUND_PLAY_6,
                UniquePixel.BACKGROUND_PLAY_7, UniquePixel.BACKGROUND_PLAY_8, UniquePixel.BACKGROUND_PLAY_9
        });
    }


    Integer imageIdentifyDeckSlot(BufferedImage image) {
        if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_1A, UniquePixel.DECK_SLOT_1B})) {
            return 1;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_2A, UniquePixel.DECK_SLOT_2B})) {
            return 2;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_3A, UniquePixel.DECK_SLOT_3B})) {
            return 3;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_4A, UniquePixel.DECK_SLOT_4B})) {
            return 4;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_5A, UniquePixel.DECK_SLOT_5B})) {
            return 5;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_6A, UniquePixel.DECK_SLOT_6B})) {
            return 6;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_7A, UniquePixel.DECK_SLOT_7B})) {
            return 7;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_8A, UniquePixel.DECK_SLOT_8B})) {
            return 8;
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DECK_SLOT_9A, UniquePixel.DECK_SLOT_9B})) {
            return 9;
        } else {
            return null;
        }
    }


    String imageIdentifyYourClass(BufferedImage image) {
        if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_DRUID_1, UniquePixel.YOUR_DRUID_2, UniquePixel.YOUR_DRUID_3})) {
            return "Druid";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_HUNTER_1, UniquePixel.YOUR_HUNTER_2, UniquePixel.YOUR_HUNTER_3})) {
            return "Hunter";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_HUNTER_GOLDEN_1, UniquePixel.YOUR_HUNTER_GOLDEN_2, UniquePixel.YOUR_HUNTER_GOLDEN_3})) {
            return "Hunter";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_MAGE_1, UniquePixel.YOUR_MAGE_2, UniquePixel.YOUR_MAGE_3})) {
            return "Mage";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_PALADIN_1, UniquePixel.YOUR_PALADIN_2, UniquePixel.YOUR_PALADIN_3})) {
            return "Paladin";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_PRIEST_1, UniquePixel.YOUR_PRIEST_2, UniquePixel.YOUR_PRIEST_3})) {
            return "Priest";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_ROGUE_1, UniquePixel.YOUR_ROGUE_2, UniquePixel.YOUR_ROGUE_3})) {
            return "Rogue";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_SHAMAN_1, UniquePixel.YOUR_SHAMAN_2, UniquePixel.YOUR_SHAMAN_3})) {
            return "Shaman";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_WARLOCK_1, UniquePixel.YOUR_WARLOCK_2, UniquePixel.YOUR_WARLOCK_3})) {
            return "Warlock";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[] {
                UniquePixel.YOUR_WARRIOR_1, UniquePixel.YOUR_WARRIOR_2, UniquePixel.YOUR_WARRIOR_3})) {
            return "Warrior";
        } else {
            return null;
        }

    }


    String imageIdentifyOpponentClass(BufferedImage image) {
        if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_DRUID_1, UniquePixel.OPPONENT_DRUID_2, UniquePixel.OPPONENT_DRUID_3})) {
            return "Druid";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_HUNTER_1, UniquePixel.OPPONENT_HUNTER_2, UniquePixel.OPPONENT_HUNTER_3})) {
            return "Hunter";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_MAGE_1, UniquePixel.OPPONENT_MAGE_2, UniquePixel.OPPONENT_MAGE_3})) {
            return "Mage";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_PALADIN_1, UniquePixel.OPPONENT_PALADIN_2, UniquePixel.OPPONENT_PALADIN_3})) {
            return "Paladin";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_PRIEST_1, UniquePixel.OPPONENT_PRIEST_2, UniquePixel.OPPONENT_PRIEST_3})) {
            return "Priest";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_ROGUE_1, UniquePixel.OPPONENT_ROGUE_2, UniquePixel.OPPONENT_ROGUE_3})) {
            return "Rogue";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_SHAMAN_1, UniquePixel.OPPONENT_SHAMAN_2, UniquePixel.OPPONENT_SHAMAN_3})) {
            return "Shaman";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_WARLOCK_1, UniquePixel.OPPONENT_WARLOCK_2, UniquePixel.OPPONENT_WARLOCK_3})) {
            return "Warlock";
        } else if (individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.OPPONENT_WARRIOR_1, UniquePixel.OPPONENT_WARRIOR_2, UniquePixel.OPPONENT_WARRIOR_3})) {
            return "Warrior";
        } else {
            return null;
        }
    }


    private void analyzeRankLevel(BufferedImage image) {

        try {
            Integer rankInteger = rankLevelOcr.processNumber(image);
            Rank rank = Rank.fromInt(rankInteger);

            if (rank == null) {
                Log.warn("Could not interpret rank, your rank may not be recorded correctly");
            } else {
                setRankLevel(rank);
            }

        } catch (OcrException e) {
            Main.showErrorDialog(e.getMessage(), e);
            notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE);
        }

    }


    private void analyseOpponentName(BufferedImage image) {
        String opponentName;

        try {
            if ("Ranked".equals(getMode())) {
                opponentName = opponentNameRankedOcr.process(image);
            } else {
                opponentName = opponentNameUnrankedOcr.process(image);
            }

            setOpponentName(opponentName);

        } catch (OcrException e) {
            Main.showErrorDialog(e.getMessage(), e);
            notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE);
        }
    }


    public void resetMatch() {
        match = new HearthstoneMatch();
    }


    public void reset() {
        resetMatch();
        screen = null;
        isYourTurn = false;
        arenaRunEndDetected = false;
    }


    public boolean getCoin() {
		return match.coin();
    }


    public HearthstoneMatch getMatch() {
        return match;
    }


    public int getDeckSlot() {
		return match.deckSlot();
    }


    public String getMode() {
		return match.mode();
    }


    public String getOpponentClass() {
		return match.opponentClass();
    }


    public String getOpponentName() {
		return match.opponentName();
    }


    public Rank getRankLevel() {
		return match.rankLevel();
    }


    public String getResult() {
		return match.result();
    }


    public String getYourClass() {
		return match.userClass();
    }


    public Screen getScreen() {
        //return screen == null ? "Unknown" : screen.title;
        return screen;
    }


    public boolean isYourTurn() {
        return isYourTurn;
    }


    public boolean isNewArena() {
        return isNewArena;
    }


    public void setIsNewArena(boolean isNew) {
        isNewArena = isNew;
        notifyObserversOfChangeTo(AnalyserEvent.NEW_ARENA);
    }


    private void setDeckSlot(int deckSlot) {
        this.deckSlot = deckSlot;
		match.deckSlot_$eq(deckSlot);
        notifyObserversOfChangeTo(AnalyserEvent.DECK_SLOT);
    }


    private void setCoin(boolean coin) {
		match.coin_$eq(coin);
        notifyObserversOfChangeTo(AnalyserEvent.COIN);
    }


    private void setMode(String mode) {
        if (!StringUtils.equals(this.mode, mode)) {
            debugLog.debug("Mode changed from {} to {}", this.mode, mode);

            this.mode = mode;
			match.mode_$eq(mode);

            notifyObserversOfChangeTo(AnalyserEvent.MODE);
        }
    }


    private void setOpponentClass(String opponentClass) {
		match.opponentClass_$eq(opponentClass);
        notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_CLASS);
    }


    private void setRankLevel(Rank rankLevel) {
        this.rankLevel = rankLevel;
		match.rankLevel_$eq(rankLevel);
    }


    private void setOpponentName(String opponentName) {
		match.opponentName_$eq(opponentName);
        notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_NAME);
    }


    private void setResult(String result) {
		match.result_$eq(result);
        notifyObserversOfChangeTo(AnalyserEvent.RESULT);
    }


    private void setScreen(Screen screen) {
        this.screen = screen;
        notifyObserversOfChangeTo(AnalyserEvent.SCREEN);
    }


    private void setYourClass(String yourClass) {
		match.userClass_$eq(yourClass);
        notifyObserversOfChangeTo(AnalyserEvent.YOUR_CLASS);
    }


    private void setYourTurn(boolean yourTurn) {
        this.isYourTurn = yourTurn;
        if (yourTurn) {
			match.numTurns_$eq(match.numTurns() + 1);
        }
        notifyObserversOfChangeTo(AnalyserEvent.YOUR_TURN);
    }


    private void startTimer() {
        startTime = System.currentTimeMillis();
    }


    private void endTimer() {
		match.duration_$eq(Math.round((System.currentTimeMillis() - startTime) / 1000));
    }


    private void notifyObserversOfChangeTo(AnalyserEvent property) {
        setChanged();
        notifyObservers(property);
    }

    private String t(String key, String value0) {
        String message = bundle.getString(key);
        return MessageFormat.format(message, value0);
    }


    static public float getRatio(BufferedImage image) {
        return image.getHeight() / (float) PixelLocation.REFERENCE_SIZE.y;
    }


    static public int getXOffset(BufferedImage image, float ratio) {
        return (int) (((float) image.getWidth() - (ratio * PixelLocation.REFERENCE_SIZE.x)) / 2);
    }
}
