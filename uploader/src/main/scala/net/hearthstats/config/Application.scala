package net.hearthstats.config

import scala.io.Source

/**
 * Represents information about the HearthStats Uploader application, such a version and location.
 */
object Application {

  def version: String = {
    val source = Source.fromURL(getClass.getResource("/version"))
    source.mkString.trim
  }

}