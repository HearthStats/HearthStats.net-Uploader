package net.hearthstats.modules

import net.hearthstats.log.Log
import net.hearthstats.config.TempConfig
import net.hearthstats.video.VideoEncoder
import net.hearthstats.upload.FileUploader
import net.hearthstats.API

/**
 * Checks if the video module is present in the classpath, either uses it or a dummy implementation.
 */
object FileUploaderFactory extends ModuleFactory[FileUploader](
  "video uploader",
  TempConfig.uploadVideoReplay && API.premiumUserId.isDefined,
  new FileUploader {},
  "net.hearthstats.upload.AwsUploader",
  API.awsKeys) {

  def newUploader() = newInstance(Seq(TempConfig.awsBucket, TempConfig.awsVideoPrefix))
}

