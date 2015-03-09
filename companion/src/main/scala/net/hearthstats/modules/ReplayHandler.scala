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
import net.hearthstats.ui.GeneralUI

class ReplayHandler(
  fileUploaderFactory: FileUploaderFactory,
  config: UserConfig,
  uiLog: Log,
  ui: GeneralUI,
  api: API) {

  import config._

  if (api.premiumUserId.isEmpty) {
    uiLog.warn("You don't have premium subscription, videos won't be uploaded to hearthstats.net")
  }

  def reallyUpload = uploadVideo && api.premiumUserId.isDefined
  def auto = autoUploadVideo
  val videoUploader = fileUploaderFactory.newInstance(!reallyUpload)

  def handleNewReplay(fileName: String, lastMatch: HearthstoneMatch): Future[String] = {
    import lastMatch._
    val gameDesc = s"$userClass ${result.get} VS $opponentClass ($opponentName)"
    val auto = false
    val choice = null
    
    val f = new File(fileName)
    val now = System.currentTimeMillis
    val dateFile = DateTimeFormat.forPattern("dd_HH'h'mm").print(now)
    val dateFolder = DateTimeFormat.forPattern("yyyy_MMM").print(now)
    val newName = s"${dateFile}_${userClass}_${result.get}_VS_$opponentClass.mp4"
    val p = recordedVideoFolder.get
    val folder = new File(s"$p/$dateFolder")
    folder.mkdirs()
    val newFile = new File(folder, newName)
    if (f.renameTo(newFile)) {
      val p = newFile.getAbsolutePath
      val u = newFile.toURI.toURL
      uiLog.info(s"Video replay of your match is saved in <a href='$u'>$p</a>")
    } else throw new IllegalArgumentException(s"Could not rename $f to $newFile")

    if(!auto)
    {
      val msg = s"""Do you want to upload $gameDesc ?
                  | You need to keep HearthStats Companion window 
                  | open during the upload""".stripMargin
    
    
      val choice = ui.showConfirmDialog(msg, "Upload last game ?", YES_NO_OPTION)
    }
    
    if ( (auto && reallyUpload) || (YES_OPTION == choice && reallyUpload))
      videoUploader.uploadFile(newFile, lastMatch.id.toString, config, api).map {
        case () =>
          uiLog.info(s"$newName was uploaded to hearthstats.net")
          newName
      }
    else Promise[String].future
  }
}