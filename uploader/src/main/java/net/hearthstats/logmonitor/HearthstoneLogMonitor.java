package net.hearthstats.logmonitor;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.hearthstats.Config;
import net.hearthstats.log.Log;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gtch
 */
public class HearthstoneLogMonitor extends TailerListenerAdapter {

    private static final Logger debugLog = LoggerFactory.getLogger(HearthstoneLogMonitor.class);
    private static final String LOADING_SCREEN_PREFIX = "[LoadingScreen]";
    private static final String ZONE_PREFIX = "[Zone]";

    private static final String LS_ONSCENELOADED_REGEX = "^\\[LoadingScreen\\] LoadingScreen\\.OnSceneLoaded\\(\\) \\- prevMode=(\\S*) nextMode=(\\S*)";

    private static final String ZONE_PROCESSCHANGES_REGEX = "^\\[Zone\\] ZoneChangeList\\.ProcessChanges\\(\\) - id=(\\d*) local=(.*) \\[name=(.*) id=(\\d*) zone=(.*) zonePos=(\\d*) cardId=(.*) player=(\\d*)\\] zone from (.*) -> (.*)";

    private final Pattern lsOnSceneLoadedPattern = Pattern.compile(LS_ONSCENELOADED_REGEX);
    private final Pattern zoneProcessChangesPattern = Pattern.compile(ZONE_PROCESSCHANGES_REGEX);


    private final String logFile;

    private Tailer tailer;

    private String screen;


    public HearthstoneLogMonitor() {
		logFile = Config.programHelper().hearthstoneLogFile();
        screen = "GAMEPLAY";    // Assume we're in a game until proved otherwise... just in case a game is already in progress
    }


    public void startMonitoring() {
        debugLog.debug("Starting Hearthstone log monitor on file {}", logFile);

        if (tailer != null) {
            debugLog.warn("HearthstoneLogMonitor tailer already existed before start; it should not be running without the monitoring running");
            tailer.stop();
            tailer = null;
        }

        File file = new File(logFile);

        tailer = new Tailer(file, this, 500, true);
        Thread thread = new Thread(tailer);
        thread.setDaemon(true); // optional
        thread.start();

    }


    public void stopMonitoring() {
        debugLog.debug("Stopping Hearthstone log monitor on file {}", logFile);

        if (tailer == null) {
            debugLog.warn("HearthstoneLogMonitor tailer does not exist, cannot be stopped");
        }

        tailer.stop();
    }


    @Override
    public void handle(String line) {
        if (line != null && line.length() > 0 && line.charAt(0) == '[') {
            if (line.startsWith(LOADING_SCREEN_PREFIX)) {
                handleLoadingScreen(line);
            } else if (line.startsWith(ZONE_PREFIX)) {
                handleZone(line);
            }
        }
        super.handle(line);
    }


    void handleLoadingScreen(String line) {
        Matcher onSceneLoaded = lsOnSceneLoadedPattern.matcher(line);

        if (onSceneLoaded.matches()) {
            String prev = onSceneLoaded.group(1);
            String next = onSceneLoaded.group(2);
            debugLog.debug("HS LoadingScreen Log: changed from {} to {}", prev, next);
            screen = next;
        }
    }

