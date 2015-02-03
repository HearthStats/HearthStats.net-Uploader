package net.hearthstats.game

case class GameZone(zone: Zone.Zone, friendly: Boolean)

object Zone extends Enumeration {
  type Zone = Value

  val Graveyard = Value("GRAVEYARD")
  val Secret = Value("SECRET")
  val Hand = Value("HAND")
  val Setaside = Value("SETASIDE")
  val Deck = Value("DECK")
  val Play = Value("PLAY")
  val PlayHero = Value("PLAY (Hero)")
  val PlayPower = Value("PLAY (Hero Power)")
  val PlayWeapon = Value("PLAY (Weapon)")
  val Empty = Value("")
}