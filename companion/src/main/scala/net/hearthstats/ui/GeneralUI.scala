package net.hearthstats.ui

import java.awt.Frame._
import java.awt.event.{ WindowAdapter, WindowEvent }
import scala.swing.Swing
import grizzled.slf4j.Logging
import javax.swing.JFrame
import net.hearthstats.config.{ Environment, UserConfig }
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.NotificationQueue
import java.awt.SystemTray
import java.awt.PopupMenu
import java.awt.AWTException
import javax.swing.ImageIcon
import java.awt.event.WindowStateListener
import java.awt.TrayIcon
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.awt.MenuItem
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.Font
import javax.swing.JOptionPane
import java.io.File
import javax.swing.JFileChooser

/**
 * Defines generic UI features, non specific to HS.
 * Such as notifications, confirmation dialog ...
 */
trait GeneralUI extends JFrame with Logging {

  val environment: Environment
  val uiLog: Log
  val config: UserConfig
  val notificationQueue: NotificationQueue

  import config._

  addWindowListener(new WindowAdapter {
    override def windowClosing(e: WindowEvent) {
      handleClose()
    }
  })

  def showConfirmDialog(message: Any, title: String, optionType: Int): Int =
    JOptionPane.showConfirmDialog(this, message, title, optionType)

  def showFileDialog(message: Any): Option[File] = {
    JOptionPane.showMessageDialog(this, message)
    val chooser = new JFileChooser
    chooser.showOpenDialog(this) match {
      case JFileChooser.APPROVE_OPTION =>
        Some(chooser.getSelectedFile)
      case _ => None
    }
  }

  def showOptionDialog(message: Any, title: String, optionType: Int, values: Array[AnyRef]): Int =
    JOptionPane.showOptionDialog(
      this,
      message,
      title,
      optionType,
      JOptionPane.QUESTION_MESSAGE,
      null,
      values,
      values(0))

  private def handleClose() {
    try {
      val p = getLocationOnScreen
      windowX.set(p.x)
      windowY.set(p.y)
      val rect = getSize
      windowWidth.set(rect.getWidth.toInt)
      windowHeight.set(rect.getHeight.toInt)
    } catch {
      case t: Exception => uiLog.warn("Error occurred trying to save your settings, your window position may not be saved", t)
    }
    System.exit(0)
  }

  /**
   * Brings the monitor window to the front of other windows. Should only be
   * used for important events like a modal dialog or error that we want the
   * user to see immediately.
   */
  def bringWindowToFront() {
    Swing.onEDT(setVisible(true))
  }

  /**
   * Overridden version of setVisible based on
   * http://stackoverflow.com/questions
   * /309023/how-to-bring-a-window-to-the-front that should ensure the window is
   * brought to the front for important things like modal dialogs.
   */
  override def setVisible(visible: Boolean) {
    if (!visible || !isVisible) {
      super.setVisible(visible)
    }
    if (visible) {
      var state = getExtendedState
      state &= ~ICONIFIED
      setExtendedState(state)
      setAlwaysOnTop(true)
      super.toFront()
      requestFocus()
      setAlwaysOnTop(false)
    }
  }

  override def toFront() {
    super.setVisible(true)
    var state = getExtendedState
    state &= ~ICONIFIED
    setExtendedState(state)
    setAlwaysOnTop(true)
    super.toFront()
    requestFocus()
    setAlwaysOnTop(false)
  }

  def notify(header: String) {
    notify(header, "")
  }

  def notify(header: String, message: String) {
    if (notifyOverall) notificationQueue.add(header, message, false)
  }

  def enableMinimizeToTray() {
    if (SystemTray.isSupported) {
      val tray = SystemTray.getSystemTray
      val popup = new PopupMenu
      popup.add(restoreButton)
      popup.add(exitButton)
      val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
      val trayIcon = new TrayIcon(icon, "HearthStats Companion", popup)
      trayIcon.setImageAutoSize(true)
      trayIcon.addMouseListener(new MouseAdapter {
        override def mousePressed(e: MouseEvent) {
          if (e.getClickCount >= 2) {
            setVisible(true)
            setExtendedState(NORMAL)
          }
        }
      })
      addWindowStateListener(new WindowStateListener {
        def windowStateChanged(e: WindowEvent) {
          if (enableMinToTray) {
            e.getNewState match {
              case ICONIFIED =>
                try {
                  tray.add(trayIcon)
                  setVisible(false)
                } catch {
                  case ex: AWTException => debug(ex.getMessage, ex)
                }
              case MAXIMIZED_BOTH | NORMAL =>
                tray.remove(trayIcon)
                setVisible(true)
                debug("Tray icon removed")
            }
          }
        }
      })
    } else debug("system tray not supported")
  }

  lazy val restoreButton = {
    val button = new MenuItem("Restore")
    button.setFont(new Font("Arial", Font.BOLD, 14))
    button.addActionListener(new ActionListener {
      def actionPerformed(e: ActionEvent) {
        setVisible(true)
        setExtendedState(NORMAL)
      }
    })
    button
  }

  lazy val exitButton = {
    val exitListener = new ActionListener() {
      def actionPerformed(e: ActionEvent) {
        System.exit(0)
      }
    }
    val button = new MenuItem("Exit")
    button.addActionListener(exitListener)
    button.setFont(new Font("Arial", Font.PLAIN, 14))
    button
  }

}