package net.hearthstats.analysis

import java.awt.image.BufferedImage
import java.text.MessageFormat
import java.util.Observable
import java.util.ResourceBundle
import org.apache.commons.lang3.StringUtils
import grizzled.slf4j.Logging
import net.hearthstats.BackgroundImageSave
import net.hearthstats.Config
import net.hearthstats.HearthstoneMatch
import net.hearthstats.Main
import net.hearthstats.log.Log
import net.hearthstats.ocr.OcrException
import net.hearthstats.ocr.OpponentNameRankedOcr
import net.hearthstats.ocr.OpponentNameUnrankedOcr
import net.hearthstats.ocr.RankLevelOcr
import net.hearthstats.state.PixelLocation
import net.hearthstats.state.Screen
import net.hearthstats.state.Screen.ARENA_LOBBY
import net.hearthstats.state.Screen.MATCH_STARTINGHAND
import net.hearthstats.state.Screen.MATCH_VS
import net.hearthstats.state.Screen.PLAY_LOBBY
import net.hearthstats.state.Screen.PRACTICE_LOBBY
import net.hearthstats.state.ScreenGroup
import net.hearthstats.state.ScreenGroup.MATCH_END
import net.hearthstats.state.ScreenGroup.MATCH_PLAYING
import net.hearthstats.state.ScreenGroup.MATCH_START
import net.hearthstats.state.UniquePixel._
import net.hearthstats.util.MatchOutcome
import net.hearthstats.util.Rank
import net.hearthstats.state.UniquePixel

/**
 * The main analyser for Hearthstone. Uses screenshots to determine what state the game is in,
 * and updates the match records appropriately.
 *
 * @author gtch
 */
object HearthstoneAnalyser extends Observable with Logging {

  def getRatio(image: BufferedImage): Float =
    image.getHeight / PixelLocation.REFERENCE_SIZE.y.toFloat

  def getXOffset(image: BufferedImage, ratio: Float): Int =
    ((image.getWidth.toFloat - (ratio * PixelLocation.REFERENCE_SIZE.x)) / 2).toInt

  private val bundle = ResourceBundle.getBundle("net.hearthstats.resources.Main")

  private val screenAnalyser = new ScreenAnalyser
  private val individualPixelAnalyser = new IndividualPixelAnalyser
  private val relativePixelAnalyser = new RelativePixelAnalyser
  private val opponentNameRankedOcr = new OpponentNameRankedOcr
  private val opponentNameUnrankedOcr = new OpponentNameUnrankedOcr
  private val rankLevelOcr = new RankLevelOcr

  private var lastImage: BufferedImage = _

  var screen: Screen = null

  var isNewArena: Boolean = false

  private var arenaRunEndDetected: Boolean = false

  private var victoryOrDefeatDetected: Boolean = false

  var isYourTurn: Boolean = true

  var hsMatch: HearthstoneMatch = new HearthstoneMatch()

  private var mode: String = _

  private var deckSlot: Int = _

  private var rankLevel: Rank = _

  private var startTime: Long = _

  private var iterationsSinceFindingOpponent: Int = 0
  private var iterationsSinceClassCheckingStarted: Int = 0
  private var iterationsSinceScreenMatched: Int = 0
  private var iterationsSinceYourTurn: Int = 0
  private var iterationsSinceOpponentTurn: Int = 0

  def analyze(image: BufferedImage) {
    if (lastImage != null) {
      lastImage.flush()
    }
    lastImage = image
    val matchedScreen =
      if (iterationsSinceScreenMatched < 10) screenAnalyser.identifyScreen(image, screen)
      else screenAnalyser.identifyScreen(image, null)
    if (matchedScreen == null)
      iterationsSinceScreenMatched += 1
    else {
      iterationsSinceScreenMatched = 0
      val screenChangedOK = handleScreenChange(image, screen, matchedScreen)
      if (screenChangedOK)
        handleScreenActions(image, matchedScreen)
      else
        debug(s"Ignored screen $matchedScreen because it was determined to be invalid")
    }
  }

