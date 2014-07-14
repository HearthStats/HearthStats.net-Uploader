package net.hearthstats.modules

import scala.concurrent.ExecutionContext.Implicits.global
import net.hearthstats.HearthstoneMatch
import org.joda.time.format.DateTimeFormat
import net.hearthstats.log.Log
import java.io.File
import scala.concurrent.Future
import net.hearthstats.config.TempConfig

object ReplayHandler {
  val videoUploader = FileUploaderFactory.newUploader()

  def handleNewReplay(fileName: String, lastMatch: HearthstoneMatch): Future[Unit] = {
    val f = new File(fileName)
    val date = DateTimeFormat.forPattern("yyyyMMddHHmm").print(lastMatch.startedAt)
    val newName = s"${date}_${lastMatch.userClass}_VS_${lastMatch.opponentClass}.mp4"
    val newFile = new File(TempConfig.recordedVideoFolder, newName)
    if (f.renameTo(newFile)) {
      Log.info("Video replay of your match is saved in " + newFile.getAbsolutePath)
      videoUploader.uploadFile(newFile, lastMatch.user)
    } else throw new IllegalArgumentException(s"Could not rename $f to $newFile")
  }
}