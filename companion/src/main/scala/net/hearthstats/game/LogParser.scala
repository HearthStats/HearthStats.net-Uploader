package net.hearthstats.game

import grizzled.slf4j.Logging
import net.hearthstats.game.CardEvents._

class LogParser extends Logging {
  val ZONE_PROCESSCHANGES_REGEX = """^\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[name=(.*) id=(\d*) zone=(.*) zonePos=(\d*) cardId=(.*) player=(\d*)\] zone from (.*) -> (.*)""".r
  val TURN_CHANGE_REGEX = """\[Zone\] ZoneChangeList.ProcessChanges\(\) - processing index=.* change=powerTask=\[power=\[type=TAG_CHANGE entity=\[id=.* cardId= name=GameEntity\] tag=NEXT_STEP value=MAIN_ACTION\] complete=False\] entity=GameEntity srcZoneTag=INVALID srcPos= dstZoneTag=INVALID dstPos=""".r

  def analyseLine(line: String): Option[GameEvent] = {
    line match {
      case ZONE_PROCESSCHANGES_REGEX(zoneId, local, card, id, cardZone, zonePos, cardId, player, fromZone, toZone) =>
        debug(s"HS Zone uiLog: zoneId=$zoneId local=$local cardName=$card id=$id cardZone=$cardZone zonePos=$zonePos cardId=$cardId player=$player fromZone=$fromZone toZone=$toZone")
        analyseCard(cardZone, fromZone, toZone, card, cardId)
      case TURN_CHANGE_REGEX() =>
        debug("turn passed")
        Some(TurnPassedEvent)
      case _ =>
        // ignore line
        None
    }
  }

  val heroId = """HERO_(\d+)""".r

  def analyseCard(cardZone: String, fromZone: String, toZone: String, card: String, cardId: String): Option[GameEvent] =
    (cardZone, fromZone, toZone) match {
      //TODO : detect hero power activation
      case ("DECK", "FRIENDLY HAND", "FRIENDLY DECK") =>
        Some(CardReplaced(card))
      case ("HAND", "", "FRIENDLY HAND") =>
        Some(CardDrawn(card))
      case ("HAND", "FRIENDLY DECK", "FRIENDLY HAND") =>
        Some(CardDrawn(card))
      case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY") =>
        Some(CardPlayed(card))
      case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY (Weapon)") =>
        Some(CardPlayed(card))
      case ("HAND", "FRIENDLY HAND", "FRIENDLY SECRET") =>
        Some(CardPlayed(card))
      case ("HAND", "FRIENDLY HAND", "") =>
        Some(CardPlayed(card))
      case ("HAND", "FRIENDLY PLAY", "FRIENDLY HAND") =>
        Some(CardReturned(card))
      case ("PLAY", "", "FRIENDLY PLAY") =>
        Some(CardPutInPlay(card))
      case ("PLAY", "", "FRIENDLY PLAY (Weapon)") =>
        Some(CardPutInPlay(card))
      case ("PLAY", "", "FRIENDLY PLAY (Hero)") =>
        val heroId(id) = cardId
        Some(HeroChosen(card, id.toInt, opponent = false))
      case ("PLAY", "", "FRIENDLY PLAY (Hero Power)") =>
        None // hero power declared
      case ("PLAY", "", "OPPOSING PLAY") =>
        Some(CardPutInPlay(card))
      case ("PLAY", "", "OPPOSING PLAY (Weapon)") =>
        Some(CardPutInPlay(card))
      case ("PLAY", "", "OPPOSING PLAY (Hero)") =>
        val heroId(id) = cardId
        Some(HeroChosen(card, id.toInt, opponent = true))
      case ("PLAY", "", "OPPOSING PLAY (Hero Power)") =>
        None // hero powered declared
      case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY") =>
        Some(CardPlayed(card))
      case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY (Weapon)") =>
        Some(CardPlayed(card))
      case ("PLAY", "OPPOSING HAND", "") =>
        Some(CardPlayed(card))
      case ("GRAVEYARD", "", "FRIENDLY GRAVEYARD") =>
        Some(CardDiscarded(card))
      case ("GRAVEYARD", "", "OPPOSING GRAVEYARD") =>
        Some(CardDiscarded(card))
      case ("GRAVEYARD", "FRIENDLY HAND", "FRIENDLY GRAVEYARD") =>
        Some(CardDiscarded(card))
      case ("GRAVEYARD", "FRIENDLY PLAY", "FRIENDLY GRAVEYARD") =>
        Some(CardDestroyed(card))
      case ("GRAVEYARD", "FRIENDLY PLAY (Weapon)", "FRIENDLY GRAVEYARD") =>
        Some(CardDestroyed(card))
      case ("GRAVEYARD", "FRIENDLY PLAY (Hero)", "FRIENDLY GRAVEYARD") =>
        Some(HeroDestroyedEvent(false))
      case ("GRAVEYARD", "FRIENDLY SECRET", "FRIENDLY GRAVEYARD") =>
        Some(CardDestroyed(card))
      case ("GRAVEYARD", "OPPOSING HAND", "OPPOSING GRAVEYARD") =>
        Some(CardDiscarded(card))
      case ("GRAVEYARD", "OPPOSING PLAY", "OPPOSING GRAVEYARD") =>
        Some(CardDestroyed(card))
      case ("GRAVEYARD", "OPPOSING PLAY (Weapon)", "OPPOSING GRAVEYARD") =>
        Some(CardDestroyed(card))
      case ("GRAVEYARD", "OPPOSING PLAY (Hero)", "OPPOSING GRAVEYARD") =>
        Some(HeroDestroyedEvent(true))
      case ("GRAVEYARD", "OPPOSING SECRET", "OPPOSING GRAVEYARD") =>
        Some(CardDestroyed(card))
      case _ =>
        warn(s"Unhandled log for $card: zone $cardZone from $fromZone to $toZone")
        None
    }
}