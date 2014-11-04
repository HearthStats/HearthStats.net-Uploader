package net.hearthstats.game

import grizzled.slf4j.Logging
import net.hearthstats.game.CardEvents._
import net.hearthstats.core.HeroClass

class LogParser extends Logging {

  def analyseLine(line: String): Option[GameEvent] = {
    line match {
      case ZONE_PROCESSCHANGES_REGEX(zoneId, local, card, id, cardZone, zonePos, cardId, player, fromZone, toZone) =>
        debug(s"HS Zone uiLog: zoneId=$zoneId local=$local cardName=$card id=$id cardZone=$cardZone zonePos=$zonePos cardId=$cardId player=$player fromZone=$fromZone toZone=$toZone")
        analyseCard(cardZone, fromZone, toZone, card, cardId, player.toInt, id.toInt)
      case HIDDEN_REGEX(zoneId, local, id, cardId, _, cardZone, zonePos, player, fromZone, toZone) =>
        debug(s"HS Zone uiLog: zoneId=$zoneId local=$local cardName=HIDDEN id=$id cardZone=$cardZone zonePos=$zonePos cardId=$cardId player=$player fromZone=$fromZone toZone=$toZone")
        analyseCard(cardZone, fromZone, toZone, "", cardId, player.toInt, id.toInt)
      case TURN_CHANGE_REGEX() =>
        debug("turn passed")
        Some(TurnPassedEvent)
      case HERO_POWER_USE_REGEX(cardId, player) =>
        debug("Hero Power")
        Some(HeroPowerEvent(cardId, player.toInt))
      // Note : emitted at game start + several times at each use, need to filter !
      case _ =>
        // ignore line
        None
    }
  }

  type ZoneToEvent = PartialFunction[(String, String), GameEvent]

  def analyseCard(
    cardZone: String,
    fromZone: String,
    toZone: String,
    card: String,
    cardId: String,
    player: Int,
    id: Int): Option[GameEvent] = {

    def analyseHandZone: ZoneToEvent = _ match {
      case ("", "FRIENDLY HAND") | ("", "OPPOSING HAND") =>
        CoinReceived(cardId, player)
      case ("OPPOSING DECK", "OPPOSING HAND") | ("FRIENDLY DECK", "FRIENDLY HAND") =>
        CardDrawn(card, id, player)
      case ("FRIENDLY HAND", _) =>
        CardPlayed(card, id, player)
      case ("FRIENDLY PLAY", "FRIENDLY HAND") =>
        CardReturned(card, id, player)
    }

    def analysePlayZone: ZoneToEvent = _ match {
      case ("", "FRIENDLY PLAY") | ("", "FRIENDLY PLAY (Weapon)") | ("", "OPPOSING PLAY") | ("", "OPPOSING PLAY (Weapon)") =>
        CardPutInPlay(card, id, player)
      case ("", "FRIENDLY PLAY (Hero)") =>
        HeroChosen(card, HERO_CLASSES(cardId), opponent = false, player)
      case ("", "OPPOSING PLAY (Hero)") =>
        HeroChosen(card, HERO_CLASSES(cardId), opponent = true, player)
      case ("", "FRIENDLY PLAY (Hero Power)") | ("", "OPPOSING PLAY (Hero Power)") =>
        HeroPowerDeclared(cardId, player)
      case ("OPPOSING HAND", _) =>
        CardPlayed(card, id, player)
    }

    def analyseGraveyardZone: ZoneToEvent = _ match {
      case ("", "FRIENDLY GRAVEYARD") | ("", "OPPOSING GRAVEYARD") | ("FRIENDLY HAND", "FRIENDLY GRAVEYARD") | ("OPPOSING HAND", "OPPOSING GRAVEYARD") =>
        CardDiscarded(card, id, player)
      case ("FRIENDLY PLAY", "FRIENDLY GRAVEYARD") |
        ("FRIENDLY PLAY (Weapon)", "FRIENDLY GRAVEYARD") |
        ("FRIENDLY SECRET", "FRIENDLY GRAVEYARD") |
        ("OPPOSING PLAY", "OPPOSING GRAVEYARD") |
        ("OPPOSING PLAY (Weapon)", "OPPOSING GRAVEYARD") |
        ("OPPOSING SECRET", "OPPOSING GRAVEYARD") =>
        CardDestroyed(card, id, player)
      case ("FRIENDLY PLAY (Hero)", "FRIENDLY GRAVEYARD") =>
        HeroDestroyedEvent(false)
      case ("OPPOSING PLAY (Hero)", "OPPOSING GRAVEYARD") =>
        HeroDestroyedEvent(true)
    }

    def analyseDeckZone: ZoneToEvent = _ match {
      case ("", "FRIENDLY DECK") | ("", "OPPOSING DECK") =>
        CardAddedToDeck(card, id, player)
      case ("OPPOSING HAND", "OPPOSING DECK") | ("FRIENDLY HAND", "FRIENDLY DECK") =>
        CardReplaced(card, id, player)
    }

    def analyseSecretZone: ZoneToEvent = _ match {
      case ("OPPOSING DECK", "OPPOSING SECRET") =>
        CardPutInPlay(card, id, player)
    }

    val zoneToEvent: ZoneToEvent = cardZone match {
      case "DECK" => analyseDeckZone
      case "HAND" => analyseHandZone
      case "PLAY" => analysePlayZone
      case "GRAVEYARD" => analyseGraveyardZone
      case "SECRET" => analyseSecretZone
    }
    zoneToEvent.lift(fromZone, toZone) match {
      case None =>
        warn(s"Unhandled log for $card: zone $cardZone from $fromZone to $toZone")
        None
      case e => e
    }
  }

  val ZONE_PROCESSCHANGES_REGEX = """\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[name=(.*) id=(\d*) zone=(.*) zonePos=(\d*) cardId=(.*) player=(\d*)\] zone from (.*) -> (.*)""".r
  val HIDDEN_REGEX = """\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[id=(\d*) cardId=(.*) type=(.*) zone=(.*) zonePos=(\d*) player=(\d*)\] zone from (.*) -> (.*)""".r
  val TURN_CHANGE_REGEX = """\[Zone\] ZoneChangeList.ProcessChanges\(\) - processing index=.* change=powerTask=\[power=\[type=TAG_CHANGE entity=\[id=.* cardId= name=GameEntity\] tag=NEXT_STEP value=MAIN_ACTION\] complete=False\] entity=GameEntity srcZoneTag=INVALID srcPos= dstZoneTag=INVALID dstPos=""".r
  val HERO_POWER_USE_REGEX = """\[Power\].*cardId=(\w+).*player=(\d+).*""".r

  import HeroClass._
  val HERO_CLASSES = Map(
    "HERO_09" -> PRIEST,
    "HERO_03" -> ROGUE,
    "HERO_08" -> MAGE,
    "HERO_04" -> PALADIN,
    "HERO_01" -> WARRIOR,
    "HERO_07" -> WARLOCK,
    "HERO_05" -> HUNTER,
    "HERO_02" -> SHAMAN,
    "HERO_06" -> DRUID)
    .withDefaultValue(UNDETECTED) // to handle either Solo adventures or Lord Jarraxxus
}