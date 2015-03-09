package net.hearthstats.ui.util

import javax.swing.JTextField
import net.hearthstats.config.ConfigValue
import javax.swing.event.DocumentListener
import javax.swing.event.DocumentEvent
import scala.swing.Swing
import java.awt.Dimension
import javax.swing.JPasswordField

trait OptionTextField[T] { self: JTextField =>

  def config: ConfigValue[T]
  def converter: String => T

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

class StringOptionTextField(val config: ConfigValue[String])
  extends JTextField with OptionTextField[String] {

  def converter = identity
}

class PasswordOptionTextField(val config: ConfigValue[String])
  extends JPasswordField with OptionTextField[String] {

  def converter = identity
}


class IntOptionTextField(val config: ConfigValue[Int])
  extends JTextField with OptionTextField[Int] {
  
  def converter = Integer.parseInt
}