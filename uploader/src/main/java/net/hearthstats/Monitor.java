package net.hearthstats;

import static net.hearthstats.Constants.PROFILES_URL;
import static net.hearthstats.util.Translations.t;

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.html.HTMLDocument;

import net.hearthstats.analysis.AnalyserEvent;
import net.hearthstats.log.Log;
import net.hearthstats.log.LogPane;
import net.hearthstats.logmonitor.HearthstoneLogMonitor;
import net.hearthstats.notification.DialogNotificationQueue;
import net.hearthstats.notification.NotificationQueue;
import net.hearthstats.state.Screen;
import net.hearthstats.state.ScreenGroup;
import net.hearthstats.ui.ClickableDeckBox;
import net.hearthstats.ui.DecksTab;
import net.hearthstats.ui.MatchEndPopup;
import net.hearthstats.ui.MatchPanel;
import net.hearthstats.ui.OptionsPanel;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.hearthstats.analysis.HearthstoneAnalyser;

import com.dmurph.tracking.JGoogleAnalyticsTracker;

@SuppressWarnings("serial")
public class Monitor extends JFrame implements Observer {

  private static final int POLLING_INTERVAL_IN_MS = 100;

  private static final EnumSet<Screen> DO_NOT_NOTIFY_SCREENS = EnumSet.of(Screen.COLLECTION,
      Screen.COLLECTION_ZOOM, Screen.MAIN_TODAYSQUESTS, Screen.TITLE);

  private static Logger debugLog = LoggerFactory.getLogger(Monitor.class);

  protected ProgramHelper _hsHelper = Config.programHelper();
  protected HearthstoneLogMonitor hearthstoneLogMonitor;

  private HyperlinkListener _hyperLinkListener = HyperLinkHandler.getInstance();

  private boolean _hearthstoneDetected;
  private JGoogleAnalyticsTracker _analytics;
  private LogPane _logText;
  private JScrollPane _logScroll;
  private JTabbedPane _tabbedPane;

  private OptionsPanel optionsPanel;

  private MatchPanel matchPanel;

  public Monitor() {
    _notificationQueue = DialogNotificationQueue.newNotificationQueue();
  }

