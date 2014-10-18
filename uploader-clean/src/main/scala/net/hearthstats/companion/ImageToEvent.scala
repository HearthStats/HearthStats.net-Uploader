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
import net.hearthstats.game.ScreenEvent
import net.hearthstats.game.FindingOpponent

/**
 * Converts screenshots to game events and updates the last screen.
 */
class ImageToEvent(
  screenAnalyser: ScreenAnalyser,
  individualPixelAnalyser: IndividualPixelAnalyser) extends Logging {

  var lastScreen: Option[Screen] = None

  var iterationsSinceFindingOpponent = 0
  var iterationsSinceClassCheckingStarted = 0
  var iterationsSinceScreenMatched = 0
  var iterationsSinceYourTurn = 0
  var iterationsSinceOpponentTurn = 0

  /**
   * Also updates the current state.
   */
  def eventFromImage(bi: BufferedImage): Option[GameEvent] = {
    if (iterationsSinceScreenMatched > 10) {
      lastScreen = None
    }
    Option(screenAnalyser.identifyScreen(bi, lastScreen.getOrElse(null))) match {
      case Some(screen) =>
        iterationsSinceScreenMatched = 0
        val e = eventFromScreen(screen, bi)
        info(s"screen $screen => event $e")
        e
      case None =>
        info(s"no screen match on image, last match was $lastScreen $iterationsSinceScreenMatched iterations ago")
        iterationsSinceScreenMatched += 1
        None
    }
  }

  def canRepeatEvent(screen: Screen) =
    (Seq(PLAY_LOBBY, PRACTICE_LOBBY, VERSUS_LOBBY) contains screen) ||
      screen.group == ScreenGroup.MATCH_END

  private def eventFromScreen(newScreen: Screen, image: BufferedImage): Option[GameEvent] =
    if (lastScreen.isEmpty || lastScreen.get != newScreen || canRepeatEvent(newScreen)) {
      if (lastScreen != Some(newScreen)) {
        debug(s"Screen : $lastScreen => $newScreen")
      }
      if (newScreen == PLAY_LOBBY && imageShowsPlayBackground(image))
        None
      else if (lastScreen == FINDING_OPPONENT && iterationsSinceFindingOpponent < 5) {
        iterationsSinceFindingOpponent += 1
        None
      } else {
        iterationsSinceFindingOpponent = 0
        val screenToEvent: PartialFunction[Screen, GameEvent] = _ match {
          case s if (Seq(PLAY_LOBBY, ARENA_LOBBY, PRACTICE_LOBBY, VERSUS_LOBBY) contains s) ||
            (s.group == ScreenGroup.MATCH_END) => ScreenEvent(s, image)
          case ARENA_END => ArenaRunEnd
          case FINDING_OPPONENT => FindingOpponent
          case s if s.group == MATCH_START => StartingHand(image)
          case s if s.group == MATCH_PLAYING => FirstTurn(image)
        }
        if (screenToEvent.isDefinedAt(newScreen) || newScreen == FINDING_OPPONENT) {
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