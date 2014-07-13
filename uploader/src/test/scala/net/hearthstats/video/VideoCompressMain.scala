package net.hearthstats.video

import com.xuggle.mediatool.ToolFactory
import java.io.File

object VideoCompressMain extends App {
  val reader = ToolFactory.makeReader("""C:\Users\tyrcho\AppData\Local\Temp\HSReplay5395579179633114373video.mp4""")
  reader.addListener(ToolFactory.makeWriter(File.createTempFile("video", ".mp4").getAbsolutePath, reader))
  while (reader.readPacket() == null) ()
}