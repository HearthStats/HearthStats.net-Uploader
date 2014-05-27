package net.hearthstats.logmonitor

import org.slf4j.LoggerFactory
import net.hearthstats.Config
import org.apache.commons.io.input.Tailer
import java.io.File
import org.apache.commons.io.input.TailerListenerAdapter
import net.hearthstats.log.Log
import HearthstoneLogMonitor._
import java.util.Observable

class HearthstoneLogMonitor {

  var screen = "GAMEPLAY"; // Assume we're in a game until proved otherwise... just in case a game is already in progress
  var tailer: Tailer = null
  val logFile = Config.programHelper.hearthstoneLogFile
  val file = new File(logFile)

  def startMonitoring(): Unit = {
    if (tailer == null) {
      debugLog.debug(s"Starting Hearthstone log monitor on file $logFile")
      tailer = new Tailer(file, tailerAdapter, 500, true)
      val thread = new Thread(tailer)
      thread.setDaemon(true) // optional
      thread.start()
    }
  }

  def stopMonitoring(): Unit = {
    if (tailer != null) {
      debugLog.debug(s"Stopping Hearthstone log monitor on file $logFile")
      tailer.stop()
    }
  }

  lazy val tailerAdapter: TailerListenerAdapter = new TailerListenerAdapter {

    override def handle(line: String): Unit = {
      if (line != null && line.length() > 0 && line.charAt(0) == '[') {
        if (line.startsWith(LOADING_SCREEN_PREFIX)) {
          handleLoadingScreen(line)
        } else if (line.startsWith(ZONE_PREFIX)) {
          handleZone(line)
        }
      }
      super.handle(line)
    }

    override def handle(ex: Exception): Unit = {
      Log.error("Error reading Hearthstone log: " + ex.getMessage(), ex)
    }

    override def fileNotFound(): Unit = {
      Log.warn("Could not find Hearthstone log file " + logFile)
      Log.info("Monitoring of Hearthstone log is temporarily disabled")
      if (tailer != null) {
        tailer.stop()
      }
    }
  }

  def handleLoadingScreen(line: String): Unit = {
    line match {
      case LS_ONSCENELOADED_REGEX(prev, next) =>
        debugLog.debug(s"HS LoadingScreen Log: changed from $prev to $next")
        screen = next
      case _ => // no match
    }
  }

