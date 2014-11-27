package net.hearthstats.modules.video

import net.hearthstats.osx.VideoOsx
import org.rococoa.cocoa.foundation.NSAutoreleasePool

/**
 * Records a three-second video of Hearthstone, if it is running.
 */
object RecordHearthstoneVideoMain extends App {

  OsxNativeEncoder.loadNativeLibraries()

  val pool: NSAutoreleasePool = NSAutoreleasePool.new_
  try {
    val videoOsx = VideoOsx.CLASS.alloc()

    videoOsx.startVideo()

    Thread.sleep(3000)

    videoOsx.startVideo()

    Thread.sleep(3000)

  }

}
