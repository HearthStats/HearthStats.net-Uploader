package net.hearthstats.analysis;

import net.hearthstats.HearthstoneMatch;
import net.hearthstats.Main;
import net.hearthstats.OCR;
import net.hearthstats.state.Screen;
import net.hearthstats.state.ScreenGroup;
import net.hearthstats.state.UniquePixel;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorConvertOp;
import java.awt.image.RescaleOp;
import java.io.File;
import java.util.Date;
import java.util.Observable;

/**
 * The main analyser for Hearthstone. Uses screenshots to determine what state the game is in,
 * and updates the match records appropriately.
 *
 * @author gtch
 */
public class HearthstoneAnalyser extends Observable {

    private final static Logger debugLog = LoggerFactory.getLogger(HearthstoneAnalyser.class);

    private final ScreenAnalyser screenAnalyser;
    private final IndividualPixelAnalyser individualPixelAnalyser;

    private BufferedImage lastImage;

    private Screen screen = null;

    private boolean isNewArena = false;

    private boolean arenaRunEndDetected = false;
    private boolean victoryOrDefeatDetected = false;
    private boolean isYourTurn = true;
    private HearthstoneMatch match = new HearthstoneMatch();
    private String mode;
    private int deckSlot;
    private Integer rankLevel;
    private int analyzeRankRetries = 0;
    private int iterationsSinceFindingOpponent = 0;
    private long startTime;



    public HearthstoneAnalyser() {
        this.screenAnalyser = new ScreenAnalyser();
        this.individualPixelAnalyser = new IndividualPixelAnalyser();
    }


    private int iterations = 0;


    public void analyze(BufferedImage image) {

        // Cleanup the last image because it's no longer needed
        if (lastImage != null) {
            lastImage.flush();
        }
        lastImage = image;


        // We don't know what screen we're on, so perform a full analysis against all screens
        Screen matchedScreen = screenAnalyser.identifyScreen(image, screen);

        if (matchedScreen != null) {
            // A screen was detected, so process any screen-specific tests now
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
                        testForVictory(image);
                        testForDefeat(image);
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
                    match.setMode(mode);
                    match.setDeckSlot(deckSlot);
                    match.setRankLevel(rankLevel);
                    arenaRunEndDetected = false;
                    isYourTurn = false;
                    break;

                case MATCH_PLAYING:
                    startTimer();
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
                analyzeRankRetries = 0;
                setMode("Casual");
            }
        } else {
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
        }
    }


    private void testForOpponentClass(BufferedImage image) {
        if (getOpponentClass() == null) {
            debugLog.debug("Testing for opponent class");
            String newClass = imageIdentifyOpponentClass(image);
            if (newClass != null) {
                setOpponentClass(newClass);
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
                setYourTurn(false);
            }
        } else {
            // Last time we checked it was the opponent's turn, check whether it's now your turn
            debugLog.debug("Testing for your turn");
            if (imageShowsYourTurn(image)) {
                setYourTurn(true);
            }
        }
    }


    private void testForVictory(BufferedImage image) {
        debugLog.debug("Testing for victory");
        if (!victoryOrDefeatDetected) {
            if (imageShowsVictory(image)
                    ) {
                endTimer();
                victoryOrDefeatDetected = true;     // Set to true to prevent match results being submitted multiple times
                setResult("Victory");
            }
        }
    }


    private void testForDefeat(BufferedImage image) {
        debugLog.debug("Testing for defeat");
        if (!victoryOrDefeatDetected) {
            if (imageShowsDefeat(image)) {
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


    boolean imageShowsVictory(BufferedImage image) {
        return individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.VICTORY_1A, UniquePixel.VICTORY_1B, UniquePixel.VICTORY_1C })
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.VICTORY_2A, UniquePixel.VICTORY_2B, UniquePixel.VICTORY_2C })
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.VICTORY_3A, UniquePixel.VICTORY_3B, UniquePixel.VICTORY_3C });
    }


    boolean imageShowsDefeat(BufferedImage image) {
        return individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DEFEAT_1A, UniquePixel.DEFEAT_1B, UniquePixel.DEFEAT_1C})
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DEFEAT_2A, UniquePixel.DEFEAT_2B, UniquePixel.DEFEAT_2C})
                || individualPixelAnalyser.testAllPixelsMatch(image, new UniquePixel[]{
                UniquePixel.DEFEAT_3A, UniquePixel.DEFEAT_3B, UniquePixel.DEFEAT_3C});
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
        float ratio = getRatio(image);
        int xOffset = getXOffset(image, ratio);

        int retryOffset = (analyzeRankRetries - 1) % 3;
        int x = (int) ((875 + retryOffset) * ratio + xOffset);
        int y = (int) (162 * ratio);
        int width = (int) (32 * ratio);
        int height = (int) (22 * ratio);

        String rankStr = performOcr(x, y, width, height, "ranklevel.jpg");
        debugLog.debug("Rank str: " + rankStr);
        if(rankStr != null) {
            rankStr = rankStr.replaceAll("l", "1");
            rankStr = rankStr.replaceAll("I", "1");
            rankStr = rankStr.replaceAll("i", "1");
            rankStr = rankStr.replaceAll("S", "5");
            rankStr = rankStr.replaceAll("O", "0");
            rankStr = rankStr.replaceAll("o", "0");
            rankStr = rankStr.replaceAll("[^\\d.]", "");
        }
        debugLog.debug("Rank str parsed: " + rankStr);

        if(rankStr != null && !rankStr.isEmpty() && Integer.parseInt(rankStr) != 0 && Integer.parseInt(rankStr) < 26) {
            setRankLevel(Integer.parseInt(rankStr));
        } else if (analyzeRankRetries < 5) {	// retry up to 5 times
            analyzeRankRetries++;
            debugLog.debug("rank detection try #" + analyzeRankRetries);
            analyzeRankLevel(image);
        }
    }


    private void analyseOpponentName(BufferedImage image) {
        float ratio = getRatio(image);

        int x = (int) ((getMode() == "Ranked" ? 76 : 6) * ratio);
        int y = (int) (34 * ratio);
        int width = (int) (150 * ratio);
        int height = (int) (19 * ratio);

        OCR.setLang("eng");
        setOpponentName(performOcr(x, y, width, height, "opponentname.jpg"));
    }


    private String performOcr(int x, int y, int width, int height, String output) {
        int bigWidth = width * 3;
        int bigHeight = height * 3;

        // get cropped image of name
        BufferedImage opponentNameImg = lastImage.getSubimage(x, y, width, height);

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
        try {
            RescaleOp rescaleOp = new RescaleOp(1.8f, -30, null);
            rescaleOp.filter(newImage, newImage);  // Source and destination are the same.
        } catch(Exception e) {
            Main.showErrorDialog("Error rescaling opponent name image", e);
            notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE);
        }
        // save it to a file
        File outputfile = new File(Main.getExtractionFolder() + "/" + output);
        try {
            ImageIO.write(newImage, "jpg", outputfile);
        } catch (Exception e) {
            Main.showErrorDialog("Error writing opponent name image", e);
        }

        try {
            String ocrString = OCR.process(newImage);
            return ocrString == null ? "" : ocrString.replaceAll("\\s+","");
        } catch(Exception e) {
            Main.showErrorDialog("Error trying to analyze opponent name image", e);
            notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE);
        }
        return null;
    }


    public void resetMatch() {
        match = new HearthstoneMatch();
    }


    public void reset() {
        resetMatch();
        screen = null;
        analyzeRankRetries = 0;
        isYourTurn = false;
        arenaRunEndDetected = false;
    }


    public boolean getCoin() {
        return match.hasCoin();
    }


    public HearthstoneMatch getMatch() {
        return match;
    }


    public int getDeckSlot() {
        return match.getDeckSlot();
    }


    public String getMode() {
        return match.getMode();
    }


    public String getOpponentClass() {
        return match.getOpponentClass();
    }


    public String getOpponentName() {
        return match.getOpponentName();
    }


    public Integer getRankLevel() {
        return match.getRankLevel();
    }


    public String getResult() {
        return match.getResult();
    }


    public String getYourClass() {
        return match.getUserClass();
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
        match.setDeckSlot(deckSlot);
        notifyObserversOfChangeTo(AnalyserEvent.DECK_SLOT);
    }


    private void setCoin(boolean coin) {
        match.setCoin(coin);
        notifyObserversOfChangeTo(AnalyserEvent.COIN);
    }


    private void setMode(String mode) {
        if (!StringUtils.equals(this.mode, mode)) {
            debugLog.debug("Mode changed from {} to {}", this.mode, mode);

            this.mode = mode;
            match.setMode(mode);

            if ("Ranked".equals(mode)) {
                analyzeRankLevel(lastImage);
            }

            notifyObserversOfChangeTo(AnalyserEvent.MODE);
        }
    }


    private void setOpponentClass(String opponentClass) {
        match.setOpponentClass(opponentClass);
        notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_CLASS);
    }


    private void setRankLevel(Integer rankLevel) {
        this.rankLevel = rankLevel;
        match.setRankLevel(rankLevel);
    }


    private void setOpponentName(String opponentName) {
        match.setOpponentName(opponentName);
        notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_NAME);
    }


    private void setResult(String result) {
        match.setResult(result);
        notifyObserversOfChangeTo(AnalyserEvent.RESULT);
    }


    private void setScreen(Screen screen) {
        this.screen = screen;

        notifyObserversOfChangeTo(AnalyserEvent.SCREEN);
    }


    private void setYourClass(String yourClass) {
        match.setUserClass(yourClass);
        notifyObserversOfChangeTo(AnalyserEvent.YOUR_CLASS);
    }


    private void setYourTurn(boolean yourTurn) {
        this.isYourTurn = yourTurn;
        if (yourTurn) {
            match.setNumTurns(match.getNumTurns() + 1);
        }
        notifyObserversOfChangeTo(AnalyserEvent.YOUR_TURN);
    }


    private void startTimer() {
        startTime = System.currentTimeMillis();
    }


    private void endTimer() {
        match.setDuration(Math.round((System.currentTimeMillis() - startTime) / 1000));
    }


    private void notifyObserversOfChangeTo(AnalyserEvent property) {
        setChanged();
        notifyObservers(property);
    }


    static private float getRatio(BufferedImage image) {
        return image.getHeight() / (float) 768;
    }


    static private int getXOffset(BufferedImage image, float ratio) {
        return (int) (((float) image.getWidth() - (ratio * 1024)) / 2);
    }


}
