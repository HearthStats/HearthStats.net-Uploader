package net.hearthstats;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class Pixel {

	public int red;
	public int green;
	public int blue;
	protected BufferedImage _image;
	protected int _x;
	protected int _y;
	
	/**
	 * Gets the red, blue, and green values for a given pixel
	 * 
	 * @param image
	 * @param x
	 * @param y
	 */
	public Pixel(BufferedImage image, int x, int y) {
		_image = image;
		_x = x;
		_y = y;
		int rgb = image.getRGB(x, y);
		red = (rgb >> 16) & 0xFF;
		green = (rgb >> 8) & 0xFF;
		blue = (rgb & 0xFF);
		//image.setRGB(x, y, 255);
	}
	
	public void mark() {
		int red = (int) Math.round(Math.random() * 255);
		int green = (int) Math.round(Math.random() * 255);
		int blue = (int) Math.round(Math.random() * 255);
		int color = new Color(red, green, blue).getRGB();
		_image.setRGB(_x, _y, color);
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(red);
		sb.append(',');
		sb.append(green);
		sb.append(',');
		sb.append(blue);
		return sb.toString();
	}
}
