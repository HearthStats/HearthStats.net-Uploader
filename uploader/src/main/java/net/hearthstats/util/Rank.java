package net.hearthstats.util;

public enum Rank {

    // @formatter:off

    RANK_25 (25, "Angry Chicken"),
    RANK_24 (24, "Leper Gnome"),
    RANK_23 (23, "Argent Squire"),
    RANK_22 (22, "Murloc Raider"),
    RANK_21 (21, "Southsea Deckhand"),
    RANK_20 (20, "Shieldbearer"),
    RANK_19 (19, "Novice Engineer"),
    RANK_18 (18, "Sorcerer's Apprentice"),
    RANK_17 (17, "Tauren Warrior"),
    RANK_16 (16, "Questing Adventurer"),
    RANK_15 (15, "Silvermoon Guardian"),
    RANK_14 (14, "Raid Leader"),
    RANK_13 (13, "Dread Corsair"),
    RANK_12 (12, "Warsong Commander"),
    RANK_11 (11, "Big Game Hunter"),
    RANK_10 (10, "Ogre Magi"),
    RANK_9  ( 9, "Silver Hand Knight"),
    RANK_8  ( 8, "Frostwolf Warlord"),
    RANK_7  ( 7, "Sunwalker"),
    RANK_6  ( 6, "Ancient of War"),
    RANK_5  ( 5, "Sea Giant"),
    RANK_4  ( 4, "Moutain Giant"),
    RANK_3  ( 3, "Molten Giant"),
    RANK_2  ( 2, "The Black Knight"),
    RANK_1  ( 1, "Innkeeper"),
    LEGEND  ( 0, "Legend");

    // @formatter:on

    public final int number;
    public final String title;
    public final String description;

    Rank(int number, String title) {
        this.number = number;
        this.title = title;
        this.description = number == 0 ? title : (number + " - " + title);
    }


    @Override
    public String toString() {
        return description;
    }

    public static Rank fromInt(int number) {
        if (number < 1 || number > 25) {
            return null;
        } else {
            return values()[25 - number];
        }
    }
}
