package net.hearthstats.companion

import java.awt.image.BufferedImage
import java.text.MessageFormat
import java.util.ResourceBundle
import org.apache.commons.lang3.StringUtils
import grizzled.slf4j.Logging
import net.hearthstats.Main
import net.hearthstats.game.MatchState

/**
 * The main analyser for Hearthstone. Uses screenshots to determine what state the game is in,
 * and publishes GameEvents accordingly.
 */
class HearthstoneAnalyser(
  imageToEvent: ImageToEvent,
  matchState: MatchState) extends Logging {

  //  private def handleScreenActions(image: BufferedImage, newScreen: Screen) {
  //    if (newScreen != null) {
  //      debug(s"Screen being processed $newScreen")
  //      newScreen match {
  //        case PLAY_LOBBY =>
  //          testForCasualOrRanked(image)
  //          testForDeckSlot(image)
  //
  //        case PRACTICE_LOBBY =>
  //          setMode("Practice")
  //
  //        case VERSUS_LOBBY =>
  //          setMode("Friendly")
  //          testForDeckSlot(image)
  //
  //        case ARENA_LOBBY =>
  //          setMode("Arena")
  //          testForNewArenaRun(image)
  //
  //        case MATCH_VS =>
  //          testForYourClass(image)
  //          testForOpponentClass(image)
  //          testForCoin(image)
  //          testForOpponentName(image)
  //          iterationsSinceClassCheckingStarted += 1
  //
  //        case MATCH_STARTINGHAND =>
  //          testForCoin(image)
  //          testForOpponentName(image)
  //
  //        case _ =>
  //      }
  //      if ("Practice" != getMode) newScreen.group match {
  //        case MATCH_PLAYING => testForOpponentOrYourTurn(image)
  //        case MATCH_END => testForVictoryOrDefeat(image)
  //        case _ =>
  //      }
  //      if (newScreen.group != ScreenGroup.MATCH_END) victoryOrDefeatDetected = false
  //    }
  //  }
  //
  //  /**
  //   * <p>Handles screen changes - determines if the screen has changes since the last iteration, and if so it performs
  //   * any actions that need to occur on the transition to a new screen.</p>
  //   * <p>If the screen has not changed then no action is taken.</p>
  //   * <p>This method may determine that a screen is actually not suitable and should be rejected. This can occur when
  //   * a screenshot has occured during compositing and so we have an incomplete screen. If this method returns false
  //   * then the current iteration should be skipped because results could be invalid.</p>
  //   *
  //   * @param image The screenshot of the new screen
  //   * @param previousScreen The previous screen, if known
  //   * @param newScreen The new screen
  //   * @return true if handled OK, false if the screen was rejected and should be skipped.
  //   */
  //  private def handleScreenChange(image: BufferedImage, previousScreen: Screen, newScreen: Screen): Boolean = {
  //    if (newScreen != null && newScreen != previousScreen) {
  //      debug(s"Screen changed from $previousScreen to $newScreen")
  //      if (newScreen == Screen.PLAY_LOBBY) {
  //        if (imageShowsPlayBackground(image))
  //          return false
  //        if (previousScreen == Screen.FINDING_OPPONENT) {
  //          if (iterationsSinceFindingOpponent < 5) {
  //            iterationsSinceFindingOpponent += 1
  //            return false
  //          } else
  //            iterationsSinceFindingOpponent = 0
  //        }
  //      } else
  //        iterationsSinceFindingOpponent = 0
  //
  //      if (newScreen == Screen.ARENA_END) { setArenaRunEnd() }
  //      newScreen.group match {
  //        case MATCH_START =>
  //          handleMatchStart()
  //          videoEncoder = VideoEncoderFactory.newVideo
  //
  //        case MATCH_PLAYING =>
  //          startTimer()
  //          if (previousScreen != null && previousScreen.group == ScreenGroup.MATCH_START &&
  //            (hsMatch.opponentClass == null || hsMatch.userClass == null)) {
  //            Log.warn(t("warning.classdetection", monitor.environment.extractionFolder))
  //          }
  //
  //        case _ =>
  //      }
  //      setScreen(newScreen)
  //    }
  //    iterationsSinceFindingOpponent = 0
  //    true
  //  }
  //
  //  def handleMatchStart(): Unit = {
  //    if (hsMatch.initialized == false) {
  //      hsMatch.initialized = true
  //      hsMatch.result = None
  //      hsMatch.mode = mode
  //      hsMatch.deckSlot = deckSlot
  //      hsMatch.rankLevel = rankLevel
  //      val heroEvents = monitor.hearthstoneLogMonitor.heroEvents
  //      heroEventSubscription.map(_.unsubscribe())
  //      heroEventSubscription = Some(heroEvents.subscribe(heroEventHandler))
  //    }
  //    arenaRunEndDetected = false
  //    isYourTurn = false
  //    iterationsSinceClassCheckingStarted = 0
  //    iterationsSinceYourTurn = 0
  //    iterationsSinceOpponentTurn = 0
  //  }
  //
  //  val heroEventHandler: HeroEvent => Unit = {
  //    case HeroDestroyedEvent(opponent) =>
  //      info("Result detected by log monitor")
  //      if (TempConfig.useLogMonitoringForGameResult) {
  //        endMatch(if (opponent) VICTORY else DEFEAT, true)
  //      }
  //  }
  //
  //  private def testForNewArenaRun(image: BufferedImage) {
  //    if (!isNewArena) {
  //      debug("Testing for new arena run")
  //      if (individualPixelAnalyser.testAllPixelsMatch(image, Array(NEW_ARENA_RUN_A, NEW_ARENA_RUN_B, NEW_ARENA_RUN_C, NEW_ARENA_RUN_D, NEW_ARENA_RUN_E)))
  //        setIsNewArena(true)
  //    }
  //  }
  //
  //  private def testForCasualOrRanked(image: BufferedImage) {
  //    if ("Casual" != getMode) {
  //      debug("Testing for casual mode")
  //      if (imageShowsCasualPlaySelected(image))
  //        setMode("Casual")
  //    }
  //    if ("Ranked" != getMode) {
  //      debug("Testing for ranked mode")
  //      if (imageShowsRankedPlaySelected(image)) {
  //        analyzeRankLevel(image)
  //        setMode("Ranked")
  //      }
  //    }
  //  }
  //
  //  private def testForDeckSlot(image: BufferedImage) {
  //    debug("Testing for deck slot")
  //    imageIdentifyDeckSlot(image) match {
  //      case Some(newDeckSlot) if newDeckSlot != getDeckSlot =>
  //        setDeckSlot(newDeckSlot)
  //      case _ =>
  //    }
  //  }
  //
  //  private def testForYourClass(image: BufferedImage) {
  //    if (getYourClass == null) {
  //      debug("Testing for your class")
  //      imageIdentifyYourClass(image) match {
  //        case Some(newClass) => setYourClass(newClass)
  //        case None =>
  //      }
  //      if (iterationsSinceClassCheckingStarted > 3 && (iterationsSinceClassCheckingStarted & 3) == 0) {
  //        val filename = "class-yours-" + (iterationsSinceClassCheckingStarted >> 2)
  //        BackgroundImageSave.saveCroppedPngImage(image, filename, 204, 600, 478, 530)
  //      }
  //    }
  //  }
  //
  //  private def testForOpponentClass(image: BufferedImage) {
  //    if (getOpponentClass == null) {
  //      debug("Testing for opponent class")
  //      imageIdentifyOpponentClass(image) match {
  //        case Some(newClass) => setOpponentClass(newClass)
  //        case None =>
  //      }
  //      if (iterationsSinceClassCheckingStarted > 3 && (iterationsSinceClassCheckingStarted & 3) == 0) {
  //        val filename = "class-opponent-" + (iterationsSinceClassCheckingStarted >> 2)
  //        BackgroundImageSave.saveCroppedPngImage(image, filename, 1028, 28, 478, 530)
  //      }
  //    }
  //  }
  //
  //  private def testForOpponentName(image: BufferedImage) {
  //    if (getOpponentName == null) {
  //      debug("Testing for opponent name")
  //      if (imageShowsOpponentName(image))
  //        analyseOpponentName(image)
  //    }
  //  }
  //
  //  private def testForCoin(image: BufferedImage) {
  //    if (!getCoin) {
  //      debug("Testing for coin")
  //      if (imageShowsCoin(image))
  //        setCoin(true)
  //    }
  //  }
  //
  //  private def testForOpponentOrYourTurn(image: BufferedImage) {
  //    if (isYourTurn) {
  //      debug("Testing for opponent turn")
  //      if (imageShowsOpponentTurn(image)) {
  //        iterationsSinceYourTurn += 1
  //        if (iterationsSinceYourTurn > 2) {
  //          setYourTurn(false)
  //          iterationsSinceYourTurn = 0
  //        }
  //      } else iterationsSinceYourTurn = 0
  //    } else {
  //      debug("Testing for your turn")
  //      if (imageShowsYourTurn(image)) {
  //        iterationsSinceOpponentTurn += 1
  //        if (iterationsSinceOpponentTurn > 2) {
  //          setYourTurn(true)
  //          iterationsSinceOpponentTurn = 0
  //        }
  //      } else iterationsSinceOpponentTurn = 0
  //    }
  //  }
  //
  //  private def testForVictoryOrDefeat(image: BufferedImage) {
  //    if (!victoryOrDefeatDetected) {
  //      info("Testing for victory or defeat")
  //      imageShowsVictoryOrDefeat(image) match {
  //        case Some(outcome) =>
  //          info("Result detected by screen capture")
  //          endMatch(outcome)
  //        case _ =>
  //      }
  //    }
  //  }
  //
  //  def endMatch(result: MatchOutcome, withCertainty: Boolean = false): Unit = {
  //    endTimer()
  //    victoryOrDefeatDetected = true
  //    setResult(result, withCertainty)
  //
  //  }
  //
  //  private def setArenaRunEnd() {
  //    if (!arenaRunEndDetected) {
  //      debug("Setting end of arena run")
  //      arenaRunEndDetected = true
  //      notifyObserversOfChangeTo(AnalyserEvent.ARENA_END)
  //    }
  //  }
  //
  //
  //
  //  private def analyzeRankLevel(image: BufferedImage) {
  //    try {
  //      val rankInteger = rankLevelOcr.processNumber(image)
  //      val rank = Rank.fromInt(rankInteger)
  //      if (rank == null) {
  //        Log.warn("Could not interpret rank, your rank may not be recorded correctly")
  //      } else {
  //        setRankLevel(rank)
  //      }
  //    } catch {
  //      case e: OcrException => {
  //        Main.showErrorDialog(e.getMessage, e)
  //        notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE)
  //      }
  //    }
  //  }
  //
  //  private def analyseOpponentName(image: BufferedImage) {
  //    var opponentName: String = null
  //    try {
  //      opponentName = if ("Ranked" == getMode) opponentNameRankedOcr.process(image) else opponentNameUnrankedOcr.process(image)
  //      setOpponentName(opponentName)
  //    } catch {
  //      case e: OcrException => {
  //        Main.showErrorDialog(e.getMessage, e)
  //        notifyObserversOfChangeTo(AnalyserEvent.ERROR_ANALYSING_IMAGE)
  //      }
  //    }
  //  }
  //
  //  def reset() {
  //    hsMatch = new HearthstoneMatch
  //    screen = null
  //    isYourTurn = false
  //    arenaRunEndDetected = false
  //  }
  //
  //  def getCoin: Boolean = hsMatch.coin
  //  def getDeckSlot: Int = hsMatch.deckSlot
  //  def getMode: String = hsMatch.mode
  //  def getOpponentClass: String = hsMatch.opponentClass
  //  def getOpponentName: String = hsMatch.opponentName
  //  def getRankLevel: Rank = hsMatch.rankLevel
  //  def getYourClass: String = hsMatch.userClass
  //
  //  def setIsNewArena(isNew: Boolean) {
  //    isNewArena = isNew
  //    notifyObserversOfChangeTo(AnalyserEvent.NEW_ARENA)
  //  }
  //
  //  private def setDeckSlot(deckSlot: Int) {
  //    this.deckSlot = deckSlot
  //    hsMatch.deckSlot = deckSlot
  //    notifyObserversOfChangeTo(AnalyserEvent.DECK_SLOT)
  //  }
  //
  //  private def setCoin(coin: Boolean) {
  //    hsMatch.coin = coin
  //    notifyObserversOfChangeTo(AnalyserEvent.COIN)
  //  }
  //
  //  private def setMode(mode: String) {
  //    if (!StringUtils.equals(this.mode, mode)) {
  //      debug(s"Mode changed from ${this.mode} to $mode")
  //      this.mode = mode
  //      hsMatch.mode = mode
  //      notifyObserversOfChangeTo(AnalyserEvent.MODE)
  //    }
  //  }
  //
  //  private def setOpponentClass(opponentClass: String) {
  //    hsMatch.opponentClass = opponentClass
  //    notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_CLASS)
  //  }
  //
  //  private def setRankLevel(rankLevel: Rank) {
  //    this.rankLevel = rankLevel
  //    hsMatch.rankLevel = rankLevel
  //  }
  //
  //  private def setOpponentName(opponentName: String) {
  //    hsMatch.opponentName = opponentName
  //    notifyObserversOfChangeTo(AnalyserEvent.OPPONENT_NAME)
  //  }
  //
  //  private def setResult(result: MatchOutcome, withCertainty: Boolean = false) {
  //    if (hsMatch.result.isEmpty) {
  //      hsMatch.result = Some(result)
  //      Log.info(s"Match result : $result")
  //      monitor.handleGameResult()
  //    } else if (withCertainty) {
  //      if (hsMatch.result.get == result) {
  //        Log.info(s"Image capture and log file agree on match result : $result")
  //      } else {
  //        Log.info(s"Log file overrides match result : $result")
  //        hsMatch.result = Some(result)
  //        monitor.handleGameResult()
  //      }
  //    }
  //  }
  //
  //  private def setScreen(screen: Screen) {
  //    this.screen = screen
  //    notifyObserversOfChangeTo(AnalyserEvent.SCREEN)
  //  }
  //
  //  private def setYourClass(yourClass: String) {
  //    hsMatch.userClass = yourClass
  //    notifyObserversOfChangeTo(AnalyserEvent.YOUR_CLASS)
  //  }
  //
  //  private def setYourTurn(yourTurn: Boolean) {
  //    this.isYourTurn = yourTurn
  //    if (yourTurn) {
  //      hsMatch.numTurns = hsMatch.numTurns + 1
  //    }
  //    notifyObserversOfChangeTo(AnalyserEvent.YOUR_TURN)
  //  }
  //
  //  private def startTimer() {
  //    startTime = System.currentTimeMillis
  //  }
  //
  //  private def endTimer() {
  //    hsMatch.duration = Math.round((System.currentTimeMillis - startTime) / 1000)
  //  }
  //
  //  private def notifyObserversOfChangeTo(property: AnalyserEvent) {
  //    setChanged()
  //    notifyObservers(property)
  //  }
  //
  //  private def t(key: String, value0: String): String = {
  //    val message = bundle.getString(key)
  //    MessageFormat.format(message, value0)
  //  }
}
