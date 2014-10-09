package net.hearthstats.util

import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import javax.swing.event.ChangeListener
import javax.swing.event.ChangeEvent

object SwingHelper {
  implicit def actionListener(f: () => Unit): ActionListener = new ActionListener {
    def actionPerformed(e: ActionEvent) = f()
  }

  implicit def changeListener(f: () => Unit): ChangeListener = new ChangeListener {
    def stateChanged(e: ChangeEvent) = f()
  }

}