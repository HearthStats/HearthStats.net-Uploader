package net.hearthstats.modules

import net.hearthstats.modules.video.VideoEncoder
import net.hearthstats.modules.video.DummyVideoEncoder

class VideoEncoderFactory extends ModuleFactory[VideoEncoder](
  "video replay",
  classOf[VideoEncoder],
  classOf[DummyVideoEncoder])