package net.hearthstats.modules.video

import java.awt.image.BufferedImage
import scala.concurrent.{ Future, Promise }

/**
 * Defines the API for the video module.
 *
 * This module should encode images in a video file and return its name when finish is called.
 */
trait VideoEncoder {
  def newVideo(framesPerSec: Double, videoWidth: Int, videoHeight: Int): OngoingVideo
}

trait OngoingVideo {
  def encodeImage(bi: BufferedImage, timeMs: Long): Unit
  def finish(): Future[String]
}

class DummyVideoEncoder extends VideoEncoder {
  def newVideo(framesPerSec: Double, videoWidth: Int, videoHeight: Int) = new OngoingVideo {
    def encodeImage(bi: BufferedImage, timeMs: Long): Unit = {}
    def finish(): Future[String] = Promise[String].future
  }
}
