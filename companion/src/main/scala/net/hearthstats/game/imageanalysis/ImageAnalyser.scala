package net.hearthstats.game.imageanalysis

import net.hearthstats.game.ocr.RankLevelOcr
import java.awt.image.BufferedImage
import grizzled.slf4j.Logging

trait ImageAnalyser extends Logging {
  protected val individualPixelAnalyser = new IndividualPixelAnalyser
  protected val rankLevelOcr = new RankLevelOcr

  protected def identify[T](image: BufferedImage, pixelRules: Iterable[(Array[UniquePixel], T)]): Option[T] =
    (for {
      (pixels, result) <- pixelRules
      if individualPixelAnalyser.testAllPixelsMatch(image, pixels)
    } yield result).headOption
}