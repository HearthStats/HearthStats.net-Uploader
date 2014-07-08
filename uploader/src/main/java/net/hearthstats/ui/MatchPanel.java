package net.hearthstats.ui;

import static net.hearthstats.util.Translations.t;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.hearthstats.Config;
import net.hearthstats.Constants;
import net.hearthstats.HearthstoneMatch;
import net.hearthstats.Main;
import net.hearthstats.analysis.HearthstoneAnalyser;
import net.miginfocom.swing.MigLayout;


public class MatchPanel extends JPanel {
  private JTextField _currentOpponentNameField;
  private JLabel _currentMatchLabel;
  private JCheckBox _currentGameCoinField;
  private JTextArea _currentNotesField;
  private JButton _lastMatchButton;
  private HearthstoneMatch _lastMatch;

  public void setLastMatch(HearthstoneMatch _lastMatch) {
    this._lastMatch = _lastMatch;
  }

  private JComboBox _currentOpponentClassSelect;
  private JComboBox _currentYourClassSelector;

  private Boolean _currentMatchEnabled = false;

  public MatchPanel() {

    MigLayout layout = new MigLayout();
    setLayout(layout);

    // match label
    add(new JLabel(" "), "wrap");
    _currentMatchLabel = new JLabel();
    add(_currentMatchLabel, "skip,span,wrap");

    add(new JLabel(" "), "wrap");

    String[] localizedClassOptions = new String[Constants.hsClassOptions.length];
    localizedClassOptions[0] = "- " + t("undetected") + " -";
    for (int i = 1; i < localizedClassOptions.length; i++)
      localizedClassOptions[i] = t(Constants.hsClassOptions[i]);

    // your class
    add(new JLabel(t("match.label.your_class") + " "), "skip,right");
    _currentYourClassSelector = new JComboBox<>(localizedClassOptions);
    add(_currentYourClassSelector, "wrap");

    // opponent class
    add(new JLabel(t("match.label.opponents_class") + " "), "skip,right");
    _currentOpponentClassSelect = new JComboBox<>(localizedClassOptions);
    add(_currentOpponentClassSelect, "wrap");

    // Opponent name
    add(new JLabel("Opponent's Name: "), "skip,right");
    _currentOpponentNameField = new JTextField();
    _currentOpponentNameField.setMinimumSize(new Dimension(100, 1));
    _currentOpponentNameField.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        HearthstoneAnalyser.hsMatch().opponentName_$eq(
            _currentOpponentNameField.getText().replaceAll("(\r\n|\n)", "<br/>"));
      }
    });
    add(_currentOpponentNameField, "wrap");

    // coin
    add(new JLabel(t("match.label.coin") + " "), "skip,right");
    _currentGameCoinField = new JCheckBox(t("match.coin"));
    _currentGameCoinField.setSelected(Config.showHsClosedNotification());
    _currentGameCoinField.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        HearthstoneAnalyser.hsMatch().coin_$eq(_currentGameCoinField.isSelected());
      }
    });
    add(_currentGameCoinField, "wrap");

    // notes
    add(new JLabel(t("match.label.notes") + " "), "skip,wrap");
    _currentNotesField = new JTextArea();
    _currentNotesField.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(1, 1, 1, 1, Color.black),
        BorderFactory.createEmptyBorder(3, 6, 3, 6)));
    _currentNotesField.setMinimumSize(new Dimension(350, 150));
    _currentNotesField.setBackground(Color.WHITE);
    _currentNotesField.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {
        HearthstoneAnalyser.hsMatch().notes_$eq(_currentNotesField.getText());
      }
    });
    add(_currentNotesField, "skip,span");

    add(new JLabel(" "), "wrap");

    // last match
    add(new JLabel(t("match.label.previous_match") + " "), "skip,wrap");
    _lastMatchButton = new JButton("[n/a]");
    _lastMatchButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent arg0) {
        String url = "Arena".equals(_lastMatch.mode()) ? "http://hearthstats.net/arenas/new"
            : _lastMatch.editUrl();
        try {
          Desktop.getDesktop().browse(new URI(url));
        } catch (Throwable e) {
          Main.showErrorDialog("Error launching browser with URL " + url, e);
        }
      }
    });
    _lastMatchButton.setEnabled(false);
    add(_lastMatchButton, "skip,wrap,span");

  }

  public void updateCurrentMatchUi() {
    HearthstoneMatch match = HearthstoneAnalyser.hsMatch();
    updateMatchClassSelectorsIfSet(match);
    if (_currentMatchEnabled)
      _currentMatchLabel.setText(match.mode() + " Match - " + " Turn " + match.numTurns());
    else
      _currentMatchLabel.setText("Waiting for next match to start ...");
    _currentOpponentNameField.setText(match.opponentName());

    _currentOpponentClassSelect.setSelectedIndex(_getClassOptionIndex(match.opponentClass()));
    _currentYourClassSelector.setSelectedIndex(_getClassOptionIndex(match.userClass()));

    _currentGameCoinField.setSelected(match.coin());
    _currentNotesField.setText(match.notes());
    // last match
    if (_lastMatch != null && _lastMatch.mode() != null) {
      if (_lastMatch.result() != null) {
        String tooltip = (_lastMatch.mode().equals("Arena") ? "View current arena run on"
            : "Edit the previous match") + " on HearthStats.net";
        _lastMatchButton.setToolTipText(tooltip);
        _lastMatchButton.setText(_lastMatch.toString());
        _lastMatchButton.setEnabled(true);
      }
    }
  }

  public void setCurrentMatchEnabled(Boolean enabled) {
    _currentMatchEnabled = enabled;
    _currentYourClassSelector.setEnabled(enabled);
    _currentOpponentClassSelect.setEnabled(enabled);
    _currentGameCoinField.setEnabled(enabled);
    _currentOpponentNameField.setEnabled(enabled);
    _currentNotesField.setEnabled(enabled);
  }

  private int _getClassOptionIndex(String cName) {
    for (int i = 0; i < Constants.hsClassOptions.length; i++) {
      if (Constants.hsClassOptions[i].equals(cName)) {
        return i;
      }
    }
    return 0;
  }

  public void resetMatchClassSelectors() {
    _currentYourClassSelector.setSelectedIndex(0);
    _currentOpponentClassSelect.setSelectedIndex(0);
  }

  public void updateMatchClassSelectorsIfSet(HearthstoneMatch hsMatch) {
    if (_currentYourClassSelector.getSelectedIndex() > 0) {
      hsMatch.userClass_$eq(Constants.hsClassOptions[_currentYourClassSelector.getSelectedIndex()]);
    }
    if (_currentOpponentClassSelect.getSelectedIndex() > 0) {
      hsMatch.opponentClass_$eq(Constants.hsClassOptions[_currentOpponentClassSelect
          .getSelectedIndex()]);
    }
  }
}
