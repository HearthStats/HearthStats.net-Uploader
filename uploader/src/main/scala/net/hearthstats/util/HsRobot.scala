package net.hearthstats.util

import java.awt.{Rectangle, Robot}
import java.awt.event.InputEvent
import java.awt.event.KeyEvent._

import grizzled.slf4j.Logging
import net.hearthstats.Deck

case class HsRobot(hsWindow: Rectangle, delayRatio: Int = 2) extends Logging {

  val robot = new Robot

  val shortDelay = 10 * delayRatio
  val mediumDelay = 100 * delayRatio

  val replacements = Map(
    "The Beast" -> "The Beast Deathrattle",
    "Slam" -> "Slam if it survives",
    "Windfury" -> "Windfury GIVE")

  def create(deck: Deck): Unit = {
    // A click on the search field is necessary before constructing the deck to ensure that the window has focus.
    click(resolution.search)
    for (card <- deck.cards) {
      add(card.name, card.count)
      robot.delay(mediumDelay)
    }
  }

  def collectionScrollAway(): Unit = {
    var i = 0
    click(resolution.collectionScroll)
    robot.delay(mediumDelay)
    for (i <- 1 to 4) {
      scrollAway()
    }
  }

  def collectionScrollTowards(): Unit = {
    var i = 0
    click(resolution.collectionScroll)
    robot.delay(mediumDelay)
    for (i <- 1 to 4) {
      scrollTowards()
    }
  }

  def add(cardName: String, times: Int = 1): Unit = {
    click(resolution.search)
    robot.delay(mediumDelay)
    val searchText = replacements.get(cardName).getOrElse(cardName)
    send(searchText + "\n")
    robot.delay(mediumDelay)
    for (i <- 1 to times) {
      click(resolution.card)
      robot.delay(mediumDelay)
    }
  }

  def click(p: Point): Unit = {
    logger.debug(s"Clicked on ${p.x},${p.y}")
    robot.mouseMove(p.x, p.y)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
  }

  def drag(from: Point, to: Point): Unit = {
    logger.debug(s"Dragged from  ${from.x},${from.y} to ${to.x},${to.y})")
    robot.mouseMove(from.x, from.y)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    robot.mouseMove(to.x, to.y)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
  }

  /**
   * Scroll the mouse wheel away from user
   * (usually correlates to 'up' on Windows and 'down' on OS X)
   */
  private def scrollAway(): Unit = {
    robot.mouseWheel(-5);
    robot.delay(shortDelay)
  }

  /**
   * Scroll the mouse wheel towards the user
   * (usually correlates to 'down' on Windows and 'up' on OS X)
   */
  private def scrollTowards(): Unit = {
    robot.mouseWheel(5);
    robot.delay(shortDelay)
  }

  def send(s: String): Unit =
    for {
      char <- s.toLowerCase
      code <- mapKey(char)
    } pressAndRelease(code)

  private def mapKey(c: Char): Option[Int] = c match {
    case ' ' => Some(VK_SPACE)
    case '-' => Some(VK_SPACE)
    case ''' => Some(VK_SPACE)
    case '\n' => Some(VK_ENTER)
    case alpha if 'a' <= c && 'z' >= c => Some(c - 'a' + VK_A)
    case _ => None
  }

  private def pressAndRelease(code: Int): Unit = {
    robot.keyPress(code)
    robot.keyRelease(code)
  }

  lazy val resolution: Resolution = {
    import scala.math._
    val ratio = hsWindow.width.toFloat / hsWindow.height
    def score(r: Resolution) =
      abs(log(ratio / r.ratio))
    Seq(Res16_9, Res4_3).sortBy(score).head
  }

  sealed trait Resolution {
    def search: Point = applyRatio(searchRatio)
    def card: Point = applyRatio(cardRatio)
    def collectionScroll: Point = applyRatio(collectionScrollRatio)

    def searchRatio: (Float, Float)
    def cardRatio: (Float, Float)
    def collectionScrollRatio: (Float, Float)
    def ratio: Float

    def applyRatio(r: (Float, Float)) = {
      import hsWindow._
      val (a, b) = r
      Point(x + a * width, y + b * height)
    }
  }

  case object Res16_9 extends Resolution {
    val searchRatio = (425f / 900, 82f / 90)
    val cardRatio = (15f / 80, 13f / 40)
    val collectionScrollRatio = (0.90f, 0.05f)
    val ratio = 16f / 9
  }
  case object Res4_3 extends Resolution {
    val searchRatio = (0.48f, 0.915f)
    val cardRatio = (0.12f, 0.31f)
    val collectionScrollRatio = (0.98f, 0.05f)
    val ratio = 4f / 3
  }
}