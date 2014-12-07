package net.hearthstats.game

import grizzled.slf4j.Logging
import net.hearthstats.game.CardEvents._
import net.hearthstats.core.HeroClass
import net.hearthstats.core.GameMode
import net.hearthstats.core.MatchOutcome

class LogParser extends Logging {

  def analyseLine(line: String): Option[GameEvent] = {
    line match {
      case STARTUP_REGEX() =>
        Some(StartupEvent)
      case BEGIN_SPECTATOR_REGEX() =>
        Some(BeginSpectatorEvent)
      case END_SPECTATOR_REGEX() =>
        Some(EndSpectatorEvent)
      case RANKED_MODE_REGEX() =>
        Some(GameModeDetected(GameMode.RANKED))
      case ARENA_MODE_REGEX() =>
        Some(GameModeDetected(GameMode.ARENA))
      case GAME_MODE_REGEX(mode) =>
        GAME_MODES.get(mode) map GameModeDetected
      case LEGEND_RANK_REGEX(rank) => // TODO : test this on a legend log file ...
        Some(LegendRank(rank.toInt))
      case POWER_TAG_CHANGE_REGEX(name, "PLAYER_ID", id) =>
        Some(PlayerName(name, id.toInt))
      case POWER_TAG_CHANGE_REGEX(name, "FIRST_PLAYER", "1") =>
        Some(FirstPlayer(name))
      case POWER_TAG_CHANGE_REGEX(player, "PLAYSTATE", "WON") =>
        Some(GameOver(player, MatchOutcome.VICTORY))
      case POWER_TAG_CHANGE_REGEX(player, "PLAYSTATE", "LOST") =>
        Some(GameOver(player, MatchOutcome.DEFEAT))
      case POWER_TAG_CHANGE_REGEX(player, "TURN_START", time) if player != "GameEntity" =>
        Some(TurnStart(player, time.toInt))
      case POWER_TAG_CHANGE_REGEX(_, "TURN", turn) =>
        Some(TurnCount(turn.toInt))
      case ZONE_PROCESSCHANGES_REGEX(zoneId, local, card, id, cardZone, zonePos, cardId, player, fromZone, toZone) =>
        debug(s"zoneId=$zoneId local=$local cardName=$card id=$id cardZone=$cardZone zonePos=$zonePos cardId=$cardId player=$player fromZone=$fromZone toZone=$toZone")
        analyseCard(cardZone, fromZone, toZone, card, cardId, player.toInt, id.toInt)
      case HIDDEN_REGEX(zoneId, local, id, cardId, _, cardZone, zonePos, player, fromZone, toZone) =>
        debug(s"uiLog: zoneId=$zoneId local=$local cardName=HIDDEN id=$id cardZone=$cardZone zonePos=$zonePos cardId=$cardId player=$player fromZone=$fromZone toZone=$toZone")
        analyseCard(cardZone, fromZone, toZone, "", cardId, player.toInt, id.toInt)
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
        CardReceived(cardId, id, player)
      case ("OPPOSING DECK", "OPPOSING HAND") | ("FRIENDLY DECK", "FRIENDLY HAND") =>
        CardDrawn(cardId, id, player)
      case ("FRIENDLY HAND", _) =>
        CardPlayed(cardId, id, player)
      case ("FRIENDLY PLAY", "FRIENDLY HAND") =>
        CardReturned(cardId, id, player)
    }

    def analysePlayZone: ZoneToEvent = _ match {
      case ("", "FRIENDLY PLAY") | ("", "FRIENDLY PLAY (Weapon)") | ("", "OPPOSING PLAY") | ("", "OPPOSING PLAY (Weapon)") =>
        CardPutInPlay(cardId, id, player)
      case ("", "FRIENDLY PLAY (Hero)") =>
        HERO_CLASSES.get(cardId) match {
          case Some(hero) => MatchStart(HeroChosen(cardId, hero, opponent = false, player))
          case None => HeroChosen(cardId, HeroClass.UNDETECTED, opponent = false, player) // either Naxx computer of Jaraxxus
        }
      case ("", "OPPOSING PLAY (Hero)") =>
        HERO_CLASSES.get(cardId) match {
          case Some(hero) => MatchStart(HeroChosen(cardId, hero, opponent = true, player))
          case None => HeroChosen(cardId, HeroClass.UNDETECTED, opponent = true, player) // either Naxx computer of Jaraxxus
        }
      case ("", "FRIENDLY PLAY (Hero Power)") | ("", "OPPOSING PLAY (Hero Power)") =>
        HeroPowerDeclared(cardId, player)
      case ("OPPOSING HAND", _) =>
        CardPlayed(cardId, id, player)
    }

    def analyseGraveyardZone: ZoneToEvent = _ match {
      case ("", "FRIENDLY GRAVEYARD") | ("", "OPPOSING GRAVEYARD") | ("FRIENDLY HAND", "FRIENDLY GRAVEYARD") | ("OPPOSING HAND", "OPPOSING GRAVEYARD") =>
        CardDiscarded(cardId, id, player)
      case ("FRIENDLY PLAY", "FRIENDLY GRAVEYARD") |
        ("FRIENDLY PLAY (Weapon)", "FRIENDLY GRAVEYARD") |
        ("OPPOSING PLAY", "OPPOSING GRAVEYARD") |
        ("OPPOSING PLAY (Weapon)", "OPPOSING GRAVEYARD") =>
        CardDestroyed(cardId, id, player)
      case ("FRIENDLY SECRET", "FRIENDLY GRAVEYARD") | ("OPPOSING SECRET", "OPPOSING GRAVEYARD") =>
        CardRevealed(cardId, id, player)
      case ("FRIENDLY PLAY (Hero)", "FRIENDLY GRAVEYARD") =>
        HeroDestroyedEvent(false)
      case ("OPPOSING PLAY (Hero)", "OPPOSING GRAVEYARD") =>
        HeroDestroyedEvent(true)
      case ("OPPOSING DECK", "OPPOSING GRAVEYARD") |
        ("FRIENDLY DECK", "FRIENDLY GRAVEYARD") =>
        CardDiscardedFromDeck(cardId, id, player)
    }

    def analyseSetasideZone: ZoneToEvent = _ match {
      case ("OPPOSING PLAY", "") | ("FRIENDLY PLAY", "") =>
        CardSetAside(cardId, id, player)
    }

    def analyseDeckZone: ZoneToEvent = _ match {
      case ("", "FRIENDLY DECK") | ("", "OPPOSING DECK") =>
        CardAddedToDeck(cardId, id, player)
      case ("OPPOSING HAND", "OPPOSING DECK") | ("FRIENDLY HAND", "FRIENDLY DECK") =>
        CardReplaced(cardId, id, player)
    }

    def analyseSecretZone: ZoneToEvent = _ match {
      case ("OPPOSING HAND", "OPPOSING SECRET") | ("FRIENDLY HAND", "FRIENDLY SECRET") =>
        CardPlayed(cardId, id, player)
      case ("OPPOSING DECK", "OPPOSING SECRET") =>
        CardPutInPlay(cardId, id, player)
      case ("", "OPPOSING SECRET") | ("", "FRIENDLY SECRET") =>
        CardRevealed(cardId, id, player)
    }

    val zoneToEvent: ZoneToEvent = cardZone match {
      case "DECK" => analyseDeckZone
      case "HAND" => analyseHandZone
      case "PLAY" => analysePlayZone
      case "GRAVEYARD" => analyseGraveyardZone
      case "SETASIDE" => analyseSetasideZone
      case "SECRET" => analyseSecretZone
    }
    zoneToEvent.lift(fromZone, toZone) match {
      case None =>
        warn(s"Unhandled log for $card: zone $cardZone from $fromZone to $toZone")
        None
      case e => e
    }
  }

