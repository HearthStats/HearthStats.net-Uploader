package net.hearthstats.core;

public enum MatchOutcome {
  DEFEAT("Loss"), VICTORY("Win"), DRAW("Draw"); // DRAW is not handled yet

  private final String name;

  MatchOutcome(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

}
