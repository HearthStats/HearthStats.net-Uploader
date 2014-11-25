package net.hearthstats.modules.upload

import java.io.File
import scala.concurrent.Future
import scala.concurrent.Promise
import net.hearthstats.config.UserConfig
import net.hearthstats.hstatsapi.API

/**
 * Uploads a file to a server such as Youtube or AWS.
 */
trait FileUploader {
  def uploadFile(f: File, gameId: String, config: UserConfig, api: API): Future[Unit]
}

class DummyFileUploader extends FileUploader {
  /**
   * Default implementation never completes.
   */
  def uploadFile(f: File, gameId: String, config: UserConfig, api: API): Future[Unit] =
    Promise[Unit].future
}