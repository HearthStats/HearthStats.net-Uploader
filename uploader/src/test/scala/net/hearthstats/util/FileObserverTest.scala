package net.hearthstats.util

import org.junit.Test
import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter

class FileObserverTest {

  @Test def checkForChanges() {
    val f = File.createTempFile("prefa", "aazeae")
    val obs = FileObserver(f)
    var read: String = ""
    obs.observable.subscribe(c => read = c)
    val writer = new BufferedWriter(new FileWriter(f))
    writer.write("content\n")
    writer.close()
    Thread.sleep(obs.DEFAULT_DELAY_MS)
    assert(read == "content", read)
  }
}