  private def handleScreenActions(image: BufferedImage, newScreen: Screen) {
    if (newScreen != null) {
      debug(s"Screen being processed $newScreen")
      newScreen match {
        case PLAY_LOBBY =>
          testForCasualOrRanked(image)
          testForDeckSlot(image)

        case PRACTICE_LOBBY =>
          setMode("Practice")

        case ARENA_LOBBY =>
          setMode("Arena")
          testForNewArenaRun(image)

        case MATCH_VS =>
          testForYourClass(image)
          testForOpponentClass(image)
          testForCoin(image)
          testForOpponentName(image)
          iterationsSinceClassCheckingStarted += 1

        case MATCH_STARTINGHAND =>
          testForCoin(image)
          testForOpponentName(image)

        case _ =>
      }
      if ("Practice" != getMode) newScreen.group match {
        case MATCH_PLAYING => testForOpponentOrYourTurn(image)
        case MATCH_END => testForVictoryOrDefeat(image)
        case _ =>
      }
      if (victoryOrDefeatDetected && newScreen.group != ScreenGroup.MATCH_END)
        victoryOrDefeatDetected = false
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
  private def handleScreenChange(image: BufferedImage, previousScreen: Screen, newScreen: Screen): Boolean = {
    if (newScreen != null && newScreen != previousScreen) {
      debug(s"Screen changed from $previousScreen to $newScreen")
      if (newScreen == Screen.PLAY_LOBBY) {
        if (imageShowsPlayBackground(image)) {
          return false
        }
        if (previousScreen == Screen.FINDING_OPPONENT) {
          if (iterationsSinceFindingOpponent < 5) {
            iterationsSinceFindingOpponent += 1
            return false
          } else
            iterationsSinceFindingOpponent = 0
        }
      } else
        iterationsSinceFindingOpponent = 0

      if (newScreen == Screen.ARENA_END)
        setArenaRunEnd()
      newScreen.group match {
        case MATCH_START =>
          hsMatch = new HearthstoneMatch
          hsMatch.mode = mode
          hsMatch.deckSlot = deckSlot
          hsMatch.rankLevel = rankLevel
          arenaRunEndDetected = false
          isYourTurn = false
          iterationsSinceClassCheckingStarted = 0
          iterationsSinceYourTurn = 0
          iterationsSinceOpponentTurn = 0

        case MATCH_PLAYING =>
          startTimer()
          if (previousScreen != null && previousScreen.group == ScreenGroup.MATCH_START &&
            (hsMatch.opponentClass == null || hsMatch.userClass == null)) {
            Log.warn(t("warning.classdetection", Config.getExtractionFolder))
          }

        case MATCH_END =>

        case _ =>
      }
      setScreen(newScreen)
    }
    true
  }

  private def testForNewArenaRun(image: BufferedImage) {
    if (!isNewArena) {
      debug("Testing for new arena run")
      if (individualPixelAnalyser.testAllPixelsMatch(image, Array(NEW_ARENA_RUN_A, NEW_ARENA_RUN_B, NEW_ARENA_RUN_C, NEW_ARENA_RUN_D, NEW_ARENA_RUN_E)))
        setIsNewArena(true)
    }
  }

  private def testForCasualOrRanked(image: BufferedImage) {
    if ("Casual" != getMode) {
      debug("Testing for casual mode")
      if (imageShowsCasualPlaySelected(image))
        setMode("Casual")
    }
    if ("Ranked" != getMode) {
      debug("Testing for ranked mode")
      if (imageShowsRankedPlaySelected(image)) {
        analyzeRankLevel(image)
        setMode("Ranked")
      }
    }
  }

  private def testForDeckSlot(image: BufferedImage) {
    debug("Testing for deck slot")
    imageIdentifyDeckSlot(image) match {
      case Some(newDeckSlot) if newDeckSlot != getDeckSlot =>
        setDeckSlot(newDeckSlot)
      case None =>
    }
  }

  private def testForYourClass(image: BufferedImage) {
    if (getYourClass == null) {
      debug("Testing for your class")
      imageIdentifyYourClass(image) match {
        case Some(newClass) => setYourClass(newClass)
        case None =>
      }
      if (iterationsSinceClassCheckingStarted > 3 && (iterationsSinceClassCheckingStarted & 3) == 0) {
        val filename = "class-yours-" + (iterationsSinceClassCheckingStarted >> 2)
        BackgroundImageSave.saveCroppedPngImage(image, filename, 204, 600, 478, 530)
      }
    }
  }

  private def testForOpponentClass(image: BufferedImage) {
    if (getOpponentClass == null) {
      debug("Testing for opponent class")
      imageIdentifyOpponentClass(image) match {
        case Some(newClass) => setOpponentClass(newClass)
        case None =>
      }
      if (iterationsSinceClassCheckingStarted > 3 && (iterationsSinceClassCheckingStarted & 3) == 0) {
        val filename = "class-opponent-" + (iterationsSinceClassCheckingStarted >> 2)
        BackgroundImageSave.saveCroppedPngImage(image, filename, 1028, 28, 478, 530)
      }
    }
  }

  private def testForOpponentName(image: BufferedImage) {
    if (getOpponentName == null) {
      debug("Testing for opponent name")
      if (imageShowsOpponentName(image))
        analyseOpponentName(image)
    }
  }

  private def testForCoin(image: BufferedImage) {
    if (!getCoin) {
      debug("Testing for coin")
      if (imageShowsCoin(image))
        setCoin(true)
    }
  }

  private def testForOpponentOrYourTurn(image: BufferedImage) {
    if (isYourTurn) {
      debug("Testing for opponent turn")
      if (imageShowsOpponentTurn(image)) {
        iterationsSinceYourTurn += 1
        if (iterationsSinceYourTurn > 2) {
          setYourTurn(false)
          iterationsSinceYourTurn = 0
        }
      } else iterationsSinceYourTurn = 0
    } else {
      debug("Testing for your turn")
      if (imageShowsYourTurn(image)) {
        iterationsSinceOpponentTurn += 1
        if (iterationsSinceOpponentTurn > 2) {
          setYourTurn(true)
          iterationsSinceOpponentTurn = 0
        }
      } else iterationsSinceOpponentTurn = 0
    }
  }

  private def testForVictoryOrDefeat(image: BufferedImage) {
    if (!victoryOrDefeatDetected) {
      debug("Testing for victory or defeat")
      imageShowsVictoryOrDefeat(image) match {
        case MatchOutcome.VICTORY =>
          endTimer()
          victoryOrDefeatDetected = true
          setResult("Victory")
        case MatchOutcome.DEFEAT =>
          endTimer()
          victoryOrDefeatDetected = true
          setResult("Defeat")
        case _ =>
      }
    }
  }

  private def setArenaRunEnd() {
    if (!arenaRunEndDetected) {
      debug("Setting end of arena run")
      arenaRunEndDetected = true
      notifyObserversOfChangeTo(AnalyserEvent.ARENA_END)
    }
  }

  def imageShowsCoin(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAnyPixelsMatch(image, Array(COIN_1, COIN_2, COIN_3, COIN_4, COIN_5))

  def imageShowsCasualPlaySelected(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_CASUAL_1A, MODE_CASUAL_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_CASUAL_2A, MODE_CASUAL_2B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_CASUAL_3A, MODE_CASUAL_3B))

  def imageShowsRankedPlaySelected(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_RANKED_1A, MODE_RANKED_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_RANKED_2A, MODE_RANKED_2B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(MODE_RANKED_3A, MODE_RANKED_3B))

  def imageShowsOpponentTurn(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_OPPONENT_1A, TURN_OPPONENT_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_OPPONENT_2A, TURN_OPPONENT_2B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_OPPONENT_3A, TURN_OPPONENT_3B))

  def imageShowsYourTurn(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_YOUR_1A, TURN_YOUR_1B)) ||
      individualPixelAnalyser.testAllPixelsMatch(image, Array(TURN_YOUR_2A, TURN_YOUR_2B))

