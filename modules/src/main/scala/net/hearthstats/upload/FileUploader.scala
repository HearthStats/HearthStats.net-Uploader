package net.hearthstats.upload

import java.io.File
import scala.concurrent.Future
import scala.concurrent.Promise

/**
 * Uploads a file to a server such as Youtube or AWS.
 */
trait FileUploader {
  /**
   * Default implementation never completes.
   */
  def uploadFile(f: File, user: String): Future[Unit] =
    Promise[Unit].future
}