package net.hearthstats.ui

import java.awt.event.{ ActionEvent, ActionListener }
import java.awt.{Dimension,Font,Graphics}
import java.awt.Frame._
import java.awt.image.BufferedImage;
import javax.swing._
import javax.imageio.ImageIO;
import rapture.json._
import grizzled.slf4j.Logging
import net.hearthstats.ProgramHelper
import net.hearthstats.companion.CompanionState
import net.hearthstats.config.{ Environment, UserConfig }
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.util.StringOptionTextField
import net.hearthstats.ui.notification.NotificationQueue
import net.hearthstats.util.{ Browse, Translation }

class LandingFrame (val environment: Environment,
  val config: UserConfig,
  val notificationQueue: NotificationQueue,
  val api: API,
  programHelper: ProgramHelper,
  translation: Translation) extends GeneralUI with Logging{
  
  import config._
  import translation.t
  
  var title = "Sign In Page"
  private val signInButton = new JButton("Sign In")
  private val registerButton = new JButton("Register")
  private val registerUrl:String = "http://hearthstats.net/users/sign_up"
  private var image:BufferedImage = {            
     ImageIO.read(getClass.getResource("/images/icon.png"))
  }
  private val matched: Boolean = false
  private val enteredEmail: String = ""
  private val enteredPassword: String = ""
  
  def createAndShowGui() {
  debug("Creating GUI")
    val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
    setIconImage(icon)
    setLocation(windowX, windowY)
    setSize(windowWidth, windowHeight)    
    setTitle(title)
    
    
    enableMinimizeToTray()
    setVisible(true)
    
    if (enableStartMin) setState(ICONIFIED)
    protected override def paintComponent(g:Graphics): Unit = {
      super.paintComponent(g);
    g.drawImage(image, 10, 10, null); // see javadoc for more info on the parameters            
  }
  add(new JLabel("                      "),"wrap")

  
  add(new JLabel("Hearthstats Uploader"), "wrap")
  add(new JLabel(" "),"wrap")
  
  add(new JLabel(t("LandingPanel.label.userId") + " "), "")
  var userEmailField: JTextField = new StringOptionTextField(email)
  add(userEmailField, "wrap")
  userEmailField.setText(enteredEmail)
  
  
  
  add(new JLabel(t("LandingPanel.label.password") + " "), "")
  var passwordField: JTextField = new StringOptionTextField(password)
  add(passwordField, "wrap")
  passwordField.setText(enteredPassword)
  
  
  add(new JLabel(" "), "wrap")
  
  signInButton.addActionListener(new ActionListener {
    override def actionPerformed(arg0: ActionEvent) {
     checkForPassword     
    }
          
    }
  )
  add(signInButton, "")
  add(new JLabel(" "),"wrap")

 
  registerButton.addActionListener(new ActionListener {
    override def actionPerformed(arg0: ActionEvent) {
      Browse(registerUrl)
    }
  })
  add(registerButton, "")
  
  val firstTimeHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Game-Language",
   "First time tutorial")
  add(firstTimeHelpIcon, "")
    
  }

   def checkForPassword{    
    api.login(enteredEmail, enteredPassword)
   }
 

}