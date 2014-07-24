package net.hearthstats

import java.util.Observable
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileWriter
import java.awt.Rectangle

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

  protected def _notifyObserversOfChangeTo(property: String): Unit = {
    setChanged()
    notifyObservers(property)
  }

  def getHSWindowBounds: Rectangle

  /**
   * Returns true if config was created.
   */
  //  def createConfig(environment: Environment): Boolean = {
  //    val logConfigFile = new File(s"${environment.hearthstoneConfigFolder}/log.config")
  //    if (logConfigFile.exists) {
  //      Log.info(s"Using existing Hearthstone log config $logConfigFile")
  //      false
  //    } else {
  //      val writer = new FileWriter(logConfigFile)
  //      writer.write(configContent)
  //      writer.close()
  //      Log.info(s"Created new Hearthstone log config $logConfigFile")
  //      true
  //    }
  //  }

  var configContent =
    """[LoadingScreen]
      |LogLevel=1
      |FilePrinting=false
      |ConsolePrinting=true
      |ScreenPrinting=false
      |
      |[Zone]
      |LogLevel=1
      |FilePrinting=false
      |ConsolePrinting=true
      |ScreenPrinting=false
    """.stripMargin

}

