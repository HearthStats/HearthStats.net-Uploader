package net.hearthstats;

import java.awt.image.BufferedImage;

public class Pixel {

	public int red;
	public int green;
	public int blue;
	
	/**
	 * Gets the red, blue, and green values for a given pixel
	 * 
	 * @param image
	 * @param x
	 * @param y
	 */
	public Pixel(BufferedImage image, int x, int y) {
		int rgb = image.getRGB(x, y);
		red = (rgb >> 16) & 0xFF;
		green = (rgb >> 8) & 0xFF;
		blue = (rgb & 0xFF);
		//image.setRGB(x, y, 255);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(red);
		sb.append(',');
		sb.append(blue);
		sb.append(',');
		sb.append(green);
		return sb.toString();
	}
}
