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
import akka.pattern.ask
import com.xuggle.xuggler.video.ConverterFactory
import com.xuggle.xuggler.IPixelFormat
import com.xuggle.xuggler.video.ArgbConverter

class SequenceEncoder extends VideoEncoder with Logging {
  val system = ActorSystem("video")

  def newVideo(framesPerSec: Double, videoWidth: Int, videoHeight: Int) = new OngoingVideo {
    val actor = system.actorOf(Props(new EncodeActor))

    implicit val timeout = Timeout(2.seconds)

    def encodeImage(bi: BufferedImage, timeMs: Long): Future[Unit] = (actor ? (bi, timeMs)).mapTo[Unit]

    /**
     * Returns the name of the compressed file.
     */
    def finish(): Future[String] =
      actor.ask(Finish)(60 seconds).asInstanceOf[Future[String]]

    case object Finish

    class EncodeActor extends Actor {
      val video = File.createTempFile("HSReplay", ".mp4").getAbsolutePath
      var closed = false
      var writer: IMediaWriter = _

      def receive = {
        case (bi: BufferedImage, timeMs: Long) =>
          val encodingStart = System.nanoTime
          if (!closed) {
            try {
              val image = new BufferedImage(bi.getWidth, bi.getHeight(), BufferedImage.TYPE_3BYTE_BGR)
              image.getGraphics().drawImage(bi, 0, 0, null)
              if (writer == null) {
                writer = createWriter
                val w = image.getWidth
                val h = image.getHeight
                info(s"writing to $video : ${w}x$h @$framesPerSec")
              }
              writer.encodeVideo(0, image, timeMs, TimeUnit.MILLISECONDS)
              val duration = (System.nanoTime - encodingStart) / 1000000
              debug(s"encoded until $timeMs ms, took $duration ms")
            } catch {
              case NonFatal(e) => warn(s"could not encode an image into video", e)
              //normally only happens with screenshots used in tests
            }
          }
          sender ! ()
        case Finish =>
          if (!closed) {
            closed = true
            writer.close()
            info(s"Video $video finished encoding")
            sender ! video
          }
      }

      private def createWriter = {
        val writer = ToolFactory.makeWriter(video)
        writer.addVideoStream(0, 0, IRational.make((framesPerSec * 1000).toInt, 1000), videoWidth, videoHeight)
        writer
      }
    }
  }
}
