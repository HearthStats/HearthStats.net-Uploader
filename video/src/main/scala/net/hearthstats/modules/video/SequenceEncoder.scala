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

class SequenceEncoder extends VideoEncoder with Logging {

  def newVideo(framesPerSec: Int = 10, videoWidth: Int = 800, videoHeight: Int = 600) =
    new SequenceEncoderOngoingVideo(framesPerSec, videoWidth, videoHeight)

  class SequenceEncoderOngoingVideo(framesPerSec: Int = 10, videoWidth: Int = 800, videoHeight: Int = 600) extends OngoingVideo {

    val video = File.createTempFile("HSReplay", ".mp4").getAbsolutePath
    info(s"writing to $video")

    var time = 0
    var closed = false
    var writer: IMediaWriter = _

    override def encodeImage(bi: BufferedImage): Unit = video.synchronized {
      if (!closed) {
        try {
          val resized = resize(bi, videoWidth, videoHeight)
          if (writer == null) {
            writer = createWriter(resized)
          }
          writer.encodeVideo(0, resized, time, TimeUnit.MILLISECONDS)
          time += 1000 / framesPerSec
          debug(s"encoded until $time ms")
        } catch {
          case NonFatal(e) => warn(s"could not encode an image into video", e)
          //normally only happens with screenshots used in tests
        }
      }
    }

    /**
     * Returns the name of the compressed file.
     */
    override def finish(): Future[String] = video.synchronized {
      if (!closed) {
        closed = true
        Future {
          writer.close()
          info(s"Video $video finished encoding")
          video
        }
      } else Promise[String].future // never completes
    }

    def resize(bi: BufferedImage, x: Int, y: Int): BufferedImage = {
      val (h, w) = (bi.getHeight, bi.getWidth)
      val filtered =
        if (w <= x && h <= y) bi
        else {
          val computedHeight = (h * Math.min(x.toFloat / w, y.toFloat / h)).toInt
          val targetHeight = if (computedHeight % 2 == 0) computedHeight else computedHeight + 1
          val scale = computedHeight / h.toFloat
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
      writer.addVideoStream(0, 0, IRational.make(framesPerSec, 1), bi.getWidth, bi.getHeight)
      writer
    }
  }
}
