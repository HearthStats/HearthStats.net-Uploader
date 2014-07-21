package net.hearthstats.config;

public enum GameLanguage {
  EN, FR;

  public static GameLanguage getDefault() {
    return EN;
  }
}