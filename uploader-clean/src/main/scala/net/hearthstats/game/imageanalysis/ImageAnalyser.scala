package net.hearthstats.game.imageanalysis

import net.hearthstats.game.ocr.RankLevelOcr
import java.awt.image.BufferedImage

trait ImageAnalyser {
  val individualPixelAnalyser = new IndividualPixelAnalyser
  val rankLevelOcr = new RankLevelOcr

  protected def identify[T](image: BufferedImage, pixelRules: Iterable[(Array[UniquePixel], T)]): Option[T] =
    (for {
      (pixels, result) <- pixelRules
      if individualPixelAnalyser.testAllPixelsMatch(image, pixels)
    } yield result).headOption
}