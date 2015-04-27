package net.hearthstats.ui

import java.awt.{Color, Cursor, Dimension}
import javax.imageio.ImageIO
import javax.swing.{ImageIcon, JOptionPane}

import grizzled.slf4j.Logging
import net.hearthstats.config.{Environment, UserConfig}
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.ui.util.{FontHelper, MigPanel, PasswordOptionTextField, StringOptionTextField}
import net.hearthstats.util.{Browse, Translation}

import scala.concurrent.{Future, Promise}
import scala.swing.event.ButtonClicked
import scala.swing.{Component, Container, Frame, Label, Point}
import scala.util.control.NonFatal

class LandingFrame(translation: Translation,
                   uilog: Log,
                   config: UserConfig,
                   api: API,
                   notificationQueue: NotificationQueue,
                   environment: Environment) extends Frame with Logging {
  import config._
  import translation.t

  val signInButton = new scala.swing.Button {
    text = t("landingpanel.label.login")
    font = FontHelper.largerFont
    border = null
    focusable = false
    cursor = new Cursor(Cursor.HAND_CURSOR)
    contentAreaFilled = false
    foreground = new Color(46, 97, 140)
    borderPainted = false
  }

  defaultButton = signInButton

  val registerButton = new swing.Button {
    text = t("landingpanel.label.register")
    font = FontHelper.largerFont
    border = null
    focusable = false
    cursor = new Cursor(Cursor.HAND_CURSOR)
    contentAreaFilled = false
    foreground = new Color(46, 97, 140)
    borderPainted = false
  }
  private val registerUrl = "http://hearthstats.net/users/sign_up"

  var connected = Promise[Unit]()

  override def closeOperation(): Unit = {
    System.exit(0)
  }

  def createLandingPage(): Future[Unit] = {

    val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
    iconImage = icon
    location = new Point(windowX, windowY)
    size = new Dimension(windowWidth, windowHeight)
    minimumSize = new Dimension(500, 600)
    visible = true

    updateTitle()

    val panel = new MigPanel(
      //    layoutConstraints = "debug",
      colConstraints = "[]push[]10[]10[]push[]", // Causes form to be centred
      rowConstraints = "[]push[]20[]20[]10[]10[]20[]push[]") {

      background = Color.WHITE

      contents += (new Label {
        icon = new ImageIcon(ImageIO.read(getClass.getResource("/images/Hearthstats_icon.png")))
      }, "newline, skip, right")

      contents += (new Label {
        icon = new ImageIcon(ImageIO.read(getClass.getResource("/images/Hearthstats_title.png")))
      }, "right, wrap")

      val titleLabel = new Label {
        text = t("landingpanel.title")
        font = FontHelper.titleFont
      }
      contents += (titleLabel, "skip 2, center, wrap")

      contents += (new Label {
        text = t("landingpanel.label.email")
        font = FontHelper.largerFont
      }, "skip, right")

      val userEmailField = new StringOptionTextField(config.email) {
        font = FontHelper.largerFont
      }
      contents += (userEmailField, "center, wrap")

      contents += (new Label {
        text = t("landingpanel.label.password")
        font = FontHelper.largerFont
      }, "skip, right")

      val passwordField = new PasswordOptionTextField(config.password) {
        font = FontHelper.largerFont
      }
      contents += (passwordField, "center, wrap")

      contents += (signInButton, "skip 2, right, gapright 5, wrap")
      listenTo(signInButton)

      contents += (registerButton, "skip 2, right, gapright 5")
      listenTo(registerButton)

      val firstTimeHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/First-Time-Tutorial",
        "First time tutorial")
      contents += (firstTimeHelpIcon, "left, wrap")

      reactions += {
        case ButtonClicked(`signInButton`) =>
          checkForPassword
        case ButtonClicked(`registerButton`) =>
          Browse(registerUrl)
      }
    }

    contents = panel
    background = Color.WHITE

    if (config.lastLoginOK) {
      new Thread {
        override def run(): Unit = {
          checkForPassword()
        }
      }.start()
    }
    connected.future
  }

  //update title
  def updateTitle() {
    title = "HearthStats Companion Login Page"
  }

  def login(): Boolean = try {
    setEnabled(false)
    cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
    api.login(config.email.get, config.password.get)
  } finally {
    setEnabled(true)
    cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  }

  def setEnabled(e: Boolean): Unit = {
    def enable(c: Component): Unit = {
      c.enabled = e
      if (c.isInstanceOf[Container]) {
        for (i <- c.asInstanceOf[Container].contents) enable(i)
      }
    }
    for (c <- contents) enable(c)
    repaint()
  }

  //check if password and email matched
  def checkForPassword(): Unit =
    try {
      if (login()) {
        connected.success(())
        config.lastLoginOK.set(true)
        info("Password correct")
        dispose()
      } else {
        JOptionPane.showMessageDialog(peer, "Invalid Email or Password")
        config.lastLoginOK.set(false)
        info("Invalid email or password")
      }
    } catch {
      case NonFatal(e) => throw new Exception("entered nothing", e)
    }
}

