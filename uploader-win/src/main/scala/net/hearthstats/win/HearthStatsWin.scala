package net.hearthstats.win

import java.io._
import net.hearthstats.config.Environment
import javax.swing.{ JOptionPane, JLabel }
import grizzled.slf4j.Logging
import net.hearthstats.ui.HyperLinkHandler
import net.hearthstats.Main
import com.softwaremill.macwire.MacwireMacros._

/**
 * Main object for the Windows application, starts up the HearthStats Companion.
 */
object HearthStatsWin extends Logging with App {

  val environment = new EnvironmentWin
  val main: Main = wire[Main]

  setupTesseract()
  main.start()

  def setupTesseract(): Unit = {
    debug("Extracting Tesseract data")

    // Determine where the Tesseract training data is stored, and copy it into the tmp folder
    val outPath = environment.extractionFolder + "/"
    (new File(outPath + "tessdata/configs")).mkdirs
    copyFileFromJarTo("/tessdata/eng.traineddata", outPath + "tessdata/eng.traineddata")
    copyFileFromJarTo("/tessdata/configs/api_config", outPath + "tessdata/configs/api_config")
    copyFileFromJarTo("/tessdata/configs/digits", outPath + "tessdata/configs/digits")
    copyFileFromJarTo("/tessdata/configs/hocr", outPath + "tessdata/configs/hocr")

    // Load the native libraries
    try {
      loadJarDll("liblept168")
      loadJarDll("libtesseract302")
    } catch {
      case e: Throwable =>
        error("Error loading libraries", e)
        showLibraryErrorMessage(e)
        System.exit(0)
    }

    // Perform the standard Tesseract setup
    main.setupTesseract(outPath)
  }

  private def showLibraryErrorMessage(e: Throwable) {
    var title: String = null
    var message: Array[JLabel] = null
    if (e.isInstanceOf[UnsatisfiedLinkError]) {
      title = "Expected libraries are not installed"
      if ("amd64" == Environment.systemProperty("os.arch")) {
        message = Array[JLabel](
          new JLabel("The HearthStats Companion requires the Visual C++ Redistributable to be installed."),
          new JLabel("Please download the 64-bit installer (vcredist_x64.exe) from"),
          HyperLinkHandler.getUrlLabel("http://www.microsoft.com/en-US/download/details.aspx?id=30679"),
          new JLabel("and install it before using the HearthStats Companion."))
      } else {
        message = Array[JLabel](
          new JLabel("The HearthStats Companion requires the Visual C++ Redistributable to be installed."),
          new JLabel("Please download the installer from"),
          HyperLinkHandler.getUrlLabel("http://www.microsoft.com/en-US/download/details.aspx?id=30679"),
          new JLabel("and install it before using the HearthStats Companion."))
      }
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

  private def copyFileFromJarTo(jarPath: String, outPath: String) {
    val stream: InputStream = getClass.getResourceAsStream(jarPath)
    if (stream == null) {
      error(s"Exception: Unable to load file from JAR: $jarPath")
      Main.showMessageDialog(null, s"Exception: Unable to find $jarPath in .jar file\n\nSee log.txt for details")
      System.exit(1)
    } else {
      var resStreamOut: OutputStream = null
      var readBytes: Int = 0
      val buffer: Array[Byte] = new Array[Byte](4096)
      try {
        resStreamOut = new FileOutputStream(new File(outPath))
        while ((({
          readBytes = stream.read(buffer); readBytes
        })) > 0) {
          resStreamOut.write(buffer, 0, readBytes)
        }
      } catch {
        case e: IOException => {
          Main.showErrorDialog(s"Error writing file $outPath", e)
        }
      } finally {
        try {
          stream.close
          resStreamOut.close
        } catch {
          case e: IOException => {
            Main.showErrorDialog(s"Error closing stream for $jarPath", e)
          }
        }
      }
    }
  }

  private def loadJarDll(name: String) {
    debug(s"Loading DLL $name")
    val resourcePath: String = "/lib/" + name + "_" + System.getProperty("sun.arch.data.model") + ".dll"
    val in: InputStream = getClass.getResourceAsStream(resourcePath)
    if (in != null) {
      val buffer: Array[Byte] = new Array[Byte](1024)
      var read: Int = -1
      val outDir: File = new File(environment.extractionFolder)
      outDir.mkdirs
      val outPath: String = outDir.getPath + "/"
      val outFileName: String = name.replace("_32", "").replace("_64", "") + ".dll"
      var fos: FileOutputStream = null
      fos = new FileOutputStream(outPath + outFileName)
      try {
        while (({
          read = in.read(buffer); read
        }) != -1) {
          fos.write(buffer, 0, read)
        }
        fos.close
        in.close
      } catch {
        case e: IOException => {
          error(s"Error copying DLL $name", e)
        }
      }
      try {
        System.loadLibrary(outPath + name)
      } catch {
        case e: UnsatisfiedLinkError => {
          error(s"UnsatisfiedLinkError loading DLL $name", e)
          throw e
        }
        case e: Exception => {
          error(s"Error loading DLL $name", e)
        }
      }
    } else {
      Main.showErrorDialog("Error loading " + name, new Exception(s"Unable to load library from $resourcePath"))
    }
  }
}