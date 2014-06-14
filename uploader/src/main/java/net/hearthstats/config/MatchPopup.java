package net.hearthstats.config;

public enum MatchPopup {
    ALWAYS, INCOMPLETE, NEVER;

  public static MatchPopup getDefault() {
        return INCOMPLETE;
    }
}