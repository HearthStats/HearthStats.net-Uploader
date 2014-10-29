package net.hearthstats.util

import java.io.File
import java.io.FileWriter
import java.io.BufferedWriter
import org.scalatest._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.mock.MockitoSugar
import com.softwaremill.macwire.MacwireMacros.wire

@RunWith(classOf[JUnitRunner])
class TranslationSpec extends FlatSpec with Matchers with MockitoSugar {
  val config = TranslationConfig("net.hearthstats.resources.Main", "fr")
  val translation = wire[Translation]

  "A translation" should "handle properly parameters" in {
    translation.t("match.end.vs.named", "a", "b", "win") shouldBe "a vs. b (win)"
  }
}
