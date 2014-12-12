package net.hearthstats.util

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FileObserverSpec extends FlatSpec with Matchers with OneInstancePerTest {

  ignore should "notify observers of content changed" in {
    val f = File.createTempFile("prefa", "aazeae")
    val obs = new FileObserver
    obs.start(f)
    var read = ""
    obs.addReceive {
      case l: String => read = l
    }
    val writer = new BufferedWriter(new FileWriter(f))
    writer.write("Димотариус\n")
    writer.close()
    Thread.sleep(4 * obs.DEFAULT_DELAY_MS)
    read shouldBe "Димотариус"
  }
}