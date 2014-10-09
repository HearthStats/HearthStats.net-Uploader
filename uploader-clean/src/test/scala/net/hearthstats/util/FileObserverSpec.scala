package net.hearthstats.util

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FileObserverSpec extends FlatSpec with Matchers with OneInstancePerTest {

  "A FileObserver" should "notify observers of content changed" in {
    val f = File.createTempFile("prefa", "aazeae")
    val obs = FileObserver(f)
    var read = ""
    obs.observable.subscribe(c => read = c)
    val writer = new BufferedWriter(new FileWriter(f))
    writer.write("content\n")
    writer.close()
    Thread.sleep(4 * obs.DEFAULT_DELAY_MS)
    read shouldBe "content"
  }
}