package net.hearthstats.modules.video

import grizzled.slf4j.Logging
import net.hearthstats.osx.VideoOsx
import org.rococoa.cocoa.foundation.NSAutoreleasePool

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

/**
 * Video encoder for OS X which uses native Mac code for fast, efficient video recording.
 */
class OsxNativeEncoder extends VideoEncoder with Logging {

  def newVideo(framesPerSec: Double, videoWidth: Int, videoHeight: Int) = new OngoingVideo {
    debug(s"Starting OS X native video encoding, fps=$framesPerSec w=$videoWidth h=$videoHeight")

    OsxNativeEncoder.loadNativeLibraries()

    val pool: NSAutoreleasePool = NSAutoreleasePool.new_
    val videoOsx = VideoOsx.CLASS.alloc()

    videoOsx.startVideo()

    override def finish(): Future[String] = Future {
      debug(s"Finishing OS X native video encoding")
      val filename = videoOsx.stopVideo()
      info(s"### finish() filename $filename")
      filename
    }

    override def encodeImages(images: List[TimedImage]): Future[Long] = {
      throw new IllegalStateException("OsxNativeEncoder does not expect images, so encodeImages() must not be called")
    }

    override def expectsImages = false

    override def finalize(): Unit = {
      super.finalize()
      pool.drain()
    }
  }
}


object OsxNativeEncoder extends Logging {

  var haveLoadedLibraries = false

  def loadNativeLibraries(): Unit = {
    if (!haveLoadedLibraries) {
      haveLoadedLibraries = true
      info("Loading native libraries for OS X video")
      loadOsxDylib("swiftCore")
      loadOsxDylib("swiftDarwin")
      loadOsxDylib("swiftObjectiveC")
      loadOsxDylib("swiftDispatch")
      loadOsxDylib("swiftCoreGraphics")
      loadOsxDylib("swiftSecurity")
      loadOsxDylib("swiftFoundation")
      loadOsxDylib("swiftQuartzCore")
      loadOsxDylib("swiftAppKit")
      loadOsxDylib("videoOsx")
    }
  }

  private def loadOsxDylib(name: String) {
    debug(s"Loading dylib $name")
    try {
      System.loadLibrary(name)
    } catch {
      case e: UnsatisfiedLinkError => {
        error(s"UnsatisfiedLinkError loading dylib $name", e)
        throw e
      }
      case e: Exception => {
        error(s"Error loading dylib $name", e)
      }
    }
  }

}