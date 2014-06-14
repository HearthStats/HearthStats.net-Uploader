package net.hearthstats.config;

public enum MonitoringMethod {
    SCREEN, SCREEN_LOG;

  public static MonitoringMethod getDefault() {
        return SCREEN;
    }
}