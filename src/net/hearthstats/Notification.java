/**
 * Based on http://harryjoy.com/2011/07/01/create-new-message-notification-pop-up-in-java/
 */

package net.hearthstats;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class Notification {

	public JDialog frame = new JDialog();
	public Notification(String header, String message) {
		frame.setSize(250, message == "" ? 40 : 100);
		frame.setFont(new Font("Arial",Font.PLAIN,14));
		frame.setLayout(new GridBagLayout());
		frame.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0f;
		constraints.weighty = 0f;
		constraints.insets = new Insets(0, 5, 0, 0);
		constraints.fill = GridBagConstraints.BOTH;
		
		// header
		JLabel headingLabel = new JLabel(header);
		ImageIcon headingIcon = new ImageIcon(getClass().getResource("/images/icon_16px.png"));
		headingLabel.setIcon(headingIcon); // --- use image icon you want to
		headingLabel.setOpaque(false);
		frame.add(headingLabel, constraints);
		constraints.gridx++;
		constraints.weightx = 0f;
		constraints.weighty = 0f;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.NORTH;
		
		// close button
		JButton closeButton = new JButton(new AbstractAction("x") {
	        @Override
	        public void actionPerformed(final ActionEvent e) {
	               frame.dispose();
	        }
		});
		closeButton.setMargin(new Insets(0, 4, 1, 4));
		closeButton.setFocusable(false);
		constraints.insets = new Insets(5, 5, 5, 5);
		frame.add(closeButton, constraints);
		
		frame.setAlwaysOnTop(true);
		frame.setUndecorated(true);
		frame.setFocusable(false);
		
		JComponent frameComponent = ((JComponent) frame.getContentPane());
		frameComponent.setBorder(BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.black ));
		constraints.gridx = 0;
		constraints.gridy++;
		constraints.weightx = 1.0f;
		constraints.weighty = 1.0f;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.fill = GridBagConstraints.BOTH;
		
		// message
		JEditorPane messageText = new JEditorPane();
		messageText.setContentType("text/html");
		messageText.setEditable(false);
		messageText.setBackground(null);
		messageText.setText("<html><body style=\"font-family:arial,sans-serif; font-size:10px;\">" + message + "</html>");
		messageText.addHyperlinkListener(HyperLinkHandler.getInstance());
		frame.add(messageText, constraints);
		offset(0);
	}
	protected boolean _wasShown = false;
	public boolean wasShown() {
		return _wasShown;
	}
	protected int _offset = 0;
	public void offset(int offset) {
		
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		Rectangle bounds = env.getMaximumWindowBounds();
		
		int x = bounds.width - frame.getWidth() - 5;
		int y = bounds.height - frame.getHeight() - 5 - (frame.getHeight() + 5) * offset;
		frame.setLocation(x, y);
	}
	public void show() {
		frame.setVisible(true);
		_wasShown = true;
	}
	public void close() {
		frame.dispose();
	}

}
