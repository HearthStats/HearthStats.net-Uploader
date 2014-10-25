package net.hearthstats.modules

import net.hearthstats.modules.video.VideoEncoder
import net.hearthstats.modules.video.DummyVideoEncoder

class VideoEncoderFactory(use: => Boolean) extends ModuleFactory[VideoEncoder](
  "video replay",
  classOf[VideoEncoder],
  use,
  classOf[DummyVideoEncoder])