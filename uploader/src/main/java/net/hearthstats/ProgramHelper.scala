package net.hearthstats

import java.util.Observable
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileWriter
import net.hearthstats.log.Log

/**
 * Abstract class that finds the Hearthstone program and takes screenshots of it.
 * A separate implementation of this class is needed for each operating system because the implementations rely on native system calls.
 */
abstract class ProgramHelper extends Observable {

  /**
   * Is Hearthstone found?
   *
   * @return Whether or not the program is found
   */
  def foundProgram: Boolean

  /**
   * Takes a screenshot of the Hearthstone window.
   *
   * @return An image of the Hearthstone window, or null if not running or not available.
   */
  def getScreenCapture: BufferedImage

  def hearthstoneFolder: String

  def hearthstoneLogFile: String

  protected def _notifyObserversOfChangeTo(property: String): Unit = {
    setChanged()
    notifyObservers(property)
  }

  def createConfig(): Unit = {
    val logFile = new File(s"$hearthstoneFolder/log.config")
    if (logFile.exists)
      Log.info(s"$logFile already exists")
    else {
      val writer = new FileWriter(logFile)
      writer.write(configContent)
      writer.close()
      Log.info(s"$logFile created")
    }
  }

  val configContent = """
[Zone]
LogLevel=1
FilePrinting=false
ConsolePrinting=true
ScreenPrinting=false    
"""
}

