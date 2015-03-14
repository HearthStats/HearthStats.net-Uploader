package net.hearthstats.ui.util

import java.awt.Dimension
import javax.swing.event.{DocumentEvent, DocumentListener}

import net.hearthstats.config.ConfigValue

import scala.swing.{PasswordField, Swing, TextField}

trait OptionTextField[T] { self: TextField =>

  def config: ConfigValue[T]
  def converter: String => T

  peer.setText(config.get.toString)

  val s = new Dimension(280, 28)
  minimumSize = s
  preferredSize = s

  peer.getDocument.addDocumentListener(new DocumentListener {
    def insertUpdate(e: DocumentEvent): Unit = handleChange
    def changedUpdate(e: DocumentEvent): Unit = handleChange
    def removeUpdate(e: DocumentEvent): Unit = handleChange

    def handleChange = Swing.onEDT({
      if (config.get != peer.getText) {
        config.set(converter(peer.getText))
      }
    })
  })
}

class StringOptionTextField(val config: ConfigValue[String])
  extends TextField with OptionTextField[String] {

  def converter = identity
}

class PasswordOptionTextField(val config: ConfigValue[String])
  extends PasswordField with OptionTextField[String] {

  def converter = identity
}


class IntOptionTextField(val config: ConfigValue[Int])
  extends TextField with OptionTextField[Int] {
  
  def converter = Integer.parseInt
}