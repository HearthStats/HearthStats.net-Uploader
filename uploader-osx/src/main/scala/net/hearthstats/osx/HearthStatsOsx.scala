package net.hearthstats.osx

import net.hearthstats.Main
import java.io.File
import net.hearthstats.log.Log
import grizzled.slf4j.Logging
import javax.swing.{JOptionPane, JLabel}
import net.hearthstats.config.Environment


/**
 * Main object for the OS X bundle, starts up the HearthStats Uploader.
 */
object HearthStatsOsx extends Logging {

  val environment = new EnvironmentOsx()


  /**
   * The entry point: this is the first method to run when HearthStats starts up on Mac OS X.
   *
   * @param args Parameters from the command line, if any.
   */
  def main(args: Array[String]) {

    setupTesseract

    Main.start(environment)

  }


  private def setupTesseract {
    debug("Setting up Tesseract data")

    // Determine where the Tesseract training data is stored
    val javaLibraryPath: File = new File(Environment.systemProperty("java.library.path"))
    val outPath = javaLibraryPath.getParentFile.getAbsolutePath + "/Resources"

    // Load the native libraries
    try {
      loadOsxDylib("lept")
      loadOsxDylib("tesseract")
    }
    catch {
      case e: Throwable => {
        Log.error("Error loading libraries", e)
        showLibraryErrorMessage(e)
        System.exit(0)
      }
    }

    // Perform the standard Tesseract setup
    Main.setupTesseract(outPath)
  }


  private def loadOsxDylib(name: String) {
    debug(s"Loading dylib $name")
    try {
      System.loadLibrary(name)
    }
    catch {
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
        new JLabel("The HearthStats Uploader was unable to start because expected system libraries were not found."),
        new JLabel("Please check your log.txt file for details."),
        new JLabel(" "),
        new JLabel("Exiting..."))
    } else {
      title = e.getMessage
      message = Array[JLabel](
        new JLabel("The HearthStats Uploader was unable to start because the OCR libraries could not be read."),
        new JLabel("Is the app already running?"),
        new JLabel(" "),
        new JLabel("Exiting..."))
    }
    JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)
  }


}