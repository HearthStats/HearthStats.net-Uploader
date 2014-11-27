package net.hearthstats

import java.util.Observable
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileWriter
import java.awt.Rectangle
import net.hearthstats.config.Environment
import net.hearthstats.ui.log.Log
import java.awt.GraphicsEnvironment
import java.awt.Robot

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

  lazy val robot = new Robot
  /**
   * Takes a screenshot of the Hearthstone window.
   *
   * @return An image of the Hearthstone window, or null if not running or not available.
   */
  def getScreenCapture: BufferedImage = {
    val bounds = getHSWindowBounds
    if (isFullScreen(bounds))
      robot.createScreenCapture(bounds)
    else
      getScreenCaptureNative
  }

  def isFullScreen(rect: Rectangle): Boolean = {
    val mode = GraphicsEnvironment.getLocalGraphicsEnvironment.getDefaultScreenDevice.getDisplayMode
    val width = mode.getWidth
    val height = mode.getHeight
    rect.width >= width && rect.height >= height
  }

  def getScreenCaptureNative: BufferedImage

  protected def _notifyObserversOfChangeTo(property: String): Unit = {
    setChanged()
    notifyObservers(property)
  }

  def getHSWindowBounds: Rectangle

  def bringWindowToForeground: Boolean

  /**
   * Returns true if config was created.
   */
  def createConfig(environment: Environment, uiLog: Log): Boolean = {
    val logConfigFile = new File(s"${environment.hearthstoneConfigFolder}/log.config")
    if (logConfigFile.exists) {
      val content = io.Source.fromFile(logConfigFile).mkString
      if (content == configContent) {
        uiLog.info(s"Using existing Hearthstone log config $logConfigFile")
        false
      } else {
        createConfigImpl(logConfigFile, uiLog)
        true
      }
    } else {
      createConfigImpl(logConfigFile, uiLog)
      true
    }
  }

  private def createConfigImpl(logConfigFile: File, uiLog: Log): Unit = {
    val writer = new FileWriter(logConfigFile)
    writer.write(configContent)
    writer.close()
    uiLog.info(s"Created new Hearthstone log config $logConfigFile")
  }

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
      | 
      |[Asset]
      |LogLevel=1
      |ConsolePrinting=true
      | 
      |[Bob]
      |LogLevel=1
      |ConsolePrinting=true
      | 
      |[Power]
      |LogLevel=1
      |ConsolePrinting=true
    """.stripMargin

}

