package net.hearthstone

import net.hearthstats.Config.OS._
import net.hearthstats.Config
import net.hearthstats.ProgramHelper

object ProgramHelperMain extends App {

  val className = Config.os match {
    case WINDOWS => "net.hearthstats.win.ProgramHelperWindows";
    case OSX => "net.hearthstats.osx.ProgramHelperOsx";
  }

  val helper = Class.forName(className).newInstance.asInstanceOf[ProgramHelper]
  helper.createConfig
}