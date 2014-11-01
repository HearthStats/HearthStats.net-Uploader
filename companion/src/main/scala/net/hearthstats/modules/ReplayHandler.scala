package net.hearthstats.modules

import java.io.File

import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.format.DateTimeFormat

import javax.swing.JOptionPane.{ YES_NO_OPTION, YES_OPTION, showConfirmDialog }
import net.hearthstats.config.UserConfig
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log

class ReplayHandler(
  fileUploaderFactory: FileUploaderFactory,
  config: UserConfig,
  uiLog: Log,
  api: API) {

  import config._

  if (api.premiumUserId.isEmpty) {
    uiLog.warn("You don't have premium subscription, videos won't be uploaded to hearthstats.net")
  }

  def reallyUpload = uploadVideo && api.premiumUserId.isDefined
  val videoUploader = fileUploaderFactory.newInstance(!reallyUpload)

  def handleNewReplay(fileName: String, lastMatch: HearthstoneMatch): Future[String] = {
    import lastMatch._
    val gameDesc = s"$userClass ${result.get} VS $opponentClass ($opponentName)"

    val f = new File(fileName)
    val dateFile = DateTimeFormat.forPattern("dd_HH'h'mm").print(startedAt)
    val dateFolder = DateTimeFormat.forPattern("yyyy_MMM").print(startedAt)
    val newName = s"${dateFile}_${userClass}_${result.get}_VS_$opponentClass.mp4"
    val p = recordedVideoFolder.get
    val folder = new File(s"$p/$dateFolder")
    folder.mkdirs()
    val newFile = new File(folder, newName)
    if (f.renameTo(newFile)) {
      val p = newFile.getAbsolutePath
      val u = newFile.toURL
      uiLog.info(s"Video replay of your match is saved in <a href='$u'>$p</a>")
    } else throw new IllegalArgumentException(s"Could not rename $f to $newFile")

    val msg = s"""Do you want to upload $gameDesc ?
                | You need to keep HearthStats Companion window 
                | open during the upload""".stripMargin
    val choice = showConfirmDialog(null, msg, "Upload last game ?", YES_NO_OPTION)

    if (YES_OPTION == choice && reallyUpload)
      videoUploader.uploadFile(newFile, config, api).map {
        case () =>
          uiLog.info(s"$newName was uploaded to hearthstats.net")
          newName
      }
    else Promise[String].future
  }
}