  val POWER_TAG_CHANGE_REGEX = """\[Power\] GameState.DebugPrintPower\(\) -\s*TAG_CHANGE Entity=(.*) tag=(.*) value=(.*)""".r
  val ZONE_PROCESSCHANGES_REGEX = """\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[name=(.*) id=(\d*) zone=(.*) zonePos=(\d*) cardId=(.*) player=(\d*)\] zone from (.*) -> (.*)""".r
  val HIDDEN_REGEX = """\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[id=(\d*) cardId=(.*) type=(.*) zone=(.*) zonePos=(\d*) player=(\d*)\] zone from (.*) -> (.*)""".r
  val HERO_POWER_USE_REGEX = """\[Power\].*cardId=(\w+).*player=(\d+).*""".r
  val GAME_MODE_REGEX = """\[Bob\] ---(\w+)---""".r
  val ARENA_MODE_REGEX = """\[LoadingScreen\]  LoadingScreen.OnSceneLoaded\(\) - prevMode=.* currMode=DRAFT""".r
  val RANKED_MODE_REGEX = ".*name=rank_window.*".r
  val LEGEND_RANK_REGEX = """\[Bob\] legend rank (\d*)""".r
  val STARTUP_REGEX = """^Initialize engine version.*""".r
  val BEGIN_SPECTATOR_REGEX = """\[Power\] .* Begin Spectating .*""".r
  val END_SPECTATOR_REGEX = """\[Power\] .* End Spectator Mode .*""".r

  import GameMode._
  val GAME_MODES = Map("RegisterScreenPractice" -> PRACTICE,
    "RegisterScreenTourneys" -> CASUAL,
    "RegisterScreenForge" -> ARENA,
    "RegisterScreenFriendly" -> FRIENDLY)
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
    .withDefaultValue(HeroClass.UNDETECTED) // to handle either Solo adventures or Lord Jarraxxus
}