package net.hearthstats.ui

import net.hearthstats.util.{Browse, Translation}
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log
import net.hearthstats.config.{Environment, UserConfig}
import net.hearthstats.ui.util.OptionTextField
import net.hearthstats.ui.util.StringOptionTextField
import net.hearthstats.ui.notification.NotificationQueue

import java.awt.event.{ ActionEvent, ActionListener, WindowEvent }
import java.awt.{ Dimension, Font, Graphics, Color }
import java.io.File
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing._
import net.miginfocom.swing.MigLayout


class LandingFrame(translation: Translation,
    uilog: Log,
    config: UserConfig,
    api: API,
    val notificationQueue: NotificationQueue,
    val environment: Environment)extends JFrame{
  import translation.t
  import config._
    
    val closed = false
    private val signInButton = new JButton("Sign In")
    private val registerButton = new JButton("Register")
    private val registerUrl:String = "http://hearthstats.net/users/sign_up"
    private val enteredEmail:String = null
    private val enteredPassword:String = null
   

    def createLandingPage(){
      val icon = new ImageIcon(getClass.getResource("/images/icon.png")).getImage
      val titleLabel = new JLabel("Hearthstats Uploader")
      titleLabel.setFont(new Font("Ariel",Font.BOLD,24))
      setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE)
      setIconImage(icon)
      setLocation(windowX, windowY)
      setSize(windowWidth, windowHeight)
      setMinimumSize(new Dimension(500, 600))
      setVisible(true)
      updateTitle
      
      setLayout(new MigLayout)
      setBackground(Color.WHITE)
      
      add(new JLabel(" "), "wrap")      
      add(new JLabel(new ImageIcon(ImageIO.read(getClass.getResource("/images/icon.png")))))
      
      add(titleLabel, "wrap")
      add(new JLabel(" "),"wrap")
        
      //entering userID
      add(new JLabel(t("LandingPanel.label.userId") + " "), "")
      var userEmailField: JTextField = new StringOptionTextField(config.email)
      add(userEmailField, "wrap")
      val enteredEmail = userEmailField.getText()
      config.email.set(enteredEmail)
        
      //entering user Password  
      add(new JLabel(t("LandingPanel.label.password") + " "), "")
      var passwordField: JTextField = new StringOptionTextField(config.password)
      add(passwordField, "wrap")
      val enteredPassword = passwordField.getText()
      config.password.set(enteredPassword)
        
        
      add(new JLabel(" "), "wrap")
        
      //add a sign in button
      signInButton.addActionListener(new ActionListener {
        override def actionPerformed(arg0: ActionEvent) {
         checkForPassword   
        }})
      add(signInButton, "")
      
      //add a register button 
      registerButton.addActionListener(new ActionListener {
        override def actionPerformed(arg0: ActionEvent) {
          Browse(registerUrl)
        }})
      add(registerButton, "")
        
      //add a first time help link  
      val firstTimeHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Game-Language",
       "First time tutorial")
      add(firstTimeHelpIcon, "")
    
    }
    
    //update title
    def updateTitle
    {
      var title = "HearthStats Companion"
      setTitle(title)
    }  
    
    def closeLandingPage
    {
      val closed = true
      this.setVisible(false)
      dispose()
    }
    
    //check if password and email matched
    def checkForPassword() {    
      if (api.login(enteredEmail, enteredPassword)){
        closeLandingPage
        }      
      else{
        add(new JLabel("Email or password is incorrect")) 
        }
  
    }   
}