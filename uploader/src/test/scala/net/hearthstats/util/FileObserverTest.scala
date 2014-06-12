package net.hearthstats.util

import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FileObserverSpec extends FlatSpec with Matchers {

  "A FileObserver" should "notify observers of content changed" in {
    val f = File.createTempFile("prefa", "aazeae")
    val obs = FileObserver(f)
    var read: String = ""
    obs.observable.subscribe(c => read = c)
    val writer = new BufferedWriter(new FileWriter(f))
    writer.write("content\n")
    writer.close()
    Thread.sleep(obs.DEFAULT_DELAY_MS * 2)
    read should ===("content")
  }
}