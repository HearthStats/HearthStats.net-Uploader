package net.hearthstats.video

import com.xuggle.mediatool.ToolFactory
import java.io.File

object VideoCompressMain extends App {
  val reader = ToolFactory.makeReader("""C:\Users\tyrcho\AppData\Local\Temp\HSReplay5340612305252038992video.mp4""")
  reader.addListener(ToolFactory.makeWriter(File.createTempFile("video", ".avi").getAbsolutePath, reader))
  while (reader.readPacket() == null) ()
}