package net.hearthstats.game

import rx.lang.scala.Observable
import rx.lang.scala.subscriptions._
import rx.subjects.PublishSubject
import rx.lang.scala.JavaConversions._
import net.hearthstats.util.FileObserver
import java.io.File
import com.softwaremill.macwire.MacwireMacros._
import net.hearthstats.hstatsapi.API
import net.hearthstats.config.UserConfig
import net.hearthstats.config.Environment
import net.hearthstats.util.FileObserver
import net.hearthstats.ui.log.Log
import grizzled.slf4j.Logging
import net.hearthstats.hstatsapi.CardUtils
import CardEvents._

trait LogMonitorModule {
  val config: UserConfig
  val api: API
  val cardUtils: CardUtils
  val environment: Environment
  val uiLog: Log
  lazy val fileObserver = FileObserver(new File(environment.hearthstoneLogFile))

  lazy val hsLogMonitor = wire[HearthstoneLogMonitor]
}

class HearthstoneLogMonitor(
  config: UserConfig,
  api: API,
  cardUtils: CardUtils,
  environment: Environment,
  uiLog: Log,
  fileObserver: FileObserver) extends GameEventProducer with Logging {

  import config._

  implicit val cardsTranslation = config.gameCardsTranslation

  val LOADING_SCREEN_PREFIX = "[LoadingScreen]"
  val ZONE_PREFIX = "[Zone]"
  val LS_ONSCENELOADED_REGEX = """^\[LoadingScreen\] LoadingScreen\.OnSceneLoaded\(\) \- prevMode=(\S*) nextMode=(\S*)""".r
  val ZONE_PROCESSCHANGES_REGEX = """^\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[name=(.*) id=(\d*) zone=(.*) zonePos=(\d*) cardId=(.*) player=(\d*)\] zone from (.*) -> (.*)""".r
  var screen = "GAMEPLAY"; // Assume we're in a game until proved otherwise... just in case a game is already in progress

  val cards = cardUtils.cards.values
  val cardPublisher = PublishSubject.create[CardEvent]

  val lines = fileObserver.observable.
    doOnNext(line => debug(s"found : [$line]")).
    doOnError(ex => uiLog.error("Error reading Hearthstone log: " + ex.getMessage, ex))
  val relevant = lines.filter(l => l != null && l.length > 0 && l.charAt(0) == '[')
  val screens = relevant.filter(l => l startsWith LOADING_SCREEN_PREFIX)
  val zones = relevant.filter(l => l startsWith ZONE_PREFIX)
  screens.doOnEach(handleLoadingScreen _)

  val gameEvents: Observable[GameEvent] = relevant.map(zoneEvent).filter(_.isDefined).map(_.get). // remove None values
    doOnNext(evt => debug(s"game event: $evt"))
  val cardEvents: Observable[CardEvent] = gameEvents.ofType(classOf[CardEvent])
  val heroEvents: Observable[HeroEvent] = gameEvents.ofType(classOf[HeroEvent])

  def stop(): Unit = {
    fileObserver.stop()
  }

  def handleLoadingScreen(line: String): Unit = {
    line match {
      case LS_ONSCENELOADED_REGEX(prev, next) =>
        debug(s"HS LoadingScreen uiLog: changed from $prev to $next")
        screen = next
      case _ => // no match
    }
  }

  def zoneEvent(line: String): Option[GameEvent] = {
    line match {
      case ZONE_PROCESSCHANGES_REGEX(zoneId, local, cardName, id, cardZone, zonePos, cardId, player, fromZone, toZone) =>

        debug("HS Zone uiLog: zoneId={} local={} cardName={} id={} cardZone={} zonePos={} cardId={} player={} fromZone={} toZone={}",
          zoneId, local, cardName, id, cardZone, zonePos, cardId, player, fromZone, toZone)

        val card = cards.find(_.name == cardName).headOption
        // Only log zone changes if we're actually in a game.
        // Some zone changes occur outside games which would result in misleading information if logged.
        if ("GAMEPLAY".equals(screen)) {
          (cardZone, fromZone, toZone) match {
            case ("DECK", "FRIENDLY HAND", "FRIENDLY DECK") =>
              // Put back into the deck... usually after replacing your starting hand
              uiLog.info("    You returned " + cardName + " to your deck")
              card map CardReplaced
            case ("HAND", "", "FRIENDLY HAND") =>
              // Received into your hand but not from your deck... usually The Coin
              uiLog.info("    You received " + cardName)
              card map CardDrawn
            case ("HAND", "FRIENDLY DECK", "FRIENDLY HAND") =>
              // Picked up into your hand
              uiLog.info("    You picked up " + cardName)
              card map CardDrawn
            case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY") =>
              // Your minion
              uiLog.info("    You played minion " + cardName)
              None
            case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY (Weapon)") =>
              uiLog.info("    You played weapon " + cardName)
              None
            case ("HAND", "FRIENDLY HAND", "FRIENDLY SECRET") =>
              uiLog.info("    You played secret " + cardName)
              None
            case ("HAND", "FRIENDLY HAND", "") =>
              uiLog.info("    You played spell " + cardName)
              None
            case ("HAND", "FRIENDLY PLAY", "FRIENDLY HAND") =>
              // Returned a card to your hand
              uiLog.info("    Your " + cardName + " was returned to your hand")
              None
            case ("PLAY", "", "FRIENDLY PLAY") =>
              // You received a minion (without playing card directly)
              uiLog.info("    You received minion " + cardName)
              None
            case ("PLAY", "", "FRIENDLY PLAY (Weapon)") =>
              // You received a weapon (without playing card directly)
              uiLog.info("    You received weapon " + cardName)
              None
            case ("PLAY", "", "FRIENDLY PLAY (Hero Power)") =>
              // Your hero power
              uiLog.info("    You played hero power " + cardName)
              None
            case ("PLAY", "", "OPPOSING PLAY") =>
              // Opponent received a minion (without playing card directly)
              uiLog.info("    Opponent received minion " + cardName)
              None
            case ("PLAY", "", "OPPOSING PLAY (Weapon)") =>
              // Opponent received a weapon (without playing card directly)
              uiLog.info("    Opponent received weapon " + cardName)
              None
            case ("PLAY", "", "OPPOSING PLAY (Hero Power)") =>
              // Opponent hero power
              uiLog.info("    Opponent played hero power " + cardName)
              None
            case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY") =>
              // Opponent minion
              uiLog.info("    Opponent played minion " + cardName)
              None
            case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY (Weapon)") =>
              // Opponent weapon
              uiLog.info("    Opponent played weapon " + cardName)
              None
            case ("PLAY", "OPPOSING HAND", "") =>
              // Opponent spell
              uiLog.info("    Opponent played spell " + cardName)
              None
            case ("GRAVEYARD", "", "FRIENDLY GRAVEYARD") =>
              // A card went directly to the graveyard, probably a spell
              debug(s"    Ignoring spell $cardName going to graveyard")
              None
            case ("GRAVEYARD", "", "OPPOSING GRAVEYARD") =>
              // A card went directly to the graveyard, probably a spell
              debug(s"    Ignoring spell $cardName going to graveyard")
              None
            case ("GRAVEYARD", "FRIENDLY HAND", "FRIENDLY GRAVEYARD") =>
              // Your card was discarded from your deck (hand full)
              uiLog.info("    Your " + cardName + " was discarded")
              card map CardDrawn
            case ("GRAVEYARD", "FRIENDLY PLAY", "FRIENDLY GRAVEYARD") =>
              // Your minion died
              uiLog.info("    Your " + cardName + " died")
              None
            case ("GRAVEYARD", "FRIENDLY PLAY (Weapon)", "FRIENDLY GRAVEYARD") =>
              // Your weapon is finished
              uiLog.info("    Your weapon " + cardName + " finished")
              None
            case ("GRAVEYARD", "FRIENDLY PLAY (Hero)", "FRIENDLY GRAVEYARD") =>
              // Your hero has died... you are defeated(?)
              info("    Your hero " + cardName + " has been defeated")
              Some(HeroDestroyedEvent(false))
            case ("GRAVEYARD", "FRIENDLY SECRET", "FRIENDLY GRAVEYARD") =>
              // Your secret was triggered... or possibly was destroyed?
              uiLog.info("    Your secret " + cardName + " was revealed")
              None
            case ("GRAVEYARD", "OPPOSING HAND", "OPPOSING GRAVEYARD") =>
              // Opponent card was discarded
              uiLog.info("    Opponent's " + cardName + " was discarded")
              None
            case ("GRAVEYARD", "OPPOSING PLAY", "OPPOSING GRAVEYARD") =>
              // Opponent minion died
              uiLog.info("    Opponent's " + cardName + " died")
              None
            case ("GRAVEYARD", "OPPOSING PLAY (Weapon)", "OPPOSING GRAVEYARD") =>
              // Opponent weapon is finished
              uiLog.info("    Opponent's weapon " + cardName + " finished")
              None
            case ("GRAVEYARD", "OPPOSING PLAY (Hero)", "OPPOSING GRAVEYARD") =>
              // Opponent's hero has died... you are victorious(?)
              info("    Opponent's hero " + cardName + " has been defeated")
              Some(HeroDestroyedEvent(true))
            case ("GRAVEYARD", "OPPOSING SECRET", "OPPOSING GRAVEYARD") =>
              uiLog.info("    Opponent's secret " + cardName + " was revealed")
              None
            case _ =>
              debug("Unhandled log for {}: zone {} from {} to {}", cardName, cardZone, fromZone, toZone)
              None
          }
        } else None
      case _ =>
        // ignore line
        None
    }
  }

  private def findCard(name: String) =
    cards.filter(_.name == name).headOption match {
      case None =>
        uiLog.warn(s"unkown card : $name")
        None
      case some => some
    }

}