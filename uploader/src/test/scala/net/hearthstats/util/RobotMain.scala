package net.hearthstats.util

import net.hearthstats.{ProgramHelper, OldConfig, DeckUtils}
import java.awt.Rectangle

object RobotMain extends App {
  val helper = Class.forName("net.hearthstats.win.ProgramHelperWindows").newInstance.asInstanceOf[ProgramHelper]

  val rect = if (helper.foundProgram)
    helper.getHSWindowBounds
  else
    new Rectangle(0, 0, 1680, 1050)

  val robot = new HsRobot(rect, 1)
  val deck = DeckUtils.getDeck(32521)
  robot.create(deck)

}