package net.hearthstats.updater.application;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * The Swing UI for the updater.
 */
class ProgressWindow {

  public static final int EVENT_CANCEL = 1;

  private final JFrame frame;

  private final ActionListener actionListener;

  private JProgressBar downloadProgressBar;
  private JLabel progressLabel;
  private JScrollPane logScrollPane;
  private JButton cancelButton;


  private JTextArea logTextArea;


  public ProgressWindow(ActionListener actionListener) {
    this.actionListener = actionListener;

    frame = new JFrame("Updating HearthStats Companion");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLayout(new BorderLayout());
    frame.setContentPane(new ImagePanel("/net/hearthstats/updater/background.jpg", 0, 0, false));

    Container container = frame.getContentPane();
    createComponents(container);
  }


  public void open() {
    // Calculate the window size and display it
    frame.pack();
    frame.setResizable(false);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
  }


  public void close() {
    frame.dispose();
  }


  public void log(final String message) {
    System.out.println(message);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        logTextArea.append(message + "\n");
      }
    });
  }

  public void setProgress(String label) {
    progressLabel.setText(label);

  }

  public void setProgress(String label, int progress, int total) {

    if (downloadProgressBar.getMaximum() != total) {
      downloadProgressBar.setMaximum(total);
    }

    downloadProgressBar.setValue(progress);

    float progressMegabytes = progress / 1048576f;
    float totalMegabytes = total / 1048576f;

    String fullLabel = String.format("%1$s %2$.1f/%3$.1fMB", label, progressMegabytes, totalMegabytes);

    progressLabel.setText(fullLabel);
  }


  public void enableCancelButton() {
    cancelButton.setEnabled(true);
  }


  public void disableCancelButton() {
    cancelButton.setEnabled(false);
  }



  private void createComponents(Container container) {
    container.setLayout(new BorderLayout(0, 0));
    Dimension windowSize = new Dimension(500, 350);
    container.setPreferredSize(windowSize);
    container.setMinimumSize(windowSize);
    container.setMaximumSize(windowSize);

    // Heading panel that contains the HearthStats Companion text
//    final JPanel headingPanel = new JPanel();
    final JPanel headingPanel = new ImagePanel("/net/hearthstats/updater/logo.png", 12, 12, true);
    headingPanel.setLayout(new GridBagLayout());
    headingPanel.setBackground(new Color(0x212121));
    container.add(headingPanel, BorderLayout.NORTH);

    headingPanel.setMinimumSize(new Dimension(500, 66));
    headingPanel.setPreferredSize(new Dimension(500, 66));

    GridBagConstraints gbc = new GridBagConstraints();

    // Main panel containing the progress bar and log
    final JPanel panel3 = new JPanel();
    panel3.setLayout(new GridBagLayout());
    panel3.setOpaque(false);
    container.add(panel3, BorderLayout.CENTER);

    // Label above the progress bar
    progressLabel = new JLabel();
    progressLabel.setForeground(Color.WHITE);
    progressLabel.setFont(new Font(progressLabel.getFont().getName(), progressLabel.getFont().getStyle(), 14));
    progressLabel.setHorizontalAlignment(0);
    progressLabel.setText("Download Progress");
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.gridwidth = 2;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(10, 12, 0, 12);
    panel3.add(progressLabel, gbc);

    // Progress bar
    downloadProgressBar = new JProgressBar();
    downloadProgressBar.setIndeterminate(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 1;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(3, 12, 3, 0);
    panel3.add(downloadProgressBar, gbc);

    // Cancel button next to the progress bar
    cancelButton = new JButton();
    cancelButton.setText("Cancel");
    cancelButton.setEnabled(false);
    cancelButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        // Tell the calling class that this has been cancelled
        actionListener.actionPerformed(new ActionEvent(this, EVENT_CANCEL, "cancel"));
      }
    });
    gbc = new GridBagConstraints();
    gbc.gridx = 1;
    gbc.gridy = 1;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 8, 0, 10);
    panel3.add(cancelButton, gbc);

    // Text area for the logs
    logTextArea = new JTextArea(8, 60);
    logTextArea.setLineWrap(true);
    logTextArea.setEditable(false);
    logTextArea.setOpaque(false);

    // Scroll pane for the logs
    logScrollPane = new JScrollPane(logTextArea) {
      /**
       * 
       */
      private static final long serialVersionUID = 1416654057851584623L;

      // Custom painting to make the text area partially translucent and show some of the background
      @Override
      protected void paintComponent(Graphics g) {
        g.setColor( getBackground() );
        g.fillRect(0, 0, getWidth(), getHeight());
        super.paintComponent(g);
      }
    };
    logScrollPane.setBackground(new Color(255, 255, 255, 200));
    logScrollPane.setOpaque(false);
    logScrollPane.getViewport().setOpaque(false);
    gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 2;
    gbc.gridwidth = 2;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.insets = new Insets(10, 12, 12, 12);
    panel3.add(logScrollPane, gbc);

  }


}
