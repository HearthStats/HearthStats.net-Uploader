package net.hearthstats.game

import java.awt.image.BufferedImage
import net.hearthstats.game.imageanalysis.PixelLocation

object ScreenConfig {

  def getRatio(image: BufferedImage): Float =
    image.getHeight / PixelLocation.REFERENCE_SIZE.y.toFloat

  def getXOffset(image: BufferedImage, ratio: Float): Int =
    ((image.getWidth.toFloat - (ratio * PixelLocation.REFERENCE_SIZE.x)) / 2).toInt

}