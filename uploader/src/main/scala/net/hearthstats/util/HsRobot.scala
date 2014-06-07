package net.hearthstats.util

import java.awt.Robot
import java.awt.event.KeyEvent._
import java.awt.event.InputEvent

class HsRobot(
  x: Int, // top left corner
  y: Int,
  width: Int,
  height: Int) {

  val robot = new Robot
  robot.setAutoDelay(1)

  val resolution = Res16_9 //TODO detect resolution

  def add(cardName: String): Unit = {
    click(resolution.search)
    send(cardName + "\n")
    robot.delay(100)
    click(resolution.card)
  }

  def click(p: Point): Unit = {
    robot.mouseMove(p.x, p.y)
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
  }

  def send(s: String): Unit =
    for {
      char <- s.toLowerCase
      code <- mapKey(char)
    } pressAndRelease(code)

  private def mapKey(c: Char): Option[Int] = c match {
    case ' ' => Some(VK_SPACE)
    case '\n' => Some(VK_ENTER)
    case alpha if 'a' <= c && 'z' >= c => Some(c - 'a' + VK_A)
    case _ => None
  }

  private def pressAndRelease(code: Int): Unit = {
    robot.keyPress(code)
    robot.keyRelease(code)
  }

  sealed trait Resolution {
    def search: Point = applyRatio(searchRatio)
    def card: Point = applyRatio(cardRatio)
    def searchRatio: (Float, Float)
    def cardRatio: (Float, Float)

    private def applyRatio(r: (Float, Float)) = {
      val (a, b) = r
      Point(x + a * width, y + b * height)
    }
  }

  case object Res16_9 extends Resolution {
    val searchRatio = (425f / 900, 82f / 90)
    val cardRatio = (15f / 80, 13f / 40)
  }
  case object Res4_3 extends Resolution {
    val searchRatio = (0.48f, 0.915f)
    val cardRatio = (0.12f, 0.31f)
  }
}