package net.hearthstats.video

import net.hearthstats.log.Log
import net.hearthstats.video.VideoEncoder

/**
 * Checks if the video module is present in the classpath, either uses it or a dummy implementation.
 */
object VideoEncoderFactory {
  var status: Status = INITIAL

  def newVideo: VideoEncoder =
    if (FAILURE == status) new VideoEncoder {}
    else {
      try {
        val encoder = Class.forName("net.hearthstats.video.SequenceEncoder").newInstance.asInstanceOf[VideoEncoder]
        if (INITIAL == status) {
          Log.info("Video module loaded")
          status = SUCCESS
        }
        encoder

      } catch {
        case e: Exception =>
          if (INITIAL == status) {
            Log.warn("Video module could not be loaded : " + e.getMessage, e)
            status = FAILURE
          }
          status = FAILURE
          new VideoEncoder {}
      }

    }

  sealed trait Status
  case object INITIAL extends Status
  case object FAILURE extends Status
  case object SUCCESS extends Status

}