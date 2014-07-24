package net.hearthstats.util

trait TranslationComponent { self: TranslationConfigComponent =>
  val translation: Translation

  trait Translation {
    def t(key: String): String

    def has(key: String): Boolean

    def opt(key: String): Option[String]
  }

}

trait TranslationConfigComponent {
  val translationConfig: TranslationConfig

  trait TranslationConfig {
    val bundle: String
    val language: String
  }
  case class SimpleTranslationConfig(bundle: String, language: String)
}