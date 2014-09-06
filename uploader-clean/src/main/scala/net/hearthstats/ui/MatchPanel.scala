package net.hearthstats.ui

import java.awt.Color
import java.awt.Dimension
import java.awt.event.{ ActionEvent, ActionListener, KeyAdapter, KeyEvent }

import javax.swing.{ JButton, JCheckBox, JComboBox, JLabel, JPanel, JTextArea, JTextField }
import javax.swing.BorderFactory.{ createCompoundBorder, createEmptyBorder, createMatteBorder }
import javax.swing.event.{ ChangeEvent, ChangeListener }
import net.hearthstats.companion.CompanionState
import net.hearthstats.core.{ HearthstoneMatch, HeroClass }
import net.hearthstats.game.MatchState
import net.hearthstats.ui.log.Log
import net.hearthstats.util.{ Browse, Translation }
import net.miginfocom.swing.MigLayout

class MatchPanel(
  matchState: MatchState,
  translation: Translation,
  uiLog: Log) extends JPanel {
  import translation.t

  private val _currentOpponentNameField = new JTextField
  private val _currentMatchLabel = new JLabel
  private val _currentGameCoinField = new JCheckBox(t("match.coin"))
  private val _currentNotesField = new JTextArea()
  private val _lastMatchButton = new JButton("[n/a]")

  setLayout(new MigLayout)
  add(new JLabel(" "), "wrap")
  add(_currentMatchLabel, "skip,span,wrap")
  add(new JLabel(" "), "wrap")

  val localizedClassOptions = Array.ofDim[String](HeroClass.values.length)
  localizedClassOptions(0) = "- " + t("undetected") + " -"
  for (i <- 1 until localizedClassOptions.length) localizedClassOptions(i) = t(HeroClass.stringWithId(i))

  private val _currentOpponentClassSelect = new JComboBox(localizedClassOptions)
  private val _currentYourClassSelector = new JComboBox(localizedClassOptions)

  add(new JLabel(t("match.label.your_class") + " "), "skip,right")
  add(_currentYourClassSelector, "wrap")
  add(new JLabel(t("match.label.opponents_class") + " "), "skip,right")
  add(_currentOpponentClassSelect, "wrap")
  add(new JLabel("Opponent's Name: "), "skip,right")

  _currentOpponentNameField.setMinimumSize(new Dimension(100, 1))
  _currentOpponentNameField.addKeyListener(new KeyAdapter {
    override def keyReleased(e: KeyEvent) {
      matchState.setOpponentName(_currentOpponentNameField.getText.replaceAll("(\r\n|\n)", "<br/>"))
    }
  })

  add(_currentOpponentNameField, "wrap")
  add(new JLabel(t("match.label.coin") + " "), "skip,right")
  _currentGameCoinField.setSelected(false)
  _currentGameCoinField.addChangeListener(new ChangeListener {
    def stateChanged(e: ChangeEvent) {
      matchState.setCoin(_currentGameCoinField.isSelected)
    }
  })

  add(_currentGameCoinField, "wrap")
  add(new JLabel(t("match.label.notes") + " "), "skip,wrap")

  _currentNotesField.setBorder(createCompoundBorder(createMatteBorder(1, 1, 1, 1, Color.black), createEmptyBorder(3, 6, 3, 6)))
  _currentNotesField.setMinimumSize(new Dimension(350, 150))
  _currentNotesField.setBackground(Color.WHITE)
  _currentNotesField.addKeyListener(new KeyAdapter {
    override def keyReleased(e: KeyEvent) {
      matchState.setNotes(_currentNotesField.getText)
    }
  })

  add(_currentNotesField, "skip,span")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("match.label.previous_match") + " "), "skip,wrap")
  _lastMatchButton.addActionListener(new ActionListener {
    override def actionPerformed(arg0: ActionEvent) {
      matchState.lastMatchUrl map Browse.apply
    }
  })

  _lastMatchButton.setEnabled(false)
  add(_lastMatchButton, "skip,wrap,span")

  def updateCurrentMatchUi() {
    matchState.currentMatch match {
      case Some(m) =>
        updateMatchClassSelectorsIfSet(m)
        _currentMatchLabel.setText(m.mode + " Match - " + " Turn " + m.numTurns)
        _currentOpponentNameField.setText(m.opponentName)
        _currentOpponentClassSelect.setSelectedIndex(_getClassOptionIndex(m.opponentClass))
        _currentYourClassSelector.setSelectedIndex(_getClassOptionIndex(m.userClass))
        _currentGameCoinField.setSelected(m.coin.getOrElse(false))
        _currentNotesField.setText(m.notes)
      case None =>
        _currentMatchLabel.setText("Waiting for next match to start ...")
    }
    matchState.lastMatch map { m =>
      if (m.mode != null && m.result != null) { //TODO use options
        val tooltip = if (m.mode == "Arena") "View current arena run on" else "Edit the previous match"
        _lastMatchButton.setToolTipText(tooltip + " on HearthStats.net")
        _lastMatchButton.setText(m.toString)
        _lastMatchButton.setEnabled(true)
      }
    }
  }

  def setCurrentMatchEnabled(enabled: Boolean) {
    _currentYourClassSelector.setEnabled(enabled)
    _currentOpponentClassSelect.setEnabled(enabled)
    _currentGameCoinField.setEnabled(enabled)
    _currentOpponentNameField.setEnabled(enabled)
    _currentNotesField.setEnabled(enabled)
  }

  private def _getClassOptionIndex(hClass: HeroClass): Int =
    hClass.ordinal

  def resetMatchClassSelectors() {
    _currentYourClassSelector.setSelectedIndex(0)
    _currentOpponentClassSelect.setSelectedIndex(0)
  }

  def updateMatchClassSelectorsIfSet(hsMatch: HearthstoneMatch) {
    if (_currentYourClassSelector.getSelectedIndex > 0) {
      hsMatch.userClass = HeroClass.values.apply(_currentYourClassSelector.getSelectedIndex)
    }
    if (_currentOpponentClassSelect.getSelectedIndex > 0) {
      hsMatch.opponentClass = HeroClass.values.apply(_currentOpponentClassSelect.getSelectedIndex)
    }
  }
}
