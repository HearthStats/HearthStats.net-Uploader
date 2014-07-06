package net.hearthstats.updater.application;

import javax.swing.*;
import java.awt.*;


/**
 * The Swing UI for the updater.
 */
class ProgressWindow {

  private final JFrame frame;

  private JTextArea logTextArea;


  public ProgressWindow() {
    frame = new JFrame("Updating HearthStats Uploader");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    Container container = frame.getContentPane();
    createComponents(container);
  }


  public void open() {
    // Calculate the window size and display it
    frame.pack();
    frame.setVisible(true);
  }


  public void close() {
    frame.dispose();
  }


  public void log(final String message) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        logTextArea.append(message + "\n");
      }
    });
  }



  private void createComponents(Container container) {
    container.setLayout(new BorderLayout(5, 5));

    // Label
    JLabel headingLabel = new JLabel("Update Progress:");
    container.add(headingLabel, BorderLayout.NORTH);

    // Progress log
    logTextArea = new JTextArea(8, 60);
    logTextArea.setLineWrap(true);
    logTextArea.setEditable(false);
    logTextArea.setPreferredSize(new Dimension(400, 160));
    JScrollPane logScrollPane = new JScrollPane(logTextArea);
    logScrollPane.setPreferredSize(new Dimension(400, 160));
    container.add(logScrollPane, BorderLayout.CENTER);
  }


}
