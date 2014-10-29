package net.hearthstats.util

import net.hearthstats.config.Environment
import java.awt.Desktop
import java.net.URI
import net.hearthstats.updater.api.model.Release
import org.apache.commons.io.filefilter.WildcardFileFilter
import net.hearthstats.updater.api.GitHubReleases
import net.hearthstats.Main
import net.hearthstats.config.OS
import java.io.File
import net.hearthstats.ui.log.Log
import grizzled.slf4j.Logging
import java.io.FileFilter

class Updater(environment: Environment, uiLog: Log) extends Logging {

  val MANUAL_DOWNLOAD_URL = "http://hearthstats.net/uploader"

  private var cachedlatestRelease: Release = _

  def getLatestRelease(): Release = {
    if (cachedlatestRelease == null) {
      try {
        debug("Loading latest release information from GitHub")
        if (environment.os == OS.OSX) {
          cachedlatestRelease = GitHubReleases.getLatestReleaseForOSX
        } else if (environment.os == OS.WINDOWS) {
          cachedlatestRelease = GitHubReleases.getLatestReleaseForWindows
        }
        if (cachedlatestRelease == null) {
          uiLog.warn("Unable to check latest release of HearthStats Companion")
        }
      } catch {
        case e: Exception => uiLog.warn("Unable to check latest release of HearthStats Companion due to error: " +
          e.getMessage, e)
      }
    }
    cachedlatestRelease
  }

  def run(release: Release) {
    uiLog.info("Extracting and running updater ...")
    var errorMessage: String = null
    try {
      errorMessage = environment.performApplicationUpdate(release)
    } catch {
      case e: Exception => {
        warn("Unable to run updater", e)
        if (errorMessage == null) {
          errorMessage = e.getMessage
        }
      }
    }
    if (errorMessage == null) {
      System.exit(0)
    } else {
      Main.showMessageDialog(null, errorMessage + "\n\nYou will now be taken to the download page.")
      try {
        Desktop.getDesktop.browse(new URI(MANUAL_DOWNLOAD_URL))
      } catch {
        case e: Exception => Main.showErrorDialog("Error launching browser with URL " + MANUAL_DOWNLOAD_URL,
          e)
      }
      uiLog.warn("Updater Error: " + errorMessage)
    }
  }

  def cleanUp() {
    removeFile(environment.extractionFolder, "updater.jar")
    removeFile(environment.extractionFolder, "update-*.zip")
  }

  /**
   * Deletes a file or files in the given path.
   * @param path The path where the file or files are located.
   * @param filename The file or files to delete. Accepts wildcards * and ?.
   */
  private def removeFile(path: String, filename: String) {
    debug(s"Deleting $path/$filename")
    val dir = new File(path)
    val fileFilter: FileFilter = new WildcardFileFilter(filename)
    val files = dir.listFiles(fileFilter)
    for (file <- files) {
      info("Deleting file " + file.getAbsolutePath)
      file.delete()
    }
  }
}
