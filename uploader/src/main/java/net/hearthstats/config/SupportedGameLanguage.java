package net.hearthstats.config;

public enum SupportedGameLanguage {
  EN("en"), FR("fr");

  private final String name;

  private SupportedGameLanguage(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }
}
