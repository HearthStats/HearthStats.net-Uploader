package net.hearthstats.ui

import java.awt.{Color, Desktop, Dimension}
import java.awt.event.{ActionEvent, ActionListener, KeyAdapter, KeyEvent}
import java.net.URI
import javax.swing.BorderFactory.{createCompoundBorder, createEmptyBorder, createMatteBorder}
import javax.swing.{JButton, JCheckBox, JComboBox, JLabel, JPanel, JTextArea, JTextField}
import javax.swing.event.{ChangeEvent, ChangeListener}

import net.hearthstats.{Constants, HearthstoneMatch, Main}
import net.hearthstats.analysis.HearthstoneAnalyser
import net.hearthstats.util.Translations.t
import net.miginfocom.swing.MigLayout

class MatchPanel extends JPanel {

  private val _currentOpponentNameField = new JTextField
  private val _currentMatchLabel = new JLabel
  private val _currentGameCoinField = new JCheckBox(t("match.coin"))
  private val _currentNotesField = new JTextArea()
  private val _lastMatchButton = new JButton("[n/a]")

  var lastMatch: HearthstoneMatch = _

  private var _currentMatchEnabled = false

  setLayout(new MigLayout)
  add(new JLabel(" "), "wrap")
  add(_currentMatchLabel, "skip,span,wrap")
  add(new JLabel(" "), "wrap")

  val localizedClassOptions = Array.ofDim[String](Constants.hsClassOptions.length)
  localizedClassOptions(0) = "- " + t("undetected") + " -"
  for (i <- 1 until localizedClassOptions.length) localizedClassOptions(i) = t(Constants.hsClassOptions(i))

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
      HearthstoneAnalyser.hsMatch.opponentName = _currentOpponentNameField.getText.replaceAll("(\r\n|\n)", "<br/>")
    }
  })

  add(_currentOpponentNameField, "wrap")
  add(new JLabel(t("match.label.coin") + " "), "skip,right")
  _currentGameCoinField.setSelected(false)
  _currentGameCoinField.addChangeListener(new ChangeListener {
    def stateChanged(e: ChangeEvent) {
      HearthstoneAnalyser.hsMatch.coin = _currentGameCoinField.isSelected
    }
  })

  add(_currentGameCoinField, "wrap")
  add(new JLabel(t("match.label.notes") + " "), "skip,wrap")

  _currentNotesField.setBorder(createCompoundBorder(createMatteBorder(1, 1, 1, 1, Color.black), createEmptyBorder(3, 6, 3, 6)))
  _currentNotesField.setMinimumSize(new Dimension(350, 150))
  _currentNotesField.setBackground(Color.WHITE)
  _currentNotesField.addKeyListener(new KeyAdapter {
    override def keyReleased(e: KeyEvent) {
      HearthstoneAnalyser.hsMatch.notes = _currentNotesField.getText
    }
  })

  add(_currentNotesField, "skip,span")
  add(new JLabel(" "), "wrap")
  add(new JLabel(t("match.label.previous_match") + " "), "skip,wrap")
  _lastMatchButton.addActionListener(new ActionListener {
    override def actionPerformed(arg0: ActionEvent) {
      val url = if ("Arena" == lastMatch.mode) "http://hearthstats.net/arenas/new" else lastMatch.editUrl
      try Desktop.getDesktop.browse(new URI(url))
      catch {
        case e: Throwable => Main.showErrorDialog("Error launching browser with URL " + url, e)
      }
    }
  })

  _lastMatchButton.setEnabled(false)
  add(_lastMatchButton, "skip,wrap,span")

  def updateCurrentMatchUi() {
    val currentMatch = HearthstoneAnalyser.hsMatch
    updateMatchClassSelectorsIfSet(currentMatch)
    if (_currentMatchEnabled) _currentMatchLabel.setText(currentMatch.mode + " Match - " + " Turn " + currentMatch.numTurns) else _currentMatchLabel.setText("Waiting for next match to start ...")
    _currentOpponentNameField.setText(currentMatch.opponentName)
    _currentOpponentClassSelect.setSelectedIndex(_getClassOptionIndex(currentMatch.opponentClass))
    _currentYourClassSelector.setSelectedIndex(_getClassOptionIndex(currentMatch.userClass))
    _currentGameCoinField.setSelected(currentMatch.coin)
    _currentNotesField.setText(currentMatch.notes)
    if (lastMatch != null && lastMatch.mode != null) {
      if (lastMatch.result != null) {
        val tooltip = (if (lastMatch.mode == "Arena") "View current arena run on" else "Edit the previous match") +
          " on HearthStats.net"
        _lastMatchButton.setToolTipText(tooltip)
        _lastMatchButton.setText(lastMatch.toString)
        _lastMatchButton.setEnabled(true)
      }
    }
  }

  def setCurrentMatchEnabled(enabled: Boolean) {
    _currentMatchEnabled = enabled
    _currentYourClassSelector.setEnabled(enabled)
    _currentOpponentClassSelect.setEnabled(enabled)
    _currentGameCoinField.setEnabled(enabled)
    _currentOpponentNameField.setEnabled(enabled)
    _currentNotesField.setEnabled(enabled)
  }

  private def _getClassOptionIndex(cName: String): Int = {
    (0 until Constants.hsClassOptions.length).find(Constants.hsClassOptions(_) == cName)
      .getOrElse(0)
  }

  def resetMatchClassSelectors() {
    _currentYourClassSelector.setSelectedIndex(0)
    _currentOpponentClassSelect.setSelectedIndex(0)
    _currentGameCoinField.setSelected(false)
  }

  def updateMatchClassSelectorsIfSet(hsMatch: HearthstoneMatch) {
    if (_currentYourClassSelector.getSelectedIndex > 0) {
      hsMatch.userClass = Constants.hsClassOptions(_currentYourClassSelector.getSelectedIndex)
    }
    if (_currentOpponentClassSelect.getSelectedIndex > 0) {
      hsMatch.opponentClass = Constants.hsClassOptions(_currentOpponentClassSelect.getSelectedIndex)
    }
  }
}