  def handleZone(line: String) {
    line match {
      case ZONE_PROCESSCHANGES_REGEX(zoneId, local, cardName, id, cardZone, zonePos, cardId, player, fromZone, toZone) =>

        debugLog.debug("HS Zone Log: zoneId={} local={} cardName={} id={} cardZone={} zonePos={} cardId={} player={} fromZone={} toZone={}",
          zoneId, local, cardName, id, cardZone, zonePos, cardId, player, fromZone, toZone)

        // Only log zone changes if we're actually in a game.
        // Some zone changes occur outside games which would result in misleading information if logged.
        if ("GAMEPLAY".equals(screen)) {
          (cardZone, fromZone, toZone) match {
            case ("DECK", "FRIENDLY HAND", "FRIENDLY DECK") =>
              // Put back into the deck... usually after replacing your starting hand
              Log.info("    You returned " + cardName + " to your deck")
              notifyCardPutBack(cardName)
            case ("HAND", "", "FRIENDLY HAND") =>
              // Received into your hand but not from your deck... usually The Coin
              Log.info("    You received " + cardName)
              notifyCardDrawn(cardName)
            case ("HAND", "FRIENDLY DECK", "FRIENDLY HAND") =>
              // Picked up into your hand
              Log.info("    You picked up " + cardName);
              notifyCardDrawn(cardName)
            case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY") =>
              // Your minion
              Log.info("    You played minion " + cardName)
            case ("HAND", "FRIENDLY HAND", "FRIENDLY PLAY (Weapon)") =>
              Log.info("    You played weapon " + cardName);
            case ("HAND", "FRIENDLY HAND", "FRIENDLY SECRET") =>
              Log.info("    You played secret " + cardName);
            case ("HAND", "FRIENDLY HAND", "") =>
              Log.info("    You played spell " + cardName);
            case ("HAND", "FRIENDLY PLAY", "FRIENDLY HAND") =>
              // Returned a card to your hand
              Log.info("    Your " + cardName + " was returned to your hand");
            case ("PLAY", "", "FRIENDLY PLAY") =>
              // You received a minion (without playing card directly)
              Log.info("    You received minion " + cardName);
            case ("PLAY", "", "FRIENDLY PLAY (Weapon)") =>
              // You received a weapon (without playing card directly)
              Log.info("    You received weapon " + cardName);
            case ("PLAY", "", "FRIENDLY PLAY (Hero Power)") =>
              // Your hero power
              Log.info("    You played hero power " + cardName);
            case ("PLAY", "", "OPPOSING PLAY") =>
              // Opponent received a minion (without playing card directly)
              Log.info("    Opponent received minion " + cardName);
            case ("PLAY", "", "OPPOSING PLAY (Weapon)") =>
              // Opponent received a weapon (without playing card directly)
              Log.info("    Opponent received weapon " + cardName);
            case ("PLAY", "", "OPPOSING PLAY (Hero Power)") =>
              // Opponent hero power
              Log.info("    Opponent played hero power " + cardName);
            case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY") =>
              // Opponent minion
              Log.info("    Opponent played minion " + cardName);
            case ("PLAY", "OPPOSING HAND", "OPPOSING PLAY (Weapon)") =>
              // Opponent weapon
              Log.info("    Opponent played weapon " + cardName);
            case ("PLAY", "OPPOSING HAND", "") =>
              // Opponent spell
              Log.info("    Opponent played spell " + cardName);
            case ("GRAVEYARD", "", "FRIENDLY GRAVEYARD") =>
              // A card went directly to the graveyard, probably a spell
              debugLog.debug("    Ignoring spell {} going to graveyard", cardName);
            case ("GRAVEYARD", "", "OPPOSING GRAVEYARD") =>
              // A card went directly to the graveyard, probably a spell
              debugLog.debug("    Ignoring spell {} going to graveyard", cardName);
            case ("GRAVEYARD", "FRIENDLY HAND", "FRIENDLY GRAVEYARD") =>
              // Your card was discarded from your deck (hand full)
              Log.info("    Your " + cardName + " was discarded");
              notifyCardDrawn(cardName)
            case ("GRAVEYARD", "FRIENDLY PLAY", "FRIENDLY GRAVEYARD") =>
              // Your minion died
              Log.info("    Your " + cardName + " died");
            case ("GRAVEYARD", "FRIENDLY PLAY (Weapon)", "FRIENDLY GRAVEYARD") =>
              // Your weapon is finished
              Log.info("    Your weapon " + cardName + " finished");
            case ("GRAVEYARD", "FRIENDLY PLAY (Hero)", "FRIENDLY GRAVEYARD") =>
              // Your hero has died... you are defeated(?)
              debugLog.info("    Your hero " + cardName + " has been defeated");
            case ("GRAVEYARD", "FRIENDLY SECRET", "FRIENDLY GRAVEYARD") =>
              // Your secret was triggered... or possibly was destroyed?
              Log.info("    Your secret " + cardName + " was revealed");
            case ("GRAVEYARD", "OPPOSING HAND", "OPPOSING GRAVEYARD") =>
              // Opponent card was discarded
              Log.info("    Opponent's " + cardName + " was discarded");
            case ("GRAVEYARD", "OPPOSING PLAY", "OPPOSING GRAVEYARD") =>
              // Opponent minion died
              Log.info("    Opponent's " + cardName + " died");
            case ("GRAVEYARD", "OPPOSING PLAY (Weapon)", "OPPOSING GRAVEYARD") =>
              // Opponent weapon is finished
              Log.info("    Opponent's weapon " + cardName + " finished");
            case ("GRAVEYARD", "OPPOSING PLAY (Hero)", "OPPOSING GRAVEYARD") =>
              // Opponent's hero has died... you are victorious(?)
              debugLog.info("    Opponent's hero " + cardName + " has been defeated");
            case ("GRAVEYARD", "OPPOSING SECRET", "OPPOSING GRAVEYARD") =>
              Log.info("    Opponent's secret " + cardName + " was revealed");
            case _ =>
              debugLog.debug("Unhandled log for {}: zone {} from {} to {}", cardName, cardZone, fromZone, toZone);
          }
        }
      case _ => // ignore line
    }
  }

  val observers = collection.mutable.ListBuffer.empty[CardDrawnObserver]

  def addObserver(obs: CardDrawnObserver): Unit =
    observers += obs

  private def notifyCardDrawn(c: String): Unit =
    for (obs <- observers) obs.cardDrawn(c)

  private def notifyCardPutBack(c: String): Unit =
    for (obs <- observers) obs.cardPutBack(c)

}

object HearthstoneLogMonitor {
  val debugLog = LoggerFactory.getLogger(classOf[HearthstoneLogMonitor])
  val LOADING_SCREEN_PREFIX = "[LoadingScreen]"
  val ZONE_PREFIX = "[Zone]"
  val LS_ONSCENELOADED_REGEX = """^\[LoadingScreen\] LoadingScreen\.OnSceneLoaded\(\) \- prevMode=(\S*) nextMode=(\S*)""".r
  val ZONE_PROCESSCHANGES_REGEX = """^\[Zone\] ZoneChangeList\.ProcessChanges\(\) - id=(\d*) local=(.*) \[name=(.*) id=(\d*) zone=(.*) zonePos=(\d*) cardId=(.*) player=(\d*)\] zone from (.*) -> (.*)""".r

}