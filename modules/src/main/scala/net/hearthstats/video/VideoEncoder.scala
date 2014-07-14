package net.hearthstats.video

import java.awt.image.BufferedImage
import scala.concurrent.{Future, Promise}

/**
 * Defines the API for the video module.
 *
 * This module should encode images in a video file and return its name when finish is called.
 */
trait VideoEncoder {

  def encodeImage(bi: BufferedImage): Unit = {}

  /**
   * Returns the name of the compressed file.
   */
  def finish(): Future[String] = Promise[String].future
}
