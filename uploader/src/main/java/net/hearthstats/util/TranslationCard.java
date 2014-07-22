package net.hearthstats.util;

import net.hearthstats.config.GameLanguage;

import java.util.ResourceBundle;


public final class TranslationCard {
  private TranslationCard() {}


  private static ResourceBundle _bundle = null;


  public static void changeTranslation(GameLanguage language) {
    switch (language) {
      case FR:
        _bundle = ResourceBundle.getBundle("net.hearthstats.resources.card.cardFr", new UTF8Control());
        break;
      default:
        _bundle = null;
        break;
    }
  }


  public static String t(String key) {
    return _bundle.getString("card" + key);
  }


  public static Boolean hasKey(String key) {
    if (_bundle == null)
      return false;
    return _bundle.containsKey("card" + key);
  }
}
