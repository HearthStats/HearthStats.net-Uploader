package net.hearthstats.ui

import java.awt.{Color, Cursor, Dimension}
import javax.imageio.ImageIO
import javax.swing._

import grizzled.slf4j.Logging
import net.hearthstats.config.{Environment, UserConfig}
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.ui.util._
import net.hearthstats.util.{Browse, Translation}

import scala.swing.event.ButtonClicked
import scala.swing.{Frame, Label, Point}

class LandingFrame(translation: Translation,
                   uilog: Log,
                   config: UserConfig,
                   api: API,
                   val notificationQueue: NotificationQueue,
                   val environment: Environment) extends Frame with Logging {
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



  def createLandingPage() {

    val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
    iconImage = icon
    location = new Point(windowX, windowY)
    size = new Dimension(windowWidth, windowHeight)
    minimumSize = new Dimension(500, 600)
    visible = true

    updateTitle

    val panel = new MigPanel(
//    layoutConstraints = "debug",
    colConstraints = "[]push[]10[]10[]push[]",   // Causes form to be centred
    rowConstraints = "[]push[]20[]20[]10[]10[]20[]push[]"
    ) {

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

      val url = ImageIO.read(getClass.getResource("/images/loading-icon.gif"))
      val imageIcon = new ImageIcon(url)
      val label = new JLabel(imageIcon)
      
      reactions += {
        case ButtonClicked(`signInButton`) =>{
          //signInButton.cursor = (Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR))
          //contents += (new Label{icon = new ImageIcon(url)}, "center, wrap")
          checkForPassword
        }
        case ButtonClicked(`registerButton`) =>
          Browse(registerUrl)
      }
    }

    contents = panel
    background = Color.WHITE
  }

  //update title
  def updateTitle {
    title = "HearthStats Companion Login Page"
  }

  //close login page
  def closeLandingPage {
    config.closedLandingPage.set(true)
  }

  //check if password and email matched
  def checkForPassword {
    try {
      if (api.login(config.email.get, config.password.get)) {
        closeLandingPage
        JOptionPane.showMessageDialog(null,"Password Correct")
        info("Password correct")
      } else {
        JOptionPane.showMessageDialog(null, "Invalid Email or Password")
        info("Invalid email or password")
      }
    } catch {
      case e: Throwable => throw new Exception("entered nothing", e)
    }

  }
}

