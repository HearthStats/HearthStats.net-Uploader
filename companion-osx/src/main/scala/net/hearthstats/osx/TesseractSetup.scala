package net.hearthstats.osx

import java.io._
import javax.swing.{JLabel, JOptionPane}

import grizzled.slf4j.Logging
import net.hearthstats.config.Environment
import net.hearthstats.{Main, ProgramHelper}


trait TesseractSetup extends Logging {
  val environment: Environment
  val helper: ProgramHelper
  val main: Main

  def setupTesseract(): Unit = {
    debug("Setting up Tesseract data")

    // Determine where the Tesseract training data is stored
    val javaLibraryPath: File = new File(Environment.systemProperty("java.library.path"))
    val outPath = javaLibraryPath.getParentFile.getAbsolutePath + "/Resources"

    // Load the native libraries
    try {
      loadOsxDylib("lept")
      loadOsxDylib("tesseract")
    } catch {
      case e: Throwable => {
        main.uiLog.error("Error loading libraries", e)
        showLibraryErrorMessage(e)
        System.exit(0)
      }
    }

    // Perform the standard Tesseract setup
    main.setupTesseract(outPath)
  }

  private def loadOsxDylib(name: String) {
    debug(s"Loading dylib $name")
    try {
      System.loadLibrary(name)
    } catch {
      case e: UnsatisfiedLinkError => {
        error(s"UnsatisfiedLinkError loading dylib $name", e)
        throw e
      }
      case e: Exception => {
        error(s"Error loading dylib $name", e)
      }
    }
  }

  private def showLibraryErrorMessage(e: Throwable) {
    var title: String = null
    var message: Array[JLabel] = null
    if (e.isInstanceOf[UnsatisfiedLinkError]) {
      title = "Expected libraries are not installed"
      message = Array[JLabel](
        new JLabel("The HearthStats Companion was unable to start because expected system libraries were not found."),
        new JLabel("Please check your log.txt file for details."),
        new JLabel(" "),
        new JLabel("Exiting..."))
    } else {
      title = e.getMessage
      message = Array[JLabel](
        new JLabel("The HearthStats Companion was unable to start because the OCR libraries could not be read."),
        new JLabel("Is the app already running?"),
        new JLabel(" "),
        new JLabel("Exiting..."))
    }
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)
  }

}