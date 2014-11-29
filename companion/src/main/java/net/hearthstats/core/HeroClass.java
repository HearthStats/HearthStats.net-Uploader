package net.hearthstats.core;

public enum HeroClass {
  UNDETECTED("- undetected -"), 
  DRUID("Druid"), 
  HUNTER("Hunter"), 
  MAGE("Mage"), 
  PALADIN("Paladin"), 
  PRIEST("Priest"), 
  ROGUE("Rogue"), 
  SHAMAN("Shaman"), 
  WARLOCK("Warlock"),
  WARRIOR("Warrior");

  private final String name;

  HeroClass(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  public static HeroClass byName(String name) {
    for (HeroClass e : values()) {
      if(e.name.equals(name)) return e;
    }
    return UNDETECTED;
  }

  public static String stringWithId(int id) {
    return values()[id].toString();
  }
}
