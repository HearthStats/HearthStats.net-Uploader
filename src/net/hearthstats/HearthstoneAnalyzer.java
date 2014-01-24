package net.hearthstats;

import java.awt.image.BufferedImage;

public class HearthstoneAnalyzer {

	protected BufferedImage _image;
	protected String _mode;

	public HearthstoneAnalyzer() {
		
	}
	
	public void analyze(BufferedImage image) {
		_image = image;
	}
	
	public String getMode() {
		return _mode;
	}

}
