package net.hearthstats.ui.util

import java.awt.font.TextAttribute
import java.awt.{Font, GraphicsEnvironment}
import java.text.AttributedCharacterIterator
import java.util.Collections

import grizzled.slf4j.Logging

import scala.swing.Font

/**
 * Create font objects in a cross-platform way. Windows and OS X do not have the same fonts installed by default,
 * so this will return the most appropriate font found on the system.
 */
object FontHelper extends Logging {

  val preferredFontNames = Array("Helvetica Neue", "Helvetica", "Open Sans", "Arial")

  val weightLight: Map[_ <: AttributedCharacterIterator.Attribute, _] =
    Map(TextAttribute.WEIGHT -> TextAttribute.WEIGHT_LIGHT)

  lazy val fontName = {
    val fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment.getAvailableFontFamilyNames().toSet;
    // Find the best matching font; or in the very unlikely event of there being no Helvetica or Arial, use Java's standard sans serif font
    preferredFontNames.find(fontNames.contains(_)).getOrElse("SansSerif")
  }

  lazy val bodyFont = new Font(fontName, Font.PLAIN, 14)

  lazy val actionFont = new Font(fontName, Font.BOLD, 14)

  lazy val largerFont = new Font(fontName, Font.PLAIN, 16)

  lazy val titleFont = new Font(fontName, Font.PLAIN, 24)
    .deriveFont(Collections.singletonMap(TextAttribute.WEIGHT, TextAttribute.WEIGHT_LIGHT))

}
