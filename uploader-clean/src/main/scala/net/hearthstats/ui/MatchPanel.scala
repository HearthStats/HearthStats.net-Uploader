package net.hearthstats.ui

import java.awt.Color
import java.awt.Desktop
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.net.URI
import javax.swing.BorderFactory.createCompoundBorder
import javax.swing.BorderFactory.createEmptyBorder
import javax.swing.BorderFactory.createMatteBorder
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.JTextField
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener
import net.hearthstats.Main
import net.miginfocom.swing.MigLayout
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.log.Log
import net.hearthstats.util.Translation
import net.hearthstats.core.HearthstoneMatch
import net.hearthstats.core.HeroClasses
import net.hearthstats.util.Browse
import net.hearthstats.companion.CompanionState
import net.hearthstats.game.MatchState

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

  val localizedClassOptions = Array.ofDim[String](HeroClasses.all.length)
  localizedClassOptions(0) = "- " + t("undetected") + " -"
  for (i <- 1 until localizedClassOptions.length) localizedClassOptions(i) = t(HeroClasses.all(i))

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
        _currentGameCoinField.setSelected(m.coin)
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

  private def _getClassOptionIndex(cName: String): Int =
    (0 until HeroClasses.all.length).find(HeroClasses.all(_) == cName)
      .getOrElse(0)

  def resetMatchClassSelectors() {
    _currentYourClassSelector.setSelectedIndex(0)
    _currentOpponentClassSelect.setSelectedIndex(0)
  }

  def updateMatchClassSelectorsIfSet(hsMatch: HearthstoneMatch) {
    if (_currentYourClassSelector.getSelectedIndex > 0) {
      hsMatch.userClass = HeroClasses.all(_currentYourClassSelector.getSelectedIndex)
    }
    if (_currentOpponentClassSelect.getSelectedIndex > 0) {
      hsMatch.opponentClass = HeroClasses.all(_currentOpponentClassSelect.getSelectedIndex)
    }
  }
}
