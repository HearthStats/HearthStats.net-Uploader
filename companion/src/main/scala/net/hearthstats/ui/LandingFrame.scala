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
<<<<<<< HEAD
    
    private val signInButton = new JButton("Sign In")
    signInButton.setBorder(null)
    signInButton.setFocusable(false)
    signInButton.setCursor(new Cursor(Cursor.HAND_CURSOR))
    signInButton.setOpaque(false)
    signInButton.setContentAreaFilled(false)
    signInButton.setBorderPainted(false)
    private val registerButton = new JButton("Register")
    registerButton.setBorder(null)
    registerButton.setFocusable(false)
    registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR))
    registerButton.setOpaque(false)
    registerButton.setContentAreaFilled(false)
    registerButton.setBorderPainted(false)
    private val registerUrl:String = "http://hearthstats.net/users/sign_up"
    

    def createLandingPage(){
      val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
      val titleLabel = new JLabel("Uploader")
      titleLabel.setFont(new Font("Ariel",Font.BOLD,36))
      setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE)
      setIconImage(icon)
      setLocation(windowX, windowY)
      setSize(windowWidth, windowHeight)
      setMinimumSize(new Dimension(500, 600))
      setVisible(true)
      updateTitle

      setLayout(new MigLayout)
      getContentPane().setBackground(Color.WHITE)
      getRootPane().setBorder(BorderFactory.createMatteBorder(4, 4, 4, 4, Color.WHITE));
      
      
      add(new JLabel(" "), "wrap")    
      add(new JLabel(" "), "wrap")
      add(new JLabel(" "), "wrap")
      add(new JLabel("  "), "")
      add(new JLabel(new ImageIcon(ImageIO.read(getClass.getResource("/images/Hearthstats_icon.png")))),"right")
      add(new JLabel(new ImageIcon(ImageIO.read(getClass.getResource("/images/Hearthstats_title.png")))),"right")
      add(new JLabel("       "),"right")
      
      add(new JLabel(" "),"wrap")
      add(new JLabel(" "),"wrap")
      add(new JLabel(" "),"wrap")
      add(new JLabel(" "),"wrap")
      add(new JLabel("  "), "")  
      //entering userID
      add(new JLabel(t("LandingPanel.label.userId") + "    "), "right")
      var userEmailField: JTextField = new StringOptionTextField(config.email)
      
      add(userEmailField, "center,wrap")
      config.email.set(userEmailField.getText())
        
      
      add(new JLabel(" "), "wrap")
      add(new JLabel("  "), "") 
      //entering user Password  
      add(new JLabel(t("LandingPanel.label.password") + " "), "right")
      var passwordField: JTextField = new StringOptionTextField(config.password)
      passwordField.setOpaque(false)
      passwordField.setBackground(new Color(0,0,0,0))
      
      add(passwordField, "center,wrap")
      config.password.set(passwordField.getText())
        
        
      add(new JLabel(" "), "wrap")
      add(new JLabel(" "), "wrap")
      add(new JLabel("  "), "")  
      //add a sign in button
      signInButton.addActionListener(new ActionListener {
        override def actionPerformed(arg0: ActionEvent) {
         checkForPassword   
        }})
      add(signInButton, "right")
      
      //add a register button 
      registerButton.addActionListener(new ActionListener {
        override def actionPerformed(arg0: ActionEvent) {
          Browse(registerUrl)
        }})
      add(registerButton, "center")
        
      //add a first time help link  
      val firstTimeHelpIcon = new HelpIcon("http://hearthstats.net/uploader?locale=en",
       "First time tutorial")
      add(firstTimeHelpIcon, "left")
    
=======
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
>>>>>>> HearthStats/master
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
      listenTo(signInButton);

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
