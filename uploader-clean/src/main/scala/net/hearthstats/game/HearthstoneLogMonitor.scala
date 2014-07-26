package net.hearthstats.game

import org.slf4j.LoggerFactory
import rx.lang.scala.Observable
import rx.lang.scala.subscriptions._
import rx.subjects.PublishSubject
import rx.lang.scala.JavaConversions._
import org.apache.commons.logging.Log
import net.hearthstats.util.FileObserver
import java.io.File

//class HearthstoneLogMonitor extends GameEventProducer { self: ConfigComponent =>
//  import self.config._
//
//  val debugLog = LoggerFactory.getLogger(classOf[HearthstoneLogMonitor])
//  val LOADING_SCREEN_PREFIX = "[LoadingScreen]"
//  val ZONE_PREFIX = "[Zone]"
//  val LS_ONSCENELOADED_REGEX = """^\[LoadingScreen\] LoadingScreen\.OnSceneLoaded\(\) \- prevMode=(\S*) nextMode=(\S*)""".r
//  val ZONE_PROCESSCHANGES_REGEX = """^\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[name=(.*) id=(\d*) zone=(.*) zonePos=(\d*) cardId=(.*) player=(\d*)\] zone from (.*) -> (.*)""".r
//  var screen = "GAMEPLAY"; // Assume we're in a game until proved otherwise... just in case a game is already in progress
//
//  val cards = CardUtils.cards.values
//  val cardPublisher = PublishSubject.create[CardEvent]
//
//  val fileObserver = FileObserver(new File(gameLogFile))
//  val lines = fileObserver.observable.doOnError(ex => Log.error("Error reading Hearthstone log: " + ex.getMessage, ex))
//  val relevant = lines.filter(l => l != null && l.length > 0 && l.charAt(0) == '[')
//  val screens = relevant.filter(l => l startsWith LOADING_SCREEN_PREFIX)
//  val zones = relevant.filter(l => l startsWith ZONE_PREFIX)
//  screens.doOnEach(handleLoadingScreen _)
//
//  val gameEvents: Observable[GameEvent] = relevant.map(zoneEvent).filter(_.isDefined).map(_.get) // remove None values
//  val cardEvents: Observable[CardEvent] = gameEvents.ofType(classOf[CardEvent])
//  val heroEvents: Observable[HeroEvent] = gameEvents.ofType(classOf[HeroEvent])
//
//  def stop(): Unit = {
//    debugLog.debug(s"Stopping Hearthstone log monitor on file $gameLogFile")
//    fileObserver.stop()
//  }
//
//  def handleLoadingScreen(line: String): Unit = {
//    line match {
//      case LS_ONSCENELOADED_REGEX(prev, next) =>
//        debugLog.debug(s"HS LoadingScreen Log: changed from $prev to $next")
//        screen = next
//      case _ => // no match
//    }
//  }
//
//  def zoneEvent(line: String): Option[GameEvent] = {
//    line match {
//      case ZONE_PROCESSCHANGES_REGEX(zoneId, local, cardName, id, cardZone, zonePos, cardId, player, fromZone, toZone) =>
//
//        debugLog.debug("HS Zone Log: zoneId={} local={} cardName={} id={} cardZone={} zonePos={} cardId={} player={} fromZone={} toZone={}",
//          zoneId, local, cardName, id, cardZone, zonePos, cardId, player, fromZone, toZone)
//
//        val card = cards.find(_.name == cardName).headOption
//        // Only log zone changes if we're actually in a game.
//        // Some zone changes occur outside games which would result in misleading information if logged.
//        if ("GAMEPLAY".equals(screen)) {
//          (cardZone, fromZone, toZone) match {
//            case ("DECK", "FRIENDLY HAND", "FRIENDLY DECK") =>
//              // Put back into the deck... usually after replacing your starting hand
//              Log.info("    You returned " + cardName + " to your deck")
//              card map CardReplaced
//            case ("HAND", "", "FRIENDLY HAND") =>
//              // Received into your hand but not from your deck... usually The Coin
//              Log.info("    You received " + cardName)
//              card map CardDrawn
//            case ("HAND", "FRIENDLY DECK", "FRIENDLY HAND") =>
//              // Picked up into your hand
//              Log.info("    You picked up " + cardName)
//              card map CardDrawn
//            case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY") =>
//              // Your minion
//              Log.info("    You played minion " + cardName)
//              None
//            case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY (Weapon)") =>
//              Log.info("    You played weapon " + cardName)
//              None
//            case ("HAND", "FRIENDLY HAND", "FRIENDLY SECRET") =>
//              Log.info("    You played secret " + cardName)
//              None
//            case ("HAND", "FRIENDLY HAND", "") =>
//              Log.info("    You played spell " + cardName)
//              None
//            case ("HAND", "FRIENDLY PLAY", "FRIENDLY HAND") =>
//              // Returned a card to your hand
//              Log.info("    Your " + cardName + " was returned to your hand")
//              None
//            case ("PLAY", "", "FRIENDLY PLAY") =>
//              // You received a minion (without playing card directly)
//              Log.info("    You received minion " + cardName)
//              None
//            case ("PLAY", "", "FRIENDLY PLAY (Weapon)") =>
//              // You received a weapon (without playing card directly)
//              Log.info("    You received weapon " + cardName)
//              None
//            case ("PLAY", "", "FRIENDLY PLAY (Hero Power)") =>
//              // Your hero power
//              Log.info("    You played hero power " + cardName)
//              None
//            case ("PLAY", "", "OPPOSING PLAY") =>
//              // Opponent received a minion (without playing card directly)
//              Log.info("    Opponent received minion " + cardName)
//              None
//            case ("PLAY", "", "OPPOSING PLAY (Weapon)") =>
//              // Opponent received a weapon (without playing card directly)
//              Log.info("    Opponent received weapon " + cardName)
//              None
//            case ("PLAY", "", "OPPOSING PLAY (Hero Power)") =>
//              // Opponent hero power
//              Log.info("    Opponent played hero power " + cardName)
//              None
//            case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY") =>
//              // Opponent minion
//              Log.info("    Opponent played minion " + cardName)
//              None
//            case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY (Weapon)") =>
//              // Opponent weapon
//              Log.info("    Opponent played weapon " + cardName)
//              None
//            case ("PLAY", "OPPOSING HAND", "") =>
//              // Opponent spell
//              Log.info("    Opponent played spell " + cardName)
//              None
//            case ("GRAVEYARD", "", "FRIENDLY GRAVEYARD") =>
//              // A card went directly to the graveyard, probably a spell
//              debugLog.debug("    Ignoring spell {} going to graveyard", cardName)
//              None
//            case ("GRAVEYARD", "", "OPPOSING GRAVEYARD") =>
//              // A card went directly to the graveyard, probably a spell
//              debugLog.debug("    Ignoring spell {} going to graveyard", cardName)
//              None
//            case ("GRAVEYARD", "FRIENDLY HAND", "FRIENDLY GRAVEYARD") =>
//              // Your card was discarded from your deck (hand full)
//              Log.info("    Your " + cardName + " was discarded")
//              card map CardDrawn
//            case ("GRAVEYARD", "FRIENDLY PLAY", "FRIENDLY GRAVEYARD") =>
//              // Your minion died
//              Log.info("    Your " + cardName + " died")
//              None
//            case ("GRAVEYARD", "FRIENDLY PLAY (Weapon)", "FRIENDLY GRAVEYARD") =>
//              // Your weapon is finished
//              Log.info("    Your weapon " + cardName + " finished")
//              None
//            case ("GRAVEYARD", "FRIENDLY PLAY (Hero)", "FRIENDLY GRAVEYARD") =>
//              // Your hero has died... you are defeated(?)
//              debugLog.info("    Your hero " + cardName + " has been defeated")
//              Some(HeroDestroyedEvent(false))
//            case ("GRAVEYARD", "FRIENDLY SECRET", "FRIENDLY GRAVEYARD") =>
//              // Your secret was triggered... or possibly was destroyed?
//              Log.info("    Your secret " + cardName + " was revealed")
//              None
//            case ("GRAVEYARD", "OPPOSING HAND", "OPPOSING GRAVEYARD") =>
//              // Opponent card was discarded
//              Log.info("    Opponent's " + cardName + " was discarded")
//              None
//            case ("GRAVEYARD", "OPPOSING PLAY", "OPPOSING GRAVEYARD") =>
//              // Opponent minion died
//              Log.info("    Opponent's " + cardName + " died")
//              None
//            case ("GRAVEYARD", "OPPOSING PLAY (Weapon)", "OPPOSING GRAVEYARD") =>
//              // Opponent weapon is finished
//              Log.info("    Opponent's weapon " + cardName + " finished")
//              None
//            case ("GRAVEYARD", "OPPOSING PLAY (Hero)", "OPPOSING GRAVEYARD") =>
//              // Opponent's hero has died... you are victorious(?)
//              debugLog.info("    Opponent's hero " + cardName + " has been defeated")
//              Some(HeroDestroyedEvent(true))
//            case ("GRAVEYARD", "OPPOSING SECRET", "OPPOSING GRAVEYARD") =>
//              Log.info("    Opponent's secret " + cardName + " was revealed")
//              None
//            case _ =>
//              debugLog.debug("Unhandled log for {}: zone {} from {} to {}", cardName, cardZone, fromZone, toZone)
//              None
//          }
//        } else None
//      case _ =>
//        // ignore line
//        None
//    }
//  }
//
//  private def findCard(name: String) =
//    cards.filter(_.name == name).headOption match {
//      case None =>
//        Log.warn(s"unkown card : $name")
//        None
//      case some => some
//    }
//
//}