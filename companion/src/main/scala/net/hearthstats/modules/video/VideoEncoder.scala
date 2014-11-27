package net.hearthstats.modules.video

import java.awt.image.BufferedImage

import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Defines the API for the video module.
 *
 * This module should encode images in a video file and return its name when finish is called.
 */
trait VideoEncoder {
  def newVideo(framesPerSec: Double, videoWidth: Int, videoHeight: Int): OngoingVideo
}

trait OngoingVideo {
  /**
   * Replies with the timeMs until it was encoded.
   */
  def encodeImages(images: List[TimedImage]): Future[Long]

  def finish(): Future[String]

  /**
   * Whether this video encoder expects images to be supplied to it.
   * @return true if each frame of the video must be supplied, false if the video encoder gets its images from somewhere else
   */
  def expectsImages: Boolean
}

case class TimedImage(bi: BufferedImage, timeMs: Long)

class DummyVideoEncoder extends VideoEncoder {
  def newVideo(framesPerSec: Double, videoWidth: Int, videoHeight: Int) = new OngoingVideo {

    def encodeImages(images: List[TimedImage]): Future[Long] =
      Future {
        Thread.sleep((1000.0 / framesPerSec).toLong)
        // avoids returning immediately when we don't encode video
        images.map(_.timeMs).max
      }

    def finish(): Future[String] = Promise[String].future

    def expectsImages: Boolean = true
  }
}