    void handleZone(String line) {
        Matcher processChanges = zoneProcessChangesPattern.matcher(line);

        if (processChanges.matches()) {
            String zoneId = processChanges.group(1);
            String local = processChanges.group(2);
            String cardName = processChanges.group(3);
            String id = processChanges.group(4);
            String cardZone = processChanges.group(5);
            String zonePos = processChanges.group(6);
            String cardId = processChanges.group(7);
            String player = processChanges.group(8);
            String fromZone = processChanges.group(9);
            String toZone = processChanges.group(10);

            debugLog.debug("HS Zone Log: zoneId={} local={} cardName={} id={} cardZone={} zonePos={} cardId={} player={} fromZone={} toZone={}",
                    zoneId, local, cardName, id, cardZone, zonePos, cardId, player, fromZone, toZone);


            // Only log zone changes if we're actually in a game.
            // Some zone changes occur outside games which would result in misleading information if logged.
            if ("GAMEPLAY".equals(screen)) {

                boolean handledLog = false;

                switch (cardZone) {
                    case "DECK":
                        switch (fromZone) {
                            case "FRIENDLY HAND":
                                switch (toZone) {
                                    case "FRIENDLY DECK": {
                                        // Put back into the deck... usually after replacing your starting hand
                                        Log.info("    You returned " + cardName + " to your deck");
                                        handledLog = true;
                                        break;
                                    }
                                }
                                break;
                        }
                        break;
                    case "HAND":
                        switch (fromZone) {
                            case "":
                                switch (toZone) {
                                    case "FRIENDLY HAND": {
                                        // Received into your hand but not from your deck... usually The Coin
                                        Log.info("    You received " + cardName);
                                        handledLog = true;
                                        break;
                                    }
                                }
                                break;
                            case "FRIENDLY DECK":
                                switch (toZone) {
                                    case "FRIENDLY HAND":
                                        // Picked up into your hand
                                        Log.info("    You picked up " + cardName);
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "FRIENDLY HAND":
                                switch (toZone) {
                                    case "FRIENDLY PLAY":
                                        // Your minion
                                        Log.info("    You played minion " + cardName);
                                        handledLog = true;
                                        break;
                                    case "FRIENDLY PLAY (Weapon)":
                                        // Your weapon
                                        Log.info("    You played weapon " + cardName);
                                        handledLog = true;
                                        break;
                                    case "FRIENDLY SECRET":
                                        // Your secret
                                        Log.info("    You played secret " + cardName);
                                        handledLog = true;
                                        break;
                                    case "":
                                        // Your spell
                                        Log.info("    You played spell " + cardName);
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "FRIENDLY PLAY":
                                switch (toZone) {
                                    case "FRIENDLY HAND":
                                        // Returned a card to your hand
                                        Log.info("    Your " + cardName + " was returned to your hand");
                                        handledLog = true;
                                        break;
                                }
                                break;
                        }
                        break;
                    case "PLAY":
                        switch (fromZone) {
                            case "":
                                switch (toZone) {
                                    case "FRIENDLY PLAY":
                                        // You received a minion (without playing card directly)
                                        Log.info("    You received minion " + cardName);
                                        handledLog = true;
                                        break;
                                    case "FRIENDLY PLAY (Weapon)":
                                        // You received a weapon (without playing card directly)
                                        Log.info("    You received weapon " + cardName);
                                        handledLog = true;
                                        break;
                                    case "FRIENDLY PLAY (Hero Power)":
                                        // Your hero power
                                        Log.info("    You played hero power " + cardName);
                                        handledLog = true;
                                        break;
                                    case "OPPOSING PLAY":
                                        // Opponent received a minion (without playing card directly)
                                        Log.info("    Opponent received minion " + cardName);
                                        handledLog = true;
                                        break;
                                    case "OPPOSING PLAY (Weapon)":
                                        // Opponent received a weapon (without playing card directly)
                                        Log.info("    Opponent received weapon " + cardName);
                                        handledLog = true;
                                        break;
                                    case "OPPOSING PLAY (Hero Power)":
                                        // Opponent hero power
                                        Log.info("    Opponent played hero power " + cardName);
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "OPPOSING HAND":
                                switch (toZone) {
                                    case "OPPOSING PLAY":
                                        // Opponent minion
                                        Log.info("    Opponent played minion " + cardName);
                                        handledLog = true;
                                        break;
                                    case "OPPOSING PLAY (Weapon)":
                                        // Opponent weapon
                                        Log.info("    Opponent played weapon " + cardName);
                                        handledLog = true;
                                        break;
                                    case "":
                                        // Opponent spell
                                        Log.info("    Opponent played spell " + cardName);
                                        handledLog = true;
                                        break;
                                }
                                break;
                        }
                        break;
                    case "GRAVEYARD":
                        switch (fromZone) {
                            case "":
                                switch (toZone) {
                                    case "FRIENDLY GRAVEYARD":
                                        // A card went directly to the graveyard, probably a spell
                                        debugLog.debug("    Ignoring spell {} going to graveyard", cardName);
                                        handledLog = true;
                                        break;
                                    case "OPPOSING GRAVEYARD":
                                        // A card went directly to the graveyard, probably a spell
                                        debugLog.debug("    Ignoring spell {} going to graveyard", cardName);
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "FRIENDLY HAND":
                                switch (toZone) {
                                    case "FRIENDLY GRAVEYARD":
                                        // Your card was discarded
                                        Log.info("    Your " + cardName + " was discarded");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "FRIENDLY PLAY":
                                switch (toZone) {
                                    case "FRIENDLY GRAVEYARD":
                                        // Your minion died
                                        Log.info("    Your " + cardName + " died");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "FRIENDLY PLAY (Weapon)":
                                switch (toZone) {
                                    case "FRIENDLY GRAVEYARD":
                                        // Your weapon is finished
                                        Log.info("    Your weapon " + cardName + " finished");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "FRIENDLY PLAY (Hero)":
                                switch (toZone) {
                                    case "FRIENDLY GRAVEYARD":
                                        // Your hero has died... you are defeated(?)
                                        debugLog.info("    Your hero " + cardName + " has been defeated");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "FRIENDLY SECRET":
                                switch (toZone) {
                                    case "FRIENDLY GRAVEYARD":
                                        // Your secret was triggered... or possibly was destroyed?
                                        Log.info("    Your secret " + cardName + " was revealed");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "OPPOSING HAND":
                                switch (toZone) {
                                    case "OPPOSING GRAVEYARD":
                                        // Opponent card was discarded
                                        Log.info("    Opponent's " + cardName + " was discarded");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "OPPOSING PLAY":
                                switch (toZone) {
                                    case "OPPOSING GRAVEYARD":
                                        // Opponent minion died
                                        Log.info("    Opponent's " + cardName + " died");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "OPPOSING PLAY (Weapon)":
                                switch (toZone) {
                                    case "OPPOSING GRAVEYARD":
                                        // Opponent weapon is finished
                                        Log.info("    Opponent's weapon " + cardName + " finished");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "OPPOSING PLAY (Hero)":
                                switch (toZone) {
                                    case "OPPOSING GRAVEYARD":
                                        // Opponent's hero has died... you are victorious(?)
                                        debugLog.info("    Opponent's hero " + cardName + " has been defeated");
                                        handledLog = true;
                                        break;
                                }
                                break;
                            case "OPPOSING SECRET":
                                switch (toZone) {
                                    case "OPPOSING GRAVEYARD":
                                        // Opponent's secret was triggered... or possibly was destroyed?
                                        Log.info("    Opponent's secret " + cardName + " was revealed");
                                        handledLog = true;
                                        break;
                                }
                                break;
                        }
                        break;
                }

                if (!handledLog) {
                    debugLog.debug("Unhandled log for {}: zone {} from {} to {}", cardName, cardZone, fromZone, toZone);
                }
            }
        }
    }


    @Override
    public void handle(Exception ex) {
        Log.error("Error reading Hearthstone log: " + ex.getMessage(), ex);
    }


    @Override
    public void fileNotFound() {
        Log.warn("Could not find Hearthstone log file " + logFile);
        Log.info("Monitoring of Hearthstone log is temporarily disabled");
        if (tailer != null) {
            tailer.stop();
        }
    }
}
