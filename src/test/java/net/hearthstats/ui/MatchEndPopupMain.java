package net.hearthstats.ui;

import javax.swing.JFrame;

import net.hearthstats.HearthstoneMatch;

public class MatchEndPopupMain {
	public static void main(String[] args) {
		MatchEndPopup.showPopup(new JFrame(), new HearthstoneMatch(), "msg",
				"title");
		System.exit(0);
	}
}