  def imageShowsOpponentName(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(NAME_OPPONENT_1A, NAME_OPPONENT_1B, NAME_OPPONENT_1C)) &&
      individualPixelAnalyser.testAllPixelsMatch(image, Array(NAME_OPPONENT_2A, NAME_OPPONENT_2B, NAME_OPPONENT_2C))

  def imageShowsVictoryOrDefeat(image: BufferedImage): MatchOutcome = {
    val referenceCoordinate =
      relativePixelAnalyser.findRelativePixel(image, VICTORY_DEFEAT_REFBOX_TL, VICTORY_DEFEAT_REFBOX_BR, 8, 11)
    if (referenceCoordinate != null) {
      val victory1Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(VICTORY_REL_1A, VICTORY_REL_1B))
      val victory2Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(VICTORY_REL_2A, VICTORY_REL_2B, VICTORY_REL_2C))
      val defeat1Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(DEFEAT_REL_1A, DEFEAT_REL_1B, DEFEAT_REL_1C, DEFEAT_REL_1D, DEFEAT_REL_1E))
      val defeat2Matches = relativePixelAnalyser.countMatchingRelativePixels(image, referenceCoordinate,
        Array(DEFEAT_REL_2A))
      val matchedVictory = victory1Matches > 0 && victory2Matches == 3 &&
        defeat1Matches == 0 && defeat2Matches == 0
      val matchedDefeat = victory1Matches == 0 && victory2Matches == 0 &&
        defeat1Matches > 0 && defeat2Matches == 1
      if (matchedVictory && matchedDefeat) {
        warn("Matched both victory and defeat, which shouldn't be possible. Will try again next iteration.")
        null
      } else if (matchedVictory) MatchOutcome.VICTORY
      else if (matchedDefeat) MatchOutcome.DEFEAT
      else null
    } else null
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
  def imageShowsPlayBackground(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, Array(BACKGROUND_PLAY_1, BACKGROUND_PLAY_2, BACKGROUND_PLAY_3, BACKGROUND_PLAY_4, BACKGROUND_PLAY_5, BACKGROUND_PLAY_6, BACKGROUND_PLAY_7, BACKGROUND_PLAY_8, BACKGROUND_PLAY_9))

  private def identify[T](image: BufferedImage, pixelRules: Iterable[(Array[UniquePixel], T)]): Option[T] =
    (for {
      (pixels, result) <- pixelRules
      if individualPixelAnalyser.testAllPixelsMatch(image, pixels)
    } yield result).headOption

  def imageIdentifyDeckSlot(image: BufferedImage): Option[Int] =
    identify(image, Seq(
      Array(DECK_SLOT_1A, DECK_SLOT_1B) -> 1,
      Array(DECK_SLOT_2A, DECK_SLOT_2B) -> 2,
      Array(DECK_SLOT_3A, DECK_SLOT_3B) -> 3,
      Array(DECK_SLOT_4A, DECK_SLOT_4B) -> 4,
      Array(DECK_SLOT_5A, DECK_SLOT_5B) -> 5,
      Array(DECK_SLOT_6A, DECK_SLOT_6B) -> 6,
      Array(DECK_SLOT_7A, DECK_SLOT_7B) -> 7,
      Array(DECK_SLOT_8A, DECK_SLOT_8B) -> 8,
      Array(DECK_SLOT_9A, DECK_SLOT_9B) -> 9))

  def imageIdentifyYourClass(image: BufferedImage): Option[String] =
    identify(image, Seq(
      Array(YOUR_DRUID_1, YOUR_DRUID_2, YOUR_DRUID_3) -> "Druid",
      Array(YOUR_HUNTER_1, YOUR_HUNTER_2, YOUR_HUNTER_3) -> "Hunter",
      Array(YOUR_HUNTER_GOLDEN_1, YOUR_HUNTER_GOLDEN_2, YOUR_HUNTER_GOLDEN_3) -> "Hunter",
      Array(YOUR_MAGE_1, YOUR_MAGE_2, YOUR_MAGE_3) -> "Mage",
      Array(YOUR_PALADIN_1, YOUR_PALADIN_2, YOUR_PALADIN_3) -> "Paladin",
      Array(YOUR_PRIEST_1, YOUR_PRIEST_2, YOUR_PRIEST_3) -> "Priest",
      Array(YOUR_ROGUE_1, YOUR_ROGUE_2, YOUR_ROGUE_3) -> "Rogue",
      Array(YOUR_SHAMAN_1, YOUR_SHAMAN_2, YOUR_SHAMAN_3) -> "Shaman",
      Array(YOUR_WARLOCK_1, YOUR_WARLOCK_2, YOUR_WARLOCK_3) -> "Warlock",
      Array(YOUR_WARRIOR_1, YOUR_WARRIOR_2, YOUR_WARRIOR_3) -> "Warrior"))

  def imageIdentifyOpponentClass(image: BufferedImage): Option[String] =
    identify(image, Seq(
      Array(OPPONENT_DRUID_1, OPPONENT_DRUID_2, OPPONENT_DRUID_3) -> "Druid",
      Array(OPPONENT_HUNTER_1, OPPONENT_HUNTER_2, OPPONENT_HUNTER_3) -> "Hunter",
      Array(OPPONENT_MAGE_1, OPPONENT_MAGE_2, OPPONENT_MAGE_3) -> "Mage",
      Array(OPPONENT_PALADIN_1, OPPONENT_PALADIN_2, OPPONENT_PALADIN_3) -> "Paladin",
      Array(OPPONENT_PRIEST_1, OPPONENT_PRIEST_2, OPPONENT_PRIEST_3) -> "Priest",
      Array(OPPONENT_ROGUE_1, OPPONENT_ROGUE_2, OPPONENT_ROGUE_3) -> "Rogue",
      Array(OPPONENT_SHAMAN_1, OPPONENT_SHAMAN_2, OPPONENT_SHAMAN_3) -> "Shaman",
      Array(OPPONENT_WARLOCK_1, OPPONENT_WARLOCK_2, OPPONENT_WARLOCK_3) -> "Warlock",
      Array(OPPONENT_WARRIOR_1, OPPONENT_WARRIOR_2, OPPONENT_WARRIOR_3) -> "Warrior"))

  private def analyzeRankLevel(image: BufferedImage) {
    try {
      val rankInteger = rankLevelOcr.processNumber(image)
      val rank = Rank.fromInt(rankInteger)
      if (rank == null) {
        Log.warn("Could not interpret rank, your rank may not be recorded correctly")
      } else {
        setRankLevel(rank)
      }
    } catch {
      case e: OcrException => {
        Main.showErrorDialog(e.getMessage, e)
        notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE)
      }
    }
  }

  private def analyseOpponentName(image: BufferedImage) {
    var opponentName: String = null
    try {
      opponentName = if ("Ranked" == getMode) opponentNameRankedOcr.process(image) else opponentNameUnrankedOcr.process(image)
      setOpponentName(opponentName)
    } catch {
      case e: OcrException => {
        Main.showErrorDialog(e.getMessage, e)
        notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE)
      }
    }
  }

  def resetMatch() {
    hsMatch = new HearthstoneMatch()
  }

  def reset() {
    resetMatch()
    screen = null
    isYourTurn = false
    arenaRunEndDetected = false
  }

  def getCoin: Boolean = hsMatch.coin
  def getDeckSlot: Int = hsMatch.deckSlot
  def getMode: String = hsMatch.mode
  def getOpponentClass: String = hsMatch.opponentClass
  def getOpponentName: String = hsMatch.opponentName
  def getRankLevel: Rank = hsMatch.rankLevel
  def getResult: String = hsMatch.result
  def getYourClass: String = hsMatch.userClass

  def setIsNewArena(isNew: Boolean) {
    isNewArena = isNew
    notifyObserversOfChangeTo(AnalyserEvent.NEW_ARENA)
  }

  private def setDeckSlot(deckSlot: Int) {
    this.deckSlot = deckSlot
    hsMatch.deckSlot = deckSlot
    notifyObserversOfChangeTo(AnalyserEvent.DECK_SLOT)
  }

  private def setCoin(coin: Boolean) {
    hsMatch.coin = coin
    notifyObserversOfChangeTo(AnalyserEvent.COIN)
  }

  private def setMode(mode: String) {
    if (!StringUtils.equals(this.mode, mode)) {
      debug(s"Mode changed from ${this.mode} to $mode")
      this.mode = mode
      hsMatch.mode = mode
      notifyObserversOfChangeTo(AnalyserEvent.MODE)
    }
  }

  private def setOpponentClass(opponentClass: String) {
    hsMatch.opponentClass = opponentClass
    notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_CLASS)
  }

  private def setRankLevel(rankLevel: Rank) {
    this.rankLevel = rankLevel
    hsMatch.rankLevel = rankLevel
  }

  private def setOpponentName(opponentName: String) {
    hsMatch.opponentName = opponentName
    notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_NAME)
  }

  private def setResult(result: String) {
    hsMatch.result = result
    notifyObserversOfChangeTo(AnalyserEvent.RESULT)
  }

  private def setScreen(screen: Screen) {
    this.screen = screen
    notifyObserversOfChangeTo(AnalyserEvent.SCREEN)
  }

  private def setYourClass(yourClass: String) {
    hsMatch.userClass = yourClass
    notifyObserversOfChangeTo(AnalyserEvent.YOUR_CLASS)
  }

  private def setYourTurn(yourTurn: Boolean) {
    this.isYourTurn = yourTurn
    if (yourTurn) {
      hsMatch.numTurns = hsMatch.numTurns + 1
    }
    notifyObserversOfChangeTo(AnalyserEvent.YOUR_TURN)
  }

  private def startTimer() {
    startTime = System.currentTimeMillis
  }

  private def endTimer() {
    hsMatch.duration = Math.round((System.currentTimeMillis - startTime) / 1000)
  }

  private def notifyObserversOfChangeTo(property: AnalyserEvent) {
    setChanged()
    notifyObservers(property)
  }

  private def t(key: String, value0: String): String = {
    val message = bundle.getString(key)
    MessageFormat.format(message, value0)
  }
}
