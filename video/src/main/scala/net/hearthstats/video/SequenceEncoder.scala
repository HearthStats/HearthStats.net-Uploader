package net.hearthstats.video

import java.awt.geom.AffineTransform
import java.awt.image.{ AffineTransformOp, BufferedImage }
import scala.concurrent.duration.DurationInt
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
import grizzled.slf4j.Logging
import akka.actor.ActorSystem
import akka.actor.Props
import akka.actor.Actor
import akka.pattern.ask
import akka.util.Timeout
import java.nio.file.Files
import javax.imageio.ImageIO

class SequenceEncoder(
  framesPerSec: Integer = 10,
  videoWidth: Integer = 800,
  videoHeight: Integer = 600) extends VideoEncoder with Logging {

  val out: File = File.createTempFile("HSReplay", "video.mp4")
  val picFolder = Files.createTempDirectory("hspic")
  info(s"Storing pictures in $picFolder")

  val ch = NIOUtils.writableFileChannel(out)
  val transform = new RgbToYuv420(0, 0)
  val encoder = new H264Encoder
  val spsList = new ArrayList[ByteBuffer]()
  val ppsList = new ArrayList[ByteBuffer]()
  val _out = ByteBuffer.allocate(1920 * 1080 * 6)
  var frameNo = 0
  var receivedFrames = 0
  var storedFrames = 0
  val muxer = new MP4Muxer(ch, Brand.MP4)
  val outTrack = muxer.addTrackForCompressed(TrackType.VIDEO, framesPerSec)
  var closed = false

  val system = ActorSystem("HearthStatsCompanion")

  case class Encode(bi: BufferedImage)
  case object Finish

  val encodeActor = system.actorOf(Props(new Actor {
    def receive = {
      case Finish => sender ! finishImpl()
      case Encode(bi) => encodeImageImpl(bi)
    }
  }))

  def store(bi: BufferedImage): Unit = {

  }

  override def encodeImage(bi: BufferedImage): Unit =
    if (!closed) {
      receivedFrames += 1
      debug(s"$receivedFrames received to encode")
      encodeActor ! Encode(bi)
    }

  def encodeImageImpl(bi: BufferedImage): Unit = {
    val f = new File(picFolder.toFile, "t" + System.nanoTime)
    val resized = resize(bi, videoWidth, videoHeight)
    ImageIO.write(resized, "png", f)
    storedFrames += 1
    debug(s"$storedFrames stored")
  }

  /**
   * Returns the name of the compressed file.
   */
  override def finish(): Future[String] = if (!closed) {
    closed = true
    encodeActor.ask(Finish)(Timeout(10.seconds)).map(_.toString)
  } else Promise[String].future // never completes

  private def buildVideoFromImages(): Unit = {
    for (f <- picFolder.toFile.listFiles.sortBy(_.getName)) {
      val img = ImageIO.read(f)
      val toEncode = Picture.create(img.getWidth, img.getHeight, ColorSpace.YUV420)
      for (i <- 0 until 3) Arrays.fill(toEncode.getData()(i), 0)
      transform.transform(AWTUtil.fromBufferedImage(img), toEncode)
      _out.clear()
      val result = encoder.encodeFrame(_out, toEncode)
      spsList.clear()
      ppsList.clear()
      H264Utils.encodeMOVPacket(result, spsList, ppsList)
      outTrack.addFrame(new MP4Packet(result, frameNo, framesPerSec.toLong, 1, frameNo, false, null, frameNo, 0))
      frameNo += 1
      debug(s"$frameNo encoded in video")
    }
  }

  private def finishImpl(): String = {
    buildVideoFromImages()
    outTrack.addSampleEntry(H264Utils.createMOVSampleEntry(spsList, ppsList))
    muxer.writeHeader()
    NIOUtils.closeQuietly(ch)
    val reader = ToolFactory.makeReader(out.getAbsolutePath)
    val compressed = File.createTempFile("video", ".mp4").getAbsolutePath
    reader.addListener(ToolFactory.makeWriter(compressed, reader))
    while (reader.readPacket() == null) ()
    out.delete()
    compressed
  }

  private def resize(bi: BufferedImage, x: Int, y: Int): BufferedImage = {
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
