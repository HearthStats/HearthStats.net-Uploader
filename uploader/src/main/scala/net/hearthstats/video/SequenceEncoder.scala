package net.hearthstats.video

import org.jcodec.common.NIOUtils
import java.nio.ByteBuffer
import org.jcodec.scale.RgbToYuv420
import org.jcodec.codecs.h264.H264Encoder
import java.awt.image.BufferedImage
import org.jcodec.codecs.h264.H264Utils
import org.jcodec.containers.mp4.muxer.MP4Muxer
import org.jcodec.containers.mp4.Brand
import org.jcodec.common.model.Picture
import org.jcodec.common.model.ColorSpace
import java.util.Arrays
import java.io.File
import java.util.ArrayList
import org.jcodec.scale.AWTUtil
import org.jcodec.containers.mp4.TrackType
import org.jcodec.containers.mp4.MP4Packet

class SequenceEncoder(val out: File = File.createTempFile("HSReplay", "video.mp4")) {
  val ch = NIOUtils.writableFileChannel(out)
  val transform = new RgbToYuv420(0, 0)
  val encoder = new H264Encoder()
  val spsList = new ArrayList[ByteBuffer]()
  val ppsList = new ArrayList[ByteBuffer]()
  val _out = ByteBuffer.allocate(1920 * 1080 * 6)
  var frameNo = 0
  val muxer = new MP4Muxer(ch, Brand.MP4)
  val outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, 25)
  var closed = false

  def encodeImage(bi: BufferedImage) =
    if (!closed) {
      val toEncode = Picture.create(bi.getWidth, bi.getHeight, ColorSpace.YUV420)
      for (i <- 0 until 3) Arrays.fill(toEncode.getData()(i), 0)
      transform.transform(AWTUtil.fromBufferedImage(bi), toEncode)
      _out.clear()
      val result = encoder.encodeFrame(_out, toEncode)
      spsList.clear()
      ppsList.clear()
      H264Utils.encodeMOVPacket(result, spsList, ppsList)
      outTrack.addFrame(new MP4Packet(result, frameNo, 25, 1, frameNo, true, null, frameNo, 0))
      frameNo += 1
    }

  def finish() {
    outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList))
    muxer.writeHeader()
    NIOUtils.closeQuietly(ch)
    closed = true
  }
}