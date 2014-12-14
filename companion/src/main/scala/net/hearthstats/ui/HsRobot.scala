package net.hearthstats.ui

import java.awt.event.InputEvent
import java.awt.event.KeyEvent._
import java.awt.{Rectangle, Robot}

import grizzled.slf4j.Logging
import net.hearthstats.core.Deck
import net.hearthstats.util.Coordinate

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
    robot.delay(mediumDelay)
    click(resolution.collectionScrollTop)
    robot.delay(mediumDelay)
  }

  def collectionScrollTowards(): Unit = {
    robot.delay(mediumDelay)
    click(resolution.collectionScrollBottom)
    robot.delay(mediumDelay)
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

  def click(p: Coordinate): Unit = {
    logger.debug(s"Clicked on ${p.x},${p.y}")
    robot.mouseMove(p.x, p.y)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
  }

  def drag(from: Coordinate, to: Coordinate): Unit = {
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
    //TODO : handle accents like éàè 
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

  lazy val resolution = new Resolution {
    import hsWindow._

    val extraWidth = (width.toFloat - (height.toFloat * 4 / 3)).toInt
    val xOffset = (extraWidth / 2).toInt

    def search = applyRatio(0.48f, 0.915f)
    def card = applyRatio(0.12f, 0.31f)
    def collectionScrollTop = applyRatio(0.972f, 0.054f)
    def collectionScrollBottom = applyRatio(0.972f, 0.835f)

    private def applyRatio(r: (Float, Float)) = {
      val (a, b) = r
      Coordinate(x + xOffset + a * (width - extraWidth), y + b * height)
    }
  }

  sealed trait Resolution {
    def search: Coordinate
    def card: Coordinate
    def collectionScrollTop: Coordinate
    def collectionScrollBottom: Coordinate
  }

}