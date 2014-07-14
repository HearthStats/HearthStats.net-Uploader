package net.hearthstats.video

import java.awt.geom.AffineTransform
import java.awt.image.{ AffineTransformOp, BufferedImage }
import java.io.File
import java.nio.ByteBuffer
import java.util.{ ArrayList, Arrays }

import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global

import org.jcodec.codecs.h264.{ H264Encoder, H264Utils }
import org.jcodec.common.NIOUtils
import org.jcodec.common.model.{ ColorSpace, Picture }
import org.jcodec.containers.mp4.{ Brand, MP4Packet, TrackType }
import org.jcodec.containers.mp4.muxer.MP4Muxer
import org.jcodec.scale.{ AWTUtil, RgbToYuv420 }

import com.xuggle.mediatool.ToolFactory

class SequenceEncoder extends VideoEncoder {
  val out: File = File.createTempFile("HSReplay", "video.mp4")
  val framesPerSec = 10

  val ch = NIOUtils.writableFileChannel(out)
  val transform = new RgbToYuv420(0, 0)
  val encoder = new H264Encoder
  val spsList = new ArrayList[ByteBuffer]()
  val ppsList = new ArrayList[ByteBuffer]()
  val _out = ByteBuffer.allocate(1920 * 1080 * 6)
  var frameNo = 0
  val muxer = new MP4Muxer(ch, Brand.MP4)
  val outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, framesPerSec)
  var closed = false

  override def encodeImage(bi: BufferedImage): Unit =
    if (!closed) {
      val resized = resize(bi, 800, 600)
      val toEncode = Picture.create(resized.getWidth, resized.getHeight, ColorSpace.YUV420)
      for (i <- 0 until 3) Arrays.fill(toEncode.getData()(i), 0)
      transform.transform(AWTUtil.fromBufferedImage(resized), toEncode)
      _out.clear()
      val result = encoder.encodeFrame(_out, toEncode)
      spsList.clear()
      ppsList.clear()
      H264Utils.encodeMOVPacket(result, spsList, ppsList)
      outTrack.addFrame(new MP4Packet(result, frameNo, framesPerSec, 1, frameNo, false, null, frameNo, 0))
      frameNo += 1
    }

  /**
   * Returns the name of the compressed file.
   */
  override def finish(): Future[String] =
    if (!closed) {
      outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList))
      muxer.writeHeader()
      NIOUtils.closeQuietly(ch)
      closed = true
      Future {
        val reader = ToolFactory.makeReader(out.getAbsolutePath)
        val compressed = File.createTempFile("video", ".mp4").getAbsolutePath
        reader.addListener(ToolFactory.makeWriter(compressed, reader))
        while (reader.readPacket() == null) ()
        out.delete()
        compressed
      }
    } else Promise[String].future // never completes

  private def resize(bi: BufferedImage, x: Int = 640, y: Int = 480): BufferedImage = {
    val (h, w) = (bi.getHeight, bi.getWidth)
    if (w <= x && h <= y) bi
    else {
      val scale = Math.min(x.toFloat / w, y.toFloat / h)
      val at = AffineTransform.getScaleInstance(scale, scale)
      val scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
      scaleOp.filter(bi, null)
    }
  }
}
