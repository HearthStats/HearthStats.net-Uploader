package net.hearthstats.util

import java.util.ResourceBundle

trait PropertiesTranslationComponent { self: TranslationConfigComponent =>
  val translation = new PropertiesTranslation(self.translationConfig)

  class PropertiesTranslation(cfg: TranslationConfig) {
    val resourceBundle = ResourceBundle.getBundle(cfg.bundle, new UTF8Control);

    def t(key: String) = resourceBundle.getString(key)

    def has(key: String) = resourceBundle.containsKey(key)

    def opt(key: String) =
      if (has(key)) Some(t(key))
      else None
  }

}

import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLConnection
import java.util.Locale
import java.util.PropertyResourceBundle
import java.util.ResourceBundle

/**
 * Support for encoding properties files as UTF-8 instead of the default ISO-8859-1.
 * see http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
 */
class UTF8Control extends ResourceBundle.Control {

  override def newBundle(baseName: String,
    locale: Locale,
    format: String,
    loader: ClassLoader,
    reload: Boolean): ResourceBundle = {
    val bundleName = toBundleName(baseName, locale)
    val resourceName = toResourceName(bundleName, "properties")
    var bundle: ResourceBundle = null
    var stream: InputStream = null
    if (reload) {
      val url = loader.getResource(resourceName)
      if (url != null) {
        val connection = url.openConnection()
        if (connection != null) {
          connection.setUseCaches(false)
          stream = connection.getInputStream
        }
      }
    } else {
      stream = loader.getResourceAsStream(resourceName)
    }
    if (stream != null) {
      try {
        bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"))
      } finally {
        stream.close()
      }
    }
    bundle
  }
}
