package net.hearthstats.util

import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import net.hearthstats.ProgramHelper
import net.hearthstats.Config
import java.awt.Rectangle

object RobotMain extends App {
  val helper = Config.programHelper
  val rect = if (helper.foundProgram)
    helper.getHSWindowBounds
  else
    new Rectangle(0, 0, 1680, 1050)

  val robot = new HsRobot(rect)
  robot.add("Si:7 Agent")

}