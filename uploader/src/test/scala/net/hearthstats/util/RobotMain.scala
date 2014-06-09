package net.hearthstats.util

import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import net.hearthstats.ProgramHelper
import net.hearthstats.Config
import java.awt.Rectangle
import net.hearthstats.DeckUtils

object RobotMain extends App {
  val helper = Config.programHelper
  val rect = if (helper.foundProgram)
    helper.getHSWindowBounds
  else
    new Rectangle(0, 0, 1680, 1050)

  val robot = new HsRobot(rect, 1)
  val deck = DeckUtils.getDeck(32521)
  robot.create(deck)

}