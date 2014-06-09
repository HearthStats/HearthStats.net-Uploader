package net.hearthstats.util

import java.awt.Desktop
import java.net.URI
import java.io.IOException
import net.hearthstats.Main

object Browse {
  def apply(url: String): Unit =
    try {
      Desktop.getDesktop.browse(new URI(url))
    } catch {
      case e1: IOException =>
        Main.showErrorDialog("Error launching browser with URL" + url, e1)
    }
}