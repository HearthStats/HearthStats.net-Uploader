package net.hearthstats.ui

import java.awt.event.{ ActionEvent, ActionListener }
import java.awt.{ Dimension, Font, Graphics }
import javax.swing._
import net.hearthstats.config._
import net.hearthstats.hstatsapi.API
import net.hearthstats.ui.log.Log
import net.hearthstats.ui.util.OptionTextField
import net.hearthstats.ui.util.StringOptionTextField
import net.hearthstats.util.{Browse, Translation}
import java.awt.image.BufferedImage;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;


class LandingPanel(translation: Translation, uiLog: Log,config: UserConfig, api:API) extends JPanel {
  import translation.t
  import config._

  private val signInButton = new JButton("Sign In")
  private val registerButton = new JButton("Register")
  private val registerUrl:String = "http://hearthstats.net/users/sign_up"
  private var image:BufferedImage = {            
       ImageIO.read(getClass.getResource("/images/icon.png"))
  }

  
 
  protected override def paintComponent(g:Graphics): Unit = {
      super.paintComponent(g);
      g.drawImage(image, 10, 10, null); // see javadoc for more info on the parameters            
  }
  
  add(new JLabel("Hearthstats Uploader"), "skip,wrap,span")
  add(new JLabel(""),"skip,wrap,span")
  
  add(new JLabel(t("LandingPanel.label.userId") + " "), "")
  var userEmailField: JTextField = new StringOptionTextField(email)
  add(userEmailField, "wrap")
  private val enteredEmail:String = userEmailField.getText()
  config.email.set(enteredEmail)
  
  
  add(new JLabel(t("LandingPanel.label.password") + " "), "")
  var passwordField: JTextField = new StringOptionTextField(password)
  add(passwordField, "wrap")
  private val enteredPassword = passwordField.getText()
  config.password.set(enteredPassword)
  
  
  add(new JLabel(" "), "wrap")
  
  signInButton.addActionListener(new ActionListener {
    override def actionPerformed(arg0: ActionEvent) {
     checkForPassword   
    }})
  add(signInButton, "")
  add(new JLabel(" "),"wrap")

 
  registerButton.addActionListener(new ActionListener {
    override def actionPerformed(arg0: ActionEvent) {
      Browse(registerUrl)
    }})
  add(registerButton, "")
  
  
  val firstTimeHelpIcon = new HelpIcon("https://github.com/HearthStats/HearthStats.net-Uploader/wiki/Options:-Game-Language",
   "First time tutorial")
  add(firstTimeHelpIcon, "")
 

   def checkForPassword{    
    if (api.login(enteredEmail, enteredPassword)){
      add(new JLabel("password corrected"))}      
    else{
      add(new JLabel("Email or password is incorrect"))
      
      
    }
  
  }
}