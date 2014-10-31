package net.hearthstats.modules.video

import java.awt.geom.AffineTransform
import java.awt.image.{ AffineTransformOp, BufferedImage }
import java.io.File
import scala.concurrent.{ Future, Promise }
import scala.concurrent.ExecutionContext.Implicits.global
import com.xuggle.mediatool.{ IMediaWriter, ToolFactory }
import grizzled.slf4j.Logging
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import com.xuggle.xuggler.IRational
import scala.util.control.NonFatal
import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._

class SequenceEncoder extends VideoEncoder with Logging {
  val system = ActorSystem("video")

  def newVideo(framesPerSec: Double, videoWidth: Int, videoHeight: Int) = new OngoingVideo {
    val actor = system.actorOf(Props(new EncodeActor))

    override def encodeImage(bi: BufferedImage): Unit = actor ! bi

    /**
     * Returns the name of the compressed file.
     */
    override def finish(): Future[String] =
      actor.ask(Finish)(60 seconds).asInstanceOf[Future[String]]

    case object Finish

    class EncodeActor extends Actor {
      val video = File.createTempFile("HSReplay", ".mp4").getAbsolutePath
      var time = 0.0
      var closed = false
      var writer: IMediaWriter = _

      info(s"writing to $video")

      def receive = {
        case bi: BufferedImage =>
          if (!closed) {
            try {
              val resized = resize(bi, videoWidth, videoHeight)
              if (writer == null) {
                writer = createWriter(resized)
              }
              writer.encodeVideo(0, resized, time.toInt, TimeUnit.MILLISECONDS)
              time += 1000 / framesPerSec
              debug(s"encoded until $time ms")
            } catch {
              case NonFatal(e) => warn(s"could not encode an image into video", e)
              //normally only happens with screenshots used in tests
            }
          }
        case Finish =>
          if (!closed) {
            closed = true
            writer.close()
            info(s"Video $video finished encoding")
            sender ! video
          }
      }

      def resize(bi: BufferedImage, x: Int, y: Int): BufferedImage = {
        val (h, w) = (bi.getHeight, bi.getWidth)
        val filtered =
          if (w <= x && h <= y) bi
          else {
            val scale = Math.min(x.toFloat / w, y.toFloat / h)
            val at = AffineTransform.getScaleInstance(scale, scale)
            val scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR)
            scaleOp.filter(bi, null)
          }
        val res = new BufferedImage(filtered.getWidth, filtered.getHeight(), BufferedImage.TYPE_3BYTE_BGR) // type required by xuggle
        res.getGraphics.drawImage(filtered, 0, 0, null)
        res
      }

      private def createWriter(bi: BufferedImage) = {
        val writer = ToolFactory.makeWriter(video)
        val w = if (bi.getWidth % 2 == 0) bi.getWidth else bi.getWidth + 1
        val h = if (bi.getHeight % 2 == 0) bi.getHeight else bi.getHeight + 1
        writer.addVideoStream(0, 0, IRational.make((framesPerSec * 1000).toInt, 1000), w, h)
        writer
      }
    }
  }
}
