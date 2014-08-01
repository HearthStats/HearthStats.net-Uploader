package net.hearthstats.core;

public enum GameMode {
  ARENA("Arena"), 
  CASUAL("Casual"),
  RANKED("Ranked"),
  PRACTICE("Practice"),
  FRIENDLY("Friendly");

  private final String name;

  GameMode(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

}
