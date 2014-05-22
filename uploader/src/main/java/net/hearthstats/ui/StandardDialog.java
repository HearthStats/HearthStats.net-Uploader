/**
 * Based on http://harryjoy.com/2011/07/01/create-new-message-notification-pop-up-in-java/
 */

package net.hearthstats.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;

/**
 * Same layout for deck overlay than for notification popup.
 */
// TODO share some code
public class StandardDialog {

	public JDialog frame = new JDialog();

	public StandardDialog(String header, Component content, boolean allowFocus) {
		frame.setFont(new Font("Arial", Font.PLAIN, 14));
		frame.setLayout(new GridBagLayout());
		frame.setBackground(Color.LIGHT_GRAY);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0f;
		constraints.weighty = 0f;
		constraints.insets = new Insets(0, 5, 0, 0);
		constraints.fill = GridBagConstraints.BOTH;

		frame.setTitle(header);

		frame.setAlwaysOnTop(true);
		frame.setFocusableWindowState(true);

		JComponent frameComponent = ((JComponent) frame.getContentPane());
		frameComponent.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1,
				Color.black));
		constraints.gridx = 0;
		constraints.gridy++;
		constraints.weightx = 1.0f;
		constraints.weighty = 1.0f;
		constraints.insets = new Insets(5, 5, 5, 5);
		constraints.fill = GridBagConstraints.BOTH;

		frame.add(content, constraints);
	}

	public void show() {
		frame.pack();
		frame.setVisible(true);
	}

	public void close() {
		frame.dispose();
	}

}
