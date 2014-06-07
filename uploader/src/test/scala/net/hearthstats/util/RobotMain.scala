package net.hearthstats.util

import java.awt.Robot
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

object RobotMain extends App {
  val (x, y) = (0, 0)
  val (width, height) = (1680, 1050)

  val robot = new HsRobot(x, y, width, height)
  robot.add("Si:7 Agent")

}