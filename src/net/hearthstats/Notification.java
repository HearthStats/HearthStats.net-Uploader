/**
 * Based on http://harryjoy.com/2011/07/01/create-new-message-notification-pop-up-in-java/
 */

package net.hearthstats;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

public class Notification {

	public JFrame frame = new JFrame();
	public Notification(String header, String message) {
		
		frame.setSize(200, 75);
		frame.setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0f;
		constraints.weighty = 1.0f;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.fill = GridBagConstraints.BOTH;
		JLabel headingLabel = new JLabel(header);
		ImageIcon headingIcon = new ImageIcon("images/icon_16px.png");
		headingLabel.setIcon(headingIcon); // --- use image icon you want to
		headingLabel.setOpaque(false);
		frame.add(headingLabel, constraints);
		constraints.gridx++;
		constraints.weightx = 0f;
		constraints.weighty = 0f;
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.NORTH;
		frame.setAlwaysOnTop(true);
		frame.setUndecorated(true);
		JComponent frameComponent = ((JComponent) frame.getContentPane());
		frameComponent.setBorder(BorderFactory.createMatteBorder( 1, 1, 1, 1, Color.black ));
		constraints.gridx = 0;
		constraints.gridy++;
		constraints.weightx = 1.0f;
		constraints.weighty = 1.0f;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.fill = GridBagConstraints.BOTH;
		JLabel messageLabel = new JLabel("<HtMl>" + message);
		frame.add(messageLabel, constraints);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		offset(0);
	}
	protected boolean _wasShown = false;
	public boolean wasShown() {
		return _wasShown;
	}
	protected int _offset = 0;
	public void offset(int offset) {
		_offset = offset;
		Dimension scrSize = Toolkit.getDefaultToolkit().getScreenSize();// size of the screen
		Insets toolHeight = Toolkit.getDefaultToolkit().getScreenInsets(frame.getGraphicsConfiguration());// height of the task bar
		int x = scrSize.width - frame.getWidth() - 100;
		int y = scrSize.height - toolHeight.bottom - frame.getHeight() - 75 - (frame.getHeight() + 5) * offset;
		frame.setLocation(x, y);
	}
	public int getOffset() {
		return _offset;
	}
	public void show() {
		frame.setVisible(true);
		_wasShown = true;
	}
	public void close() {
		frame.dispose();
	}

}
