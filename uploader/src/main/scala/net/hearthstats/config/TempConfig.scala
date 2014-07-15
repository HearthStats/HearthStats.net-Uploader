package net.hearthstats.config

import java.io.File

/**
 * Temporary placeholder for config values.
 * Will be replaced when @gtch develops the new Config class.
 */
object TempConfig {
  val recordVideoReplay = true
  val recordedVideoFolder = {
    val folder = new File(System.getProperty("user.home") + "/hearthstats/videos")
    folder.mkdirs()
    folder
  }
  val uploadVideoReplay = true
  val awsBucket = "hearthstats-dev"
  val awsVideoPrefix = "prem-videos"
}