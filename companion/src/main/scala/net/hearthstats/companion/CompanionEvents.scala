package net.hearthstats.companion

import java.awt.image.BufferedImage
import scala.util.control.NonFatal
import grizzled.slf4j.Logging
import net.hearthstats.game.GameEvents.screenToObject
import net.hearthstats.game.Screen
import net.hearthstats.game.Screen.FINDING_OPPONENT
import net.hearthstats.game.Screen.PLAY_LOBBY
import net.hearthstats.game.ScreenEvent
import net.hearthstats.game.imageanalysis.IndividualPixelAnalyser
import net.hearthstats.game.imageanalysis.ScreenAnalyser
import net.hearthstats.game.imageanalysis.UniquePixel
import rx.lang.scala.JavaConversions.toScalaObservable
import rx.lang.scala.Observable
import rx.subjects.PublishSubject
import net.hearthstats.ProgramHelper

class CompanionEvents(
  companionState: CompanionState,
  individualPixelAnalyser: IndividualPixelAnalyser,
  programHelper: ProgramHelper,
  screenAnalyser: ScreenAnalyser) extends Logging {

  val subject = PublishSubject.create[Boolean]
  val hsFound: Observable[Boolean] = subject.asObservable

  val gameImages: Observable[BufferedImage] =
    hsFound.map { found =>
      if (found)
        Some(programHelper.getScreenCapture)
      else
        None
    }.filter(_.isDefined).map(_.get)

  val gameEvents: Observable[ScreenEvent] = gameImages.
    map(eventFromImage).
    filter(_.isDefined).
    map(_.get)

  private def eventFromImage(bi: BufferedImage): Option[ScreenEvent] = try {
    import companionState._
    if (iterationsSinceScreenMatched > 10) { lastScreen = None }
    Option(screenAnalyser.identifyScreen(bi, lastScreen.getOrElse(null))) match {
      case Some(screen) =>
        iterationsSinceScreenMatched = 0
        val e = eventFromScreen(screen, bi)
        debug(s"screen $screen => event $e")
        e
      case None =>
        info(s"no screen match on image, last match was $lastScreen $iterationsSinceScreenMatched iterations ago")
        iterationsSinceScreenMatched += 1
        None
    }
  } catch {
    case NonFatal(e) => error(e.getMessage, e); None
  }

  private def eventFromScreen(newScreen: Screen, image: BufferedImage): Option[ScreenEvent] = {
    import companionState._
    import net.hearthstats.game.GameEvents._

    if (newScreen == PLAY_LOBBY && individualPixelAnalyser.testAllPixelsMatch(image, UniquePixel.allBackgroundPlay))
      //      Sometimes the OS X version captures a screenshot where, apparently, Hearthstone hasn't finished compositing the screen
      //    and so we only get the background. This can happen whenever there is something layered over the main screen, for example
      //    during the 'Finding Opponent', 'Victory' and 'Defeat' screens.</p>
      //   At the moment I haven't worked out how to ensure we always get the completed screen. So this method detects when
      //    we've received and incomplete play background instead of the 'Finding Opponent' screen, so we can reject it and try again.</p>
      None
    else if (lastScreen == FINDING_OPPONENT && iterationsSinceFindingOpponent < 5) {
      iterationsSinceFindingOpponent += 1
      None
    } else {
      iterationsSinceFindingOpponent = 0
      Some(ScreenEvent(newScreen, image))
    }
  }
}