package net.hearthstats.game;

public enum CardEventType {
  OPENED_WITH,
	ADDED_TO_DECK, 
  DRAWN,
	REPLACED, 
	PLAYED,
	RETURNED,
  PUT_IN_PLAY,
  RECEIVED,
  REVEALED, // secret
  SETASIDE, // Hex, Tracking
  DESTROYED,
  DISCARDED_FROM_DECK,
  PLAYED_FROM_DECK,  // mad scientist, deathlord
  CHOSEN,
  ATTACKING,
  ATTACKED,
	DISCARDED;
}
