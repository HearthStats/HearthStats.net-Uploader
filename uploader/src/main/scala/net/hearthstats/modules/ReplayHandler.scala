package net.hearthstats.modules

import java.io.File
import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.format.DateTimeFormat
import javax.swing.JOptionPane.{ YES_NO_OPTION, YES_OPTION, showConfirmDialog }
import net.hearthstats.{ Config, HearthstoneMatch }
import net.hearthstats.config.TempConfig
import net.hearthstats.log.Log
import net.hearthstats.API

object ReplayHandler {
  val videoUploader = FileUploaderFactory.newUploader()

  def handleNewReplay(fileName: String, lastMatch: HearthstoneMatch): Future[String] = {
    import lastMatch._
    val gameDesc = s"$userClass ${result.get} VS $opponentClass ($opponentName)"

    val f = new File(fileName)
    val date = DateTimeFormat.forPattern("ddHHmm").print(startedAt)
    val newName = s"${date}_${userClass}_${result.get}_VS_$opponentClass.mp4"
    val newFile = new File(TempConfig.recordedVideoFolder, newName)
    if (f.renameTo(newFile)) {
      Log.info("Video replay of your match is saved in " + newFile.getAbsolutePath)
    } else throw new IllegalArgumentException(s"Could not rename $f to $newFile")

    val msg = s"""Do you want to upload $gameDesc ?
                | You need to keep HearthStats Companion window 
                | open during the upload""".stripMargin
    val choice = showConfirmDialog(null, msg, "Upload last game ?", YES_NO_OPTION)

    if (TempConfig.uploadVideoReplay && YES_OPTION == choice) {
      videoUploader.uploadFile(newFile, API.premiumUserId.get).map {
        case () => newName
      }
    } else Promise[String].future
  }
}