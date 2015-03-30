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
  SETASIDE, // Hex, Tracking
  DESTROYED,
  DISCARDED_FROM_DECK,
  PLAYED_FROM_DECK,  // mad scientist, deathlord
  ATTACKING,
  ATTACKED,
  IGNORED, // card discarded from play (after a spell is played)
	DISCARDED;
}
