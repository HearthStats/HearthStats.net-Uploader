package net.hearthstats.companion

import java.awt.image.BufferedImage
import net.hearthstats.game.GameEvent
import grizzled.slf4j.Logging
import net.hearthstats.game.Screen
import net.hearthstats.game.Screen._
import net.hearthstats.game.ScreenGroup._
import net.hearthstats.game.imageanalysis.ScreenAnalyser
import net.hearthstats.game.imageanalysis.RelativePixelAnalyser
import net.hearthstats.game.ScreenGroup
import net.hearthstats.game.imageanalysis.IndividualPixelAnalyser
import net.hearthstats.ui.log.Log
import net.hearthstats.game.ArenaRunEnd
import net.hearthstats.game.StartingHand
import net.hearthstats.game.FirstTurn
import net.hearthstats.game.imageanalysis.UniquePixel._
import net.hearthstats.game.imageanalysis.UniquePixel
import net.hearthstats.core.GameMode

/**
 * Current perception of HearthStone game by the companion.
 * Also capable of converting screenshots to game events and update its internal state.
 */
class CompanionState extends Logging {

  val screenAnalyser = new ScreenAnalyser
  val individualPixelAnalyser = new IndividualPixelAnalyser
  val relativePixelAnalyser = new RelativePixelAnalyser

  var mode: Option[GameMode] = None
  var deckSlot: Option[Int] = None
  var lastScreen: Option[Screen] = None

  var iterationsSinceFindingOpponent = 0
  var iterationsSinceClassCheckingStarted = 0
  var iterationsSinceScreenMatched = 0
  var iterationsSinceYourTurn = 0
  var iterationsSinceOpponentTurn = 0

  //  var isNewArena: Boolean = false
  //  var arenaRunEndDetected = false
  //  var victoryOrDefeatDetected = false
  //
  //  var isYourTurn = true

  /**
   * Also updates the current state.
   */
  def eventFromImage(bi: BufferedImage): Option[GameEvent] = {
    if (iterationsSinceScreenMatched > 10)
      lastScreen = None
    Option(screenAnalyser.identifyScreen(bi, lastScreen.getOrElse(null))) match {
      case Some(screen) =>
        iterationsSinceScreenMatched = 0
        eventFromScreen(screen, bi)
      case None =>
        iterationsSinceScreenMatched += 1
        None
    }
  }

  private def eventFromScreen(newScreen: Screen, image: BufferedImage): Option[GameEvent] =
    if (lastScreen.isEmpty || lastScreen.get != newScreen) {
      debug(s"Screen changed from $lastScreen to $newScreen")
      if (newScreen == PLAY_LOBBY && imageShowsPlayBackground(image))
        None
      else if (lastScreen == FINDING_OPPONENT && iterationsSinceFindingOpponent < 5) {
        iterationsSinceFindingOpponent += 1
        None
      } else {
        iterationsSinceFindingOpponent = 0
        val screenToEvent: PartialFunction[Screen, GameEvent] = _ match {
          case Screen.ARENA_END => ArenaRunEnd
          case s if s.group == MATCH_START => StartingHand
          case s if s.group == MATCH_PLAYING => FirstTurn
        }
        if (screenToEvent.isDefinedAt(newScreen)) {
          lastScreen = Some(newScreen)
        }
        screenToEvent.lift(newScreen)
      }
    } else None

  /**
   * <p>Sometimes the OS X version captures a screenshot where, apparently, Hearthstone hasn't finished compositing the screen
   * and so we only get the background. This can happen whenever there is something layered over the main screen, for example
   * during the 'Finding Opponent', 'Victory' and 'Defeat' screens.</p>
   * <p>At the moment I haven't worked out how to ensure we always get the completed screen. So this method detects when
   * we've receive and incomplete play background instead of the 'Finding Opponent' screen, so we can reject it and try again.</p>
   * @param image
   * @return true if this screenshot shows a background image that should be ignored
   */
  private def imageShowsPlayBackground(image: BufferedImage): Boolean =
    individualPixelAnalyser.testAllPixelsMatch(image, UniquePixel.allBackgroundPlay)

}