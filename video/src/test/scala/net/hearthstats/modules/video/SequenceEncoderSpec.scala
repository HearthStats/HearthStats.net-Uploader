package net.hearthstats.modules.video

import java.awt.image.BufferedImage
import org.junit.runner.RunWith
import org.scalatest.{ FlatSpec, Matchers }
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SequenceEncoderSpec extends FlatSpec with Matchers {
  it should "keep image size for smaller images" in {
    testResize(1024, 768, 800, 600, 800, 600)
  }

  it should "keep aspect ratio" in {
    testResize(800, 600, 1000, 1000, 600, 600)
  }

  it should "resize to even height" in {
    testResize(800, 600, 1001, 1000, 601, 600)
    testResize(800, 600, 1000, 1001, 600, 600)
  }

  def testResize(toX: Int, toY: Int, fromX: Int, fromY: Int, expectedX: Int, expectedY: Int) = {
    val encoder = new SequenceEncoder
    val ongoing = encoder.newVideo()
    val (w, h) = (800, 600)
    val image = new BufferedImage(fromX, fromY, BufferedImage.TYPE_3BYTE_BGR)
    val resized = ongoing.resize(image, toX, toY)
    resized.getWidth shouldBe expectedX
    resized.getHeight shouldBe expectedY
  }

}