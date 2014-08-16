package net.hearthstats.config;

public enum GameLanguage {
  EN, FR, RU;

  public static GameLanguage getDefault() {
    return EN;
  }
}