package net.hearthstats;

import java.awt.Component;
import java.io.File;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import net.hearthstats.config.Application;
import net.hearthstats.config.Environment;
import net.hearthstats.log.LogPane;
import net.hearthstats.notification.DialogNotification;
import net.sourceforge.tess4j.Tesseract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {
  private Main() {} // never instantiated


  private static Logger debugLog = LoggerFactory.getLogger(Main.class);

  private static String ocrLanguage = "eng";

  private static Monitor monitor;


  public static LogPane getLogPane() {
    if (monitor == null) {
      return null;
    } else {
      return monitor.mainFrame().getLogPane();
    }
  }


  public static void showErrorDialog(String message, Throwable e) {
    debugLog.error(message, e);
    JFrame frame = new JFrame();
    frame.setFocusableWindowState(true);
    Main.showMessageDialog(null, message + "\n" + e.getMessage() + "\n\nSee log.txt for details");
  }


  public static void showMessageDialog(Component parentComponent, String message) {
    JOptionPane op = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE);
    JDialog dialog = op.createDialog(parentComponent, "HearthStats.net");
    dialog.setAlwaysOnTop(true);
    dialog.setModal(true);
    dialog.setFocusableWindowState(true);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.setVisible(true);
  }


  public static void start(Environment environment) {

    try {

      DialogNotification loadingNotification = new DialogNotification("HearthStats.net Uploader", "Loading ...");
      loadingNotification.show();

      Updater.cleanUp();
      Config.rebuild(environment);

      logSystemInformation(environment);

      cleanupDebugFiles(environment);

      loadingNotification.close();

      monitor = new Monitor(environment);
      monitor.start();

    } catch (Throwable e) {
      Main.showErrorDialog("Error in Main", e);
      System.exit(1);
    }

  }


  private static void logSystemInformation(Environment environment) {
    if (debugLog.isInfoEnabled()) {
      debugLog.info("**********************************************************************");
      debugLog.info("  Starting HearthStats.net Uploader {} on {}", Application.version(), environment.os());
      debugLog.info("  os.name={}", environment.systemProperty("os.name"));
      debugLog.info("  os.version={}", environment.systemProperty("os.version"));
      debugLog.info("  os.arch={}", environment.systemProperty("os.arch"));
      debugLog
          .info("  java.runtime.version={}", environment.systemProperty("java.runtime.version"));
      debugLog.info("  java.class.path={}", environment.systemProperty("java.class.path"));
      debugLog.info("  java.library.path={}", environment.systemProperty("java.library.path"));
      debugLog.info("  user.language={}", environment.systemProperty("user.language"));
      debugLog.info("**********************************************************************");
    }
  }


  private static void cleanupDebugFiles(Environment environment) {
    try {
      File folder = new File(environment.extractionFolder());
      if (folder.exists()) {
        File[] files = folder.listFiles();
        for (File file : files) {
          if (file.isFile() && file.getName().startsWith("class-") && file.getName().endsWith(".png")) {
            // This is a hero/class image used for debugging, so it should be deleted
            file.delete();
          }
        }
      }
    } catch (Exception e) {
      debugLog.warn("Ignoring exception when cleaning up debug files", e);
    }
  }


  public static void setupTesseract(String outPath) {
    Tesseract instance = Tesseract.getInstance();
    instance.setDatapath(outPath + "tessdata");
    instance.setLanguage(ocrLanguage);
  }

}