  public void start() throws IOException {
    if (Config.analyticsEnabled()) {
      debugLog.debug("Enabling analytics");
      _analytics = AnalyticsTracker.tracker();
      _analytics.trackEvent("app", "AppStart");
    }
    addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent e) {
        handleClose();
      }
    });

    createAndShowGui();
    showWelcomeLog();
    checkForUpdates();

    API.addObserver(this);
    HearthstoneAnalyser.addObserver(this);
    _hsHelper.addObserver(this);

    if (_checkForUserKey()) {
      poller.start();
    } else {
      System.exit(1);
    }

    if (Config.os == Config.OS.OSX) {
      Log.info(t("waiting_for_hs"));
    } else {
      Log.info(t("waiting_for_hs_windowed"));
    }
  }

  private boolean _checkForUserKey() {
    boolean userKeySet = !Config.getUserKey().equals("your_userkey_here");
    if (userKeySet) {
      return true;
    } else {
      Log.warn(t("error.userkey_not_entered"));

      bringWindowToFront();

      JOptionPane.showMessageDialog(this, "HearthStats.net " + t("error.title") + ":\n\n"
          + t("you_need_to_enter_userkey") + "\n\n" + t("get_it_at_hsnet_profiles"));

      Desktop d = Desktop.getDesktop();
      try {
        d.browse(new URI(PROFILES_URL));
      } catch (IOException | URISyntaxException e) {
        Log.warn("Error launching browser with URL " + PROFILES_URL, e);
      }

      String userkey = JOptionPane.showInputDialog(this, t("enter_your_userkey"));
      if (StringUtils.isEmpty(userkey)) {
        return false;
      } else {
        Config.setUserKey(userkey);
        try {
          optionsPanel.setUserKey(userkey);
          Config.save();
          Log.info(t("UserkeyStored"));
        } catch (Throwable e) {
          Log.warn("Error occurred trying to write settings file, your settings may not be saved",
              e);
        }
        return true;
      }
    }
  }

  public void handleClose() {
    Point p = getLocationOnScreen();
    Config.setX(p.x);
    Config.setY(p.y);
    Dimension rect = getSize();
    Config.setWidth((int) rect.getWidth());
    Config.setHeight((int) rect.getHeight());
    try {
      Config.save();
    } catch (Throwable t) {
      Log.warn("Error occurred trying to write settings file, your settings may not be saved", t);
    }
    System.exit(0);
  }

  private void showWelcomeLog() {
    debugLog.debug("Showing welcome log messages");

    Log.welcome("HearthStats.net " + t("Uploader") + " v" + Config.getVersionWithOs());

    Log.help(t("welcome_1_set_decks"));
    if (Config.os == Config.OS.OSX) {
      Log.help(t("welcome_2_run_hearthstone"));
      Log.help(t("welcome_3_notifications"));
    } else {
      Log.help(t("welcome_2_run_hearthstone_windowed"));
      Log.help(t("welcome_3_notifications_windowed"));
    }
    String logFileLocation = Log.getLogFileLocation();
    if (logFileLocation == null) {
      Log.help(t("welcome_4_feedback"));
    } else {
      Log.help(t("welcome_4_feedback_with_log", logFileLocation));
    }

  }

  /**
   * Brings the monitor window to the front of other windows. Should only be
   * used for important events like a modal dialog or error that we want the
   * user to see immediately.
   */
  public void bringWindowToFront() {
    final Monitor frame = this;
    java.awt.EventQueue.invokeLater(new Runnable() {
      @Override
      public void run() {
        frame.setVisible(true);
      }
    });
  }

  /**
   * Overridden version of setVisible based on
   * http://stackoverflow.com/questions
   * /309023/how-to-bring-a-window-to-the-front that should ensure the window is
   * brought to the front for important things like modal dialogs.
   */
  @Override
  public void setVisible(final boolean visible) {
    // let's handle visibility...
    if (!visible || !isVisible()) { // have to check this condition simply
                                    // because super.setVisible(true) invokes
                                    // toFront if frame was already visible
      super.setVisible(visible);
    }
    // ...and bring frame to the front.. in a strange and weird way
    if (visible) {
      int state = super.getExtendedState();
      state &= ~JFrame.ICONIFIED;
      super.setExtendedState(state);
      super.setAlwaysOnTop(true);
      super.toFront();
      super.requestFocus();
      super.setAlwaysOnTop(false);
    }
  }

  @Override
  public void toFront() {
    super.setVisible(true);
    int state = super.getExtendedState();
    state &= ~JFrame.ICONIFIED;
    super.setExtendedState(state);
    super.setAlwaysOnTop(true);
    super.toFront();
    super.requestFocus();
    super.setAlwaysOnTop(false);
  }

  private void createAndShowGui() {
    debugLog.debug("Creating GUI");

    Image icon = new ImageIcon(getClass().getResource("/images/icon.png")).getImage();
    setIconImage(icon);
    setLocation(Config.getX(), Config.getY());
    setSize(Config.getWidth(), Config.getHeight());

    _tabbedPane = new JTabbedPane();
    add(_tabbedPane);

    // log
    _logText = new LogPane();
    _logScroll = new JScrollPane(_logText, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    _tabbedPane.add(_logScroll, t("tab.log"));

    _tabbedPane.add(matchPanel = new MatchPanel(), t("tab.current_match"));
    _tabbedPane.add(new DecksTab(), t("tab.decks"));
    _tabbedPane.add(optionsPanel = new OptionsPanel(this), t("tab.options"));
    _tabbedPane.add(_createAboutUi(), t("tab.about"));

    matchPanel.updateCurrentMatchUi();

    _enableMinimizeToTray();

    setMinimumSize(new Dimension(500, 600));
    setVisible(true);

    if (Config.startMinimized())
      setState(JFrame.ICONIFIED);

    _updateTitle();
  }

  private JScrollPane _createAboutUi() {
    Map<String, String> localeStrings = new HashMap<String, String>();
    localeStrings.put("Author", t("Author"));
    localeStrings.put("version", t("Uploader") + " v" + Config.getVersion());
    localeStrings.put("utility_l1", t("about.utility_l1"));
    localeStrings.put("utility_l2", t("about.utility_l2"));
    localeStrings.put("utility_l3", t("about.utility_l3"));
    localeStrings.put("open_source_l1", t("about.open_source_l1"));
    localeStrings.put("open_source_l2", t("about.open_source_l2"));
    localeStrings.put("project_source", t("about.project_source"));
    localeStrings.put("releases_and_changelog", t("about.releases_and_changelog"));
    localeStrings.put("feedback_and_suggestions", t("about.feedback_and_suggestions"));
    localeStrings.put("support_project", t("about.support_project"));
    localeStrings.put("donate_image", getClass().getResource("/images/donate.gif").toString());

    JEditorPane contributorsText = new JEditorPane();
    contributorsText.setContentType("text/html");
    contributorsText.setEditable(false);
    contributorsText.setBackground(Color.WHITE);

    try (Reader cssReader = new InputStreamReader(
        LogPane.class.getResourceAsStream("/net/hearthstats/about.css"))) {
      ((HTMLDocument) contributorsText.getDocument()).getStyleSheet().loadRules(cssReader, null);
    } catch (IOException e) {
      // If we can't load the About css, log a warning but continue
      Log.warn("Unable to format About tab", e);
    }

    try (Reader aboutReader = new InputStreamReader(
        LogPane.class.getResourceAsStream("/net/hearthstats/about.html"))) {
      String aboutText = StrSubstitutor.replace(IOUtils.toString(aboutReader), localeStrings);
      contributorsText.setText(aboutText);
    } catch (IOException e) {
      // If we can't load the About text, log a warning but continue
      Log.warn("Unable to display About tab", e);
    }
    contributorsText.addHyperlinkListener(_hyperLinkListener);

    return new JScrollPane(contributorsText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
  }

  

  private void checkForUpdates() {
    if (Config.checkForUpdates()) {
      Log.info(t("checking_for_updates..."));
      try {
        String availableVersion = Updater.getAvailableVersion();
        if (availableVersion != null) {
          Log.info(t("latest_v_available") + " " + availableVersion);

          if (!availableVersion.matches(Config.getVersion())) {

            bringWindowToFront();

            int dialogButton = JOptionPane.YES_NO_OPTION;
            int dialogResult = JOptionPane.showConfirmDialog(this,
                "A new version of this uploader is available\n\n" + Updater.getRecentChanges()
                    + "\n\n" + t("would_u_like_to_install_update"), "HearthStats.net "
                    + t("uploader_updates_avail"), dialogButton);
            if (dialogResult == JOptionPane.YES_OPTION) {
              /*
               * // Create Desktop object Desktop d = Desktop.getDesktop(); //
               * Browse a URL, say google.com d.browse(new URI(
               * "https://github.com/JeromeDane/HearthStats.net-Uploader/releases"
               * )); System.exit(0);
               */
              Updater.run();
            } else {
              dialogResult = JOptionPane.showConfirmDialog(null,
                  t("would_you_like_to_disable_updates"), t("disable_update_checking"),
                  dialogButton);
              if (dialogResult == JOptionPane.YES_OPTION) {
                String[] options = { t("button.ok") };
                JPanel panel = new JPanel();
                JLabel lbl = new JLabel(t("reenable_updates_any_time"));
                panel.add(lbl);
                JOptionPane.showOptionDialog(this, panel, t("updates_disabled_msg"),
                    JOptionPane.NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                Config.setCheckForUpdates(false);
              }
            }
          }
        } else {
          Log.warn("Unable to determine latest available version");
        }
      } catch (Throwable e) {
        e.printStackTrace(System.err);
        _notify("Update Checking Error", "Unable to determine the latest available version");
      }
    }
  }

  /**
   * Sets up the Hearthstone log monitoring if enabled, or stops if it is
   * disabled
   */
  public void setupLogMonitoring() {
    setMonitorHearthstoneLog(Config.monitoringMethod() == Config.MonitoringMethod.SCREEN_LOG);
  }

  protected boolean _drawPaneAdded = false;

  protected BufferedImage image;

  protected JPanel _drawPane = new JPanel() {
    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      g.drawImage(image, 0, 0, null);
    }
  };

  protected NotificationQueue _notificationQueue;

  public void setNotificationQueue(NotificationQueue _notificationQueue) {
    this._notificationQueue = _notificationQueue;
  }

  private boolean _playingInMatch = false;

  protected void _notify(String header) {
    _notify(header, "");
  }

  protected void _notify(String header, String message) {
    if (Config.showNotifications())
      _notificationQueue.add(header, message, false);
  }

  protected void _updateTitle() {
    String title = "HearthStats.net Uploader";
    if (_hearthstoneDetected) {
      if (HearthstoneAnalyser.getScreen() != null) {
        title += " - " + HearthstoneAnalyser.getScreen().title;
        // if (HearthstoneAnalyser.getScreen() == "Play" &&
        // HearthstoneAnalyser.getMode() != null) {
        if (HearthstoneAnalyser.getScreen() == Screen.PLAY_LOBBY
            && HearthstoneAnalyser.getMode() != null) {
          title += " " + HearthstoneAnalyser.getMode();
        }
        if (HearthstoneAnalyser.getScreen() == Screen.FINDING_OPPONENT) {
          if (HearthstoneAnalyser.getMode() != null) {
            title += " for " + HearthstoneAnalyser.getMode() + " Game";
          }
        }

        // TODO: replace with enum values
        if ("Match Start".equals(HearthstoneAnalyser.getScreen().title)
            || "Playing".equals(HearthstoneAnalyser.getScreen().title)) {
          title += " "
              + (HearthstoneAnalyser.getMode() == null ? "[undetected]" : HearthstoneAnalyser
                  .getMode());
          title += " " + (HearthstoneAnalyser.getCoin() ? "" : "No ") + "Coin";
          title += " "
              + (HearthstoneAnalyser.getYourClass() == null ? "[undetected]" : HearthstoneAnalyser
                  .getYourClass());
          title += " VS. "
              + (HearthstoneAnalyser.getOpponentClass() == null ? "[undetected]"
                  : HearthstoneAnalyser.getOpponentClass());
        }
      }
    } else {
      title += " - Waiting for Hearthstone ";
    }
    setTitle(title);
  }



  private void _updateImageFrame() {
    if (!_drawPaneAdded) {
      add(_drawPane);
    }
    if (image.getWidth() >= 1024) {
      setSize(image.getWidth(), image.getHeight());
    }
    _drawPane.repaint();
    invalidate();
    validate();
    repaint();
  }

  private void _submitMatchResult(HearthstoneMatch hsMatch) throws IOException {
    // check for new arena run
    if ("Arena".equals(hsMatch.mode()) && HearthstoneAnalyser.isNewArena()) {
      ArenaRun run = new ArenaRun();
      run.setUserClass(hsMatch.userClass());
      Log.info("Creating new " + run.getUserClass() + "arena run");
      _notify("Creating new " + run.getUserClass() + "arena run");
      API.createArenaRun(run);
      HearthstoneAnalyser.setIsNewArena(false);
    }

    String header = "Submitting match result";
    String message = hsMatch.toString();
    _notify(header, message);
    Log.matchResult(header + ": " + message);

    if (Config.analyticsEnabled()) {
      _analytics.trackEvent("app", "Submit" + hsMatch.mode() + "Match");
    }

    API.createMatch(hsMatch);
  }


  protected void _handleHearthstoneFound() {
    // mark hearthstone found if necessary
    if (!_hearthstoneDetected) {
      _hearthstoneDetected = true;
      debugLog.debug("  - hearthstoneDetected");
      if (Config.showHsFoundNotification()) {
        _notify("Hearthstone found");
      }
      if (hearthstoneLogMonitor == null) {
        hearthstoneLogMonitor = new HearthstoneLogMonitor();
      }
      setupLogMonitoring();
    }

    // grab the image from Hearthstone
    debugLog.debug("  - screen capture");
    image = _hsHelper.getScreenCapture();

    if (image == null) {
      debugLog.debug("  - screen capture returned null");
    } else {
      // detect image stats
      if (image.getWidth() >= 1024) {
        debugLog.debug("  - analysing image");
        HearthstoneAnalyser.analyze(image);
      }

      if (Config.mirrorGameImage()) {
        debugLog.debug("  - mirroring image");
        _updateImageFrame();
      }
    }
  }

  protected void _handleHearthstoneNotFound() {

    // mark hearthstone not found if necessary
    if (_hearthstoneDetected) {
      _hearthstoneDetected = false;
      debugLog.debug("  - changed hearthstoneDetected to false");
      if (Config.showHsClosedNotification()) {
        _notify("Hearthstone closed");
        HearthstoneAnalyser.reset();
      }
    }
  }

  private void pollHsImpl() {
    boolean error = false;
    while (!error) {
      try {
        if (_hsHelper.foundProgram()) {
          _handleHearthstoneFound();
        } else {
          debugLog.debug("  - did not find Hearthstone");
          _handleHearthstoneNotFound();
        }
        _updateTitle();
        Thread.sleep(POLLING_INTERVAL_IN_MS);
      } catch (Throwable ex) {
        ex.printStackTrace(System.err);
        debugLog.error("  - exception which is not being handled:", ex);
        while (ex.getCause() != null) {
          ex = ex.getCause();
        }
        Log.error("ERROR: " + ex.getMessage()
            + ". You will need to restart HearthStats.net Uploader.", ex);
        error = true;
      } finally {
        debugLog.debug("<-- finished");
      }

    }
  }

  /**
   * Checks whether the match result is complete, showing a popup if necessary
   * to fix the match data, and then submits the match when ready.
   * 
   * @param match
   *          The match to check and submit.
   */
  private void checkMatchResult(final HearthstoneMatch match) {
    matchPanel.updateMatchClassSelectorsIfSet(match);

    final Config.MatchPopup matchPopup = Config.showMatchPopup();
    final boolean showPopup;

    switch (matchPopup) {
    case ALWAYS:
      showPopup = true;
      break;
    case INCOMPLETE:
      showPopup = !match.isDataComplete();
      break;
    case NEVER:
      showPopup = false;
      break;
    default:
      throw new UnsupportedOperationException("Unknown config option " + Config.showMatchPopup());
    }

    if (showPopup) {
      // Show a popup allowing the user to edit their match before submitting
      final Monitor monitor = this;
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
            boolean matchHasValidationErrors = !match.isDataComplete();
            String infoMessage = null;
            do {
              if (infoMessage == null) {
                infoMessage = (matchPopup == Config.MatchPopup.INCOMPLETE) ? t("match.popup.message.incomplete")
                    : t("match.popup.message.always");
              }
              bringWindowToFront();
              MatchEndPopup.Button buttonPressed = MatchEndPopup.showPopup(monitor, match,
                  infoMessage, t("match.popup.title"));
              matchHasValidationErrors = !match.isDataComplete();
              switch (buttonPressed) {
              case SUBMIT:
                if (matchHasValidationErrors) {
                  infoMessage = "Some match information is incomplete.<br>Please update these details then click Submit to submit the match to HearthStats:";
                } else {
                  _submitMatchResult(match);
                }
                break;
              case CANCEL:
                return;
              }

            } while (matchHasValidationErrors);
          } catch (IOException e) {
            Main.showErrorDialog("Error submitting match result", e);
          }
        }
      });

    } else {
      // Don't show a popup, submit the match directly
      try {
        _submitMatchResult(match);
      } catch (IOException e) {
        Main.showErrorDialog("Error submitting match result", e);
      }
    }
  }

  private void handleAnalyserEvent(AnalyserEvent changed) throws IOException {
    switch (changed) {
    case ARENA_END:
      _notify("End of Arena Run Detected");
      Log.info("End of Arena Run Detected");
      API.endCurrentArenaRun();
      break;

    case COIN:
      _notify("Coin Detected");
      Log.info("Coin Detected");
      break;

    case DECK_SLOT:
      Deck deck = DeckUtils.getDeckFromSlot(HearthstoneAnalyser.getDeckSlot());
      if (deck == null) {
        _tabbedPane.setSelectedIndex(2);
        bringWindowToFront();
        Main.showMessageDialog(this,
 "Unable to determine what deck you have in slot #"
            + HearthstoneAnalyser.getDeckSlot()
                + "\n\nPlease set your decks in the \"Decks\" tab.");
      } else {
        _notify("Deck Detected", deck.name());
        Log.info("Deck Detected: " + deck.name() + " Detected");
      }

      break;

    case MODE:
      _playingInMatch = false;
      matchPanel.setCurrentMatchEnabledi(false);
      if (Config.showModeNotification()) {
        debugLog.debug(HearthstoneAnalyser.getMode() + " level "
            + HearthstoneAnalyser.getRankLevel());
        if ("Ranked".equals(HearthstoneAnalyser.getMode())) {
          _notify(HearthstoneAnalyser.getMode() + " Mode Detected", "Rank Level "
              + HearthstoneAnalyser.getRankLevel());
        } else {
          _notify(HearthstoneAnalyser.getMode() + " Mode Detected");
        }
      }
      if ("Ranked".equals(HearthstoneAnalyser.getMode())) {
        Log.info(HearthstoneAnalyser.getMode() + " Mode Detected - Level "
            + HearthstoneAnalyser.getRankLevel());
      } else {
        Log.info(HearthstoneAnalyser.getMode() + " Mode Detected");
      }
      break;

    case NEW_ARENA:
      if (HearthstoneAnalyser.isNewArena())
        _notify("New Arena Run Detected");
      Log.info("New Arena Run Detected");
      break;

    case OPPONENT_CLASS:
      _notify("Playing vs " + HearthstoneAnalyser.getOpponentClass());
      Log.info("Playing vs " + HearthstoneAnalyser.getOpponentClass());
      break;

    case OPPONENT_NAME:
      _notify("Opponent: " + HearthstoneAnalyser.getOpponentName());
      Log.info("Opponent: " + HearthstoneAnalyser.getOpponentName());
      break;

    case RESULT:
      _playingInMatch = false;
      matchPanel.setCurrentMatchEnabledi(false);
      _notify(HearthstoneAnalyser.getResult() + " Detected");
      Log.info(HearthstoneAnalyser.getResult() + " Detected");
      checkMatchResult(HearthstoneAnalyser.getMatch());
      break;

    case SCREEN:

      boolean inGameModeScreen = (HearthstoneAnalyser.getScreen() == Screen.ARENA_LOBBY
          || HearthstoneAnalyser.getScreen() == Screen.ARENA_END || HearthstoneAnalyser.getScreen() == Screen.PLAY_LOBBY);
      if (inGameModeScreen) {
        if (_playingInMatch && HearthstoneAnalyser.getResult() == null) {
          _playingInMatch = false;
          _notify("Detection Error", "Match result was not detected.");
          Log.info("Detection Error: Match result was not detected.");
          checkMatchResult(HearthstoneAnalyser.getMatch());
        }
        _playingInMatch = false;
      }

      if (HearthstoneAnalyser.getScreen() == Screen.FINDING_OPPONENT) {
        // Ensure that log monitoring is running before starting the match
        // because Hearthstone may only have created the log file
        // after the HearthStats Uploader started up. In that case log
        // monitoring won't yet be running.
        setupLogMonitoring();
        matchPanel.resetMatchClassSelectors();
        // TODO : also display the overlay for Practice mode (usefull for tests)
        if (Config.showDeckOverlay() && !"Arena".equals(HearthstoneAnalyser.getMode())) {
          Deck selectedDeck = DeckUtils.getDeckFromSlot(HearthstoneAnalyser.getDeckSlot());
          if (selectedDeck != null && selectedDeck.isValid() && hearthstoneLogMonitor != null) {
            ClickableDeckBox.showBox(selectedDeck, hearthstoneLogMonitor.cardEvents());
          } else {
            String message;
            if (selectedDeck == null) {
              message = "Invalid or empty deck, edit it on HearthStats.net to display deck overlay (you will need to restart the uploader)";
            } else {
              message = String
                  .format(
                      "Invalid or empty deck, <a href='http://hearthstats.net/decks/%s/edit'>edit it on HearthStats.net</a> to display deck overlay (you will need to restart the uploader)",
                      selectedDeck.slug());
            }
            _notify(message);
            Log.info(message);
          }
        }
      }

      if (HearthstoneAnalyser.getScreen().group == ScreenGroup.MATCH_START) {
        matchPanel.setCurrentMatchEnabledi(true);
        _playingInMatch = true;
      }

      if (HearthstoneAnalyser.getScreen().group != ScreenGroup.MATCH_END
          && !DO_NOT_NOTIFY_SCREENS.contains(HearthstoneAnalyser.getScreen())
          && Config.showScreenNotification()) {
        if (HearthstoneAnalyser.getScreen() == Screen.PRACTICE_LOBBY) {
          _notify(HearthstoneAnalyser.getScreen().title + " Screen Detected",
              "Results are not tracked in practice mode");
        } else {
          _notify(HearthstoneAnalyser.getScreen().title + " Screen Detected");
        }
      }

      if (HearthstoneAnalyser.getScreen() == Screen.PRACTICE_LOBBY) {
        Log.info(HearthstoneAnalyser.getScreen().title
            + " Screen Detected. Result tracking disabled.");
      } else {
        if (HearthstoneAnalyser.getScreen() == Screen.MATCH_VS) {
          Log.divider();
        }
        Log.info(HearthstoneAnalyser.getScreen().title + " Screen Detected");
      }
      break;

    case YOUR_CLASS:
      _notify("Playing as " + HearthstoneAnalyser.getYourClass());
      Log.info("Playing as " + HearthstoneAnalyser.getYourClass());
      break;

    case YOUR_TURN:
      if (Config.showYourTurnNotification()) {
        _notify((HearthstoneAnalyser.isYourTurn() ? "Your" : "Opponent") + " turn detected");
      }
      Log.info((HearthstoneAnalyser.isYourTurn() ? "Your" : "Opponent") + " turn detected");
      break;

    case ERROR_ANALYSING_IMAGE:
      _notify("Error analysing opponent name image");
      Log.info("Error analysing opponent name image");
      break;

    default:
      _notify("Unhandled event");
      Log.info("Unhandled event");
    }
    matchPanel.updateCurrentMatchUi();
  }

  public LogPane getLogPane() {
    return _logText;
  }

  private void _handleApiEvent(Object changed) {
    switch (changed.toString()) {
    case "error":
      _notify("API Error", API.message());
      Log.error("API Error: " + API.message());
      Main.showMessageDialog(this, "API Error: " + API.message());
      break;
    case "result":
      Log.info("API Result: " + API.message());
      HearthstoneMatch lastMatch = HearthstoneAnalyser.getMatch();
      lastMatch.id_$eq(API.lastMatchId());
      matchPanel.setCurrentMatchEnabledi(false);
      matchPanel.updateCurrentMatchUi();
      matchPanel.setLastMatch(lastMatch);
      // new line after match result
      if (API.message().matches(".*(Edit match|Arena match successfully created).*")) {
        HearthstoneAnalyser.resetMatch();
        matchPanel.resetMatchClassSelectors();
        Log.divider();
      }
      break;
    }
  }

  private void _handleProgramHelperEvent(Object changed) {
    Log.info(changed.toString());
    if (changed.toString().matches(".*minimized.*")) {
      _notify("Hearthstone Minimized", "Warning! No detection possible while minimized.");
    }
    if (changed.toString().matches(".*fullscreen.*")) {
      JOptionPane
          .showMessageDialog(
              this,
              "Hearthstats.net Uploader Warning! \n\nNo detection possible while Hearthstone is in fullscreen mode.\n\nPlease set Hearthstone to WINDOWED mode and close and RESTART Hearthstone.\n\nSorry for the inconvenience.");
    }
    if (changed.toString().matches(".*restored.*")) {
      _notify("Hearthstone Restored", "Resuming detection ...");
    }
  }

  @Override
  public void update(Observable dispatcher, Object changed) {
    String dispatcherClass = dispatcher == null ? "" : dispatcher.getClass().getCanonicalName();
    if (dispatcherClass.startsWith("net.hearthstats.analysis.HearthstoneAnalyser"))
      try {
        handleAnalyserEvent((AnalyserEvent) changed);
      } catch (IOException e) {
        Main.showErrorDialog("Error handling analyzer event", e);
      }
    if (dispatcherClass.startsWith("net.hearthstats.API"))
      _handleApiEvent(changed);

    if (dispatcherClass.matches(".*ProgramHelper(Windows|Osx)?"))
      _handleProgramHelperEvent(changed);
  }


  // http://stackoverflow.com/questions/7461477/how-to-hide-a-jframe-in-system-tray-of-taskbar
  TrayIcon trayIcon;
  SystemTray tray;
  private Thread poller = new Thread(new Runnable() {
    @Override
    public void run() {
      pollHsImpl();
    }
  });

  private void _enableMinimizeToTray() {
    if (SystemTray.isSupported()) {

      tray = SystemTray.getSystemTray();

      ActionListener exitListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.exit(0);
        }
      };
      PopupMenu popup = new PopupMenu();
      MenuItem defaultItem = new MenuItem("Restore");
      defaultItem.setFont(new Font("Arial", Font.BOLD, 14));
      defaultItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setVisible(true);
          setExtendedState(JFrame.NORMAL);
        }
      });
      popup.add(defaultItem);
      defaultItem = new MenuItem("Exit");
      defaultItem.addActionListener(exitListener);
      defaultItem.setFont(new Font("Arial", Font.PLAIN, 14));
      popup.add(defaultItem);
      Image icon = new ImageIcon(getClass().getResource("/images/icon.png")).getImage();
      trayIcon = new TrayIcon(icon, "HearthStats.net Uploader", popup);
      trayIcon.setImageAutoSize(true);
      trayIcon.addMouseListener(new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
          if (e.getClickCount() >= 2) {
            setVisible(true);
            setExtendedState(JFrame.NORMAL);
          }
        }
      });
    } else {
      debugLog.debug("system tray not supported");
    }
    addWindowStateListener(new WindowStateListener() {
      public void windowStateChanged(WindowEvent e) {
        if (Config.minimizeToTray()) {
          if (e.getNewState() == ICONIFIED) {
            try {
              tray.add(trayIcon);
              setVisible(false);
            } catch (AWTException ex) {
            }
          }
          if (e.getNewState() == 7) {
            try {
              tray.add(trayIcon);
              setVisible(false);
            } catch (AWTException ex) {
            }
          }
          if (e.getNewState() == MAXIMIZED_BOTH) {
            tray.remove(trayIcon);
            setVisible(true);
          }
          if (e.getNewState() == NORMAL) {
            tray.remove(trayIcon);
            setVisible(true);
            debugLog.debug("Tray icon removed");
          }
        }
      }
    });

  }

  public void setMonitorHearthstoneLog(boolean monitorHearthstoneLog) {
    debugLog.debug("setMonitorHearthstoneLog({})", monitorHearthstoneLog);

    if (monitorHearthstoneLog) {
      // Ensure that the Hearthstone log.config file has been created
      Boolean configWasCreated = _hsHelper.createConfig();

      // Start monitoring the Hearthstone log immediately if Hearthstone is
      // already running
      if (_hearthstoneDetected) {
        if (configWasCreated) {
          // Hearthstone won't actually be logging yet because the log.config
          // was created after Hearthstone started up
          Log.help("Hearthstone log.config changed &mdash; please restart Hearthstone so that it starts generating logs");
        } else if (hearthstoneLogMonitor == null)
          hearthstoneLogMonitor = new HearthstoneLogMonitor();
      }
    } else {
      // Stop monitoring the Hearthstone log
      if (hearthstoneLogMonitor != null) {
        hearthstoneLogMonitor.stop();
        hearthstoneLogMonitor = null;
      }
    }
  }
}
