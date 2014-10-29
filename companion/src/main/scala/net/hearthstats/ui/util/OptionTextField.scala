package net.hearthstats.ui.util

import javax.swing.JTextField
import net.hearthstats.config.ConfigValue
import javax.swing.event.DocumentListener
import javax.swing.event.DocumentEvent
import scala.swing.Swing
import java.awt.Dimension

class OptionTextField[T](
  config: ConfigValue[T],
  converter: String => T)
  extends JTextField {

  setText(config.get.toString)
  val s = new Dimension(280, 28)
  setMinimumSize(s)
  setPreferredSize(s)

  getDocument.addDocumentListener(new DocumentListener {
    def insertUpdate(e: DocumentEvent): Unit = handleChange
    def changedUpdate(e: DocumentEvent): Unit = handleChange
    def removeUpdate(e: DocumentEvent): Unit = handleChange

    def handleChange = Swing.onEDT({
      if (config.get != getText) {
        config.set(converter(getText))
      }
    })
  })
}

class StringOptionTextField(config: ConfigValue[String])
  extends OptionTextField[String](
    config: ConfigValue[String],
    identity)

class IntOptionTextField(config: ConfigValue[Int])
  extends OptionTextField[Int](
    config: ConfigValue[Int],
    Integer.parseInt)