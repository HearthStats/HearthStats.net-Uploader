package net.hearthstats

import java.awt.Component
import java.io.File
import javax.swing.JDialog
import javax.swing.JFrame
import javax.swing.JOptionPane
import net.hearthstats.config.Application
import net.hearthstats.config.Environment
import net.hearthstats.ui.notification.DialogNotification
import net.sourceforge.tess4j.Tesseract
import scala.collection.JavaConversions._
import grizzled.slf4j.Logging
import javax.swing.WindowConstants

class Main(environment: Environment) extends Logging {

  private var ocrLanguage: String = "eng"

  def start(): Unit = {
    val loadingNotification = new DialogNotification("HearthStats Companion", "Loading ...")
    loadingNotification.show()
    logSystemInformation()
    Updater.cleanUp(environment)
    cleanupDebugFiles()
    loadingNotification.close()
  }

  private def logSystemInformation(): Unit = {
    if (isInfoEnabled) {
      info("**********************************************************************")
      info(s"  Starting HearthStats Companion ${Application.version} on ${environment.os}")
      info("  os.name=" + Environment.systemProperty("os.name"))
      info("  os.version=" + Environment.systemProperty("os.version"))
      info("  os.arch=" + Environment.systemProperty("os.arch"))
      info("  java.runtime.version=" + Environment.systemProperty("java.runtime.version"))
      info("  java.class.path=" + Environment.systemProperty("java.class.path"))
      info("  java.library.path=" + Environment.systemProperty("java.library.path"))
      info("  user.language=" + Environment.systemProperty("user.language"))
      info("**********************************************************************")
    }
  }

  private def cleanupDebugFiles(): Unit = {
    try {
      val folder = new File(environment.extractionFolder)
      if (folder.exists) {
        val files = folder.listFiles
        for (file <- files if file.isFile && file.getName.startsWith("class-") && file.getName.endsWith(".png")) {
          file.delete()
        }
      }
    } catch {
      case e: Exception => warn("Ignoring exception when cleaning up debug files", e)
    }
  }

  def setupTesseract(outPath: String): Unit = {
    val instance = Tesseract.getInstance
    instance.setDatapath(outPath + "tessdata")
    instance.setLanguage(ocrLanguage)
  }
}

object Main extends Logging {
  def showMessageDialog(parentComponent: Component, message: String): Unit = {
    val op = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE)
    val dialog = op.createDialog(parentComponent, "HearthStats.net")
    dialog.setAlwaysOnTop(true)
    dialog.setModal(true)
    dialog.setFocusableWindowState(true)
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
    dialog.setVisible(true)
  }

  def showErrorDialog(message: String, e: Throwable): Unit = {
    error(message, e)
    val frame = new JFrame
    frame.setFocusableWindowState(true)
    showMessageDialog(null, message + "\n" + e.getMessage + "\n\nSee log.txt for details")
  }
}