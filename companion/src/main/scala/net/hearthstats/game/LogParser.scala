package net.hearthstats.game

import grizzled.slf4j.Logging
import net.hearthstats.game.CardEvents._

class LogParser extends Logging {
  val ZONE_PROCESSCHANGES_REGEX = """^\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[name=(.*) id=(\d*) zone=(.*) zonePos=(\d*) cardId=(.*) player=(\d*)\] zone from (.*) -> (.*)""".r

  def analyseLine(line: String): Option[GameEvent] = {
    line match {
      case ZONE_PROCESSCHANGES_REGEX(zoneId, local, card, id, cardZone, zonePos, cardId, player, fromZone, toZone) =>

        debug("HS Zone uiLog: zoneId={} local={} cardName={} id={} cardZone={} zonePos={} cardId={} player={} fromZone={} toZone={}",
          zoneId, local, card, id, cardZone, zonePos, cardId, player, fromZone, toZone)

        (cardZone, fromZone, toZone) match {
          case ("DECK", "FRIENDLY HAND", "FRIENDLY DECK") =>
            Some(CardReplaced(card))
          case ("HAND", "", "FRIENDLY HAND") =>
            Some(CardDrawn(card))
          case ("HAND", "FRIENDLY DECK", "FRIENDLY HAND") =>
            Some(CardDrawn(card))
          case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY") =>
            None
          case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY (Weapon)") =>
            None
          case ("HAND", "FRIENDLY HAND", "FRIENDLY SECRET") =>
            None
          case ("HAND", "FRIENDLY HAND", "") =>
            None
          case ("HAND", "FRIENDLY PLAY", "FRIENDLY HAND") =>
            None
          case ("PLAY", "", "FRIENDLY PLAY") =>
            None
          case ("PLAY", "", "FRIENDLY PLAY (Weapon)") =>
            None
          case ("PLAY", "", "FRIENDLY PLAY (Hero Power)") =>
            None
          case ("PLAY", "", "OPPOSING PLAY") =>
            None
          case ("PLAY", "", "OPPOSING PLAY (Weapon)") =>
            None
          case ("PLAY", "", "OPPOSING PLAY (Hero Power)") =>
            None
          case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY") =>
            None
          case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY (Weapon)") =>
            None
          case ("PLAY", "OPPOSING HAND", "") =>
            None
          case ("GRAVEYARD", "", "FRIENDLY GRAVEYARD") =>
            None
          case ("GRAVEYARD", "", "OPPOSING GRAVEYARD") =>
            None
          case ("GRAVEYARD", "FRIENDLY HAND", "FRIENDLY GRAVEYARD") =>
            Some(CardDrawn(card))
          case ("GRAVEYARD", "FRIENDLY PLAY", "FRIENDLY GRAVEYARD") =>
            None
          case ("GRAVEYARD", "FRIENDLY PLAY (Weapon)", "FRIENDLY GRAVEYARD") =>
            None
          case ("GRAVEYARD", "FRIENDLY PLAY (Hero)", "FRIENDLY GRAVEYARD") =>
            Some(HeroDestroyedEvent(false))
          case ("GRAVEYARD", "FRIENDLY SECRET", "FRIENDLY GRAVEYARD") =>
            None
          case ("GRAVEYARD", "OPPOSING HAND", "OPPOSING GRAVEYARD") =>
            None
          case ("GRAVEYARD", "OPPOSING PLAY", "OPPOSING GRAVEYARD") =>
            None
          case ("GRAVEYARD", "OPPOSING PLAY (Weapon)", "OPPOSING GRAVEYARD") =>
            None
          case ("GRAVEYARD", "OPPOSING PLAY (Hero)", "OPPOSING GRAVEYARD") =>
            Some(HeroDestroyedEvent(true))
          case ("GRAVEYARD", "OPPOSING SECRET", "OPPOSING GRAVEYARD") =>
            None
          case _ =>
            debug("Unhandled log for {}: zone {} from {} to {}", card, cardZone, fromZone, toZone)
            None
        }
      case _ =>
        // ignore line
        None
    }
  }
}