package net.hearthstats.config;

public enum GameLanguage {
    EU, FR;

  public static GameLanguage getDefault() {
        return EU;
    }
}