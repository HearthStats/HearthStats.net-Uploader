package net.hearthstats.modules

import net.hearthstats.log.Log
import net.hearthstats.config.TempConfig
import net.hearthstats.video.VideoEncoder

/**
 * Checks if the video module is present in the classpath, either uses it or a dummy implementation.
 */
object VideoEncoderFactory extends ModuleFactory[VideoEncoder](
  "video replay",
  TempConfig.recordVideoReplay,
  new VideoEncoder {},
  "net.hearthstats.video.SequenceEncoder") {

  import TempConfig._
  def newVideo() = newInstance(Seq(
    Integer.valueOf(framesPerSec),
    Integer.valueOf(videoWidth),
    Integer.valueOf(videoHeight)))
}
