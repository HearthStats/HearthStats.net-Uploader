package net.hearthstats.config

import java.io.File
import org.joda.time.format.DateTimeFormat
import java.util.Date
import org.joda.time.DateTime

/**
 * Temporary placeholder for config values.
 * Will be replaced when @gtch develops the new Config class.
 */
object TempConfig {
  val recordVideoReplay = true
  val recordedVideoFolder = {
    val dateString = DateTimeFormat.forPattern("yyyy-MMM").print(DateTime.now)
    val home = System.getProperty("user.home")
    val folder = new File(s"$home/hearthstats/videos/$dateString")
    folder.mkdirs()
    folder
  }
  val uploadVideoReplay = true
  val awsBucket = "hearthstats-dev"
  val awsVideoPrefix = "prem-videos"
  val framesPerSec = 25 // capture rate and videos FPS
  val videoHeight = 600
  val videoWidth = 800

}