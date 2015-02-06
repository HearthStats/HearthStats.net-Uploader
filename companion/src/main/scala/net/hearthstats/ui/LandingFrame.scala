package net.hearthstats.ui

import net.hearthstats.util.{Browse, Translation}
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log
import net.hearthstats.config.{Environment, UserConfig}
import net.hearthstats.ui.util.OptionTextField
import net.hearthstats.ui.util.StringOptionTextField
import net.hearthstats.ui.notification.NotificationQueue

import java.awt.event.{ ActionEvent, ActionListener }
import java.awt.{ Dimension, Font, Graphics, Color, Cursor }
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
      val firstTimeHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Game-Language",
       "First time tutorial")
      add(firstTimeHelpIcon, "left")
    
    }
    
    //update title
    def updateTitle
    {
      var title = "HearthStats Companion Login Page"
      setTitle(title)
    }  
    
    //close login page
    def closeLandingPage
    {
      config.closedLandingPage.set(true)
    }
    
    //check if password and email matched
    def checkForPassword{ 
      try{      
      if (api.login(config.email.get, config.password.get)){        
        closeLandingPage
        JOptionPane.showMessageDialog(null,"Password correct")
        }      
      else{
        JOptionPane.showMessageDialog(null,"Invalid Email or Password")
        println("invalid password or email")
        }
      }
      catch {
        case e => throw new Exception("entered nothing", e) 
        }
  
    }   
}


