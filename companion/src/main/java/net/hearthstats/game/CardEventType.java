package net.hearthstats.game;

public enum CardEventType {
	ADDED_TO_DECK, 
  DRAWN,
	REPLACED, 
	PLAYED,
	RETURNED,
  PUT_IN_PLAY,
  RECEIVED,
  REVEALED, // secret
  SETASIDE, // Hex
  DESTROYED,
  DISCARDED_FROM_DECK,
  ATTACKING,
  ATTACKED,
	DISCARDED;
}
