package net.hearthstats;

import java.awt.image.BufferedImage;

import com.sun.jna.platform.win32.WinUser.BLENDFUNCTION;

public class PixelGroup {

	protected Pixel[] _pixels;
	protected int red = 0;
	protected int green = 0;
	protected int blue = 0;
	
	/**
	 * Gets the average red, blue, and green values for a group of pixels
	 * 
	 * @param image
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	public PixelGroup(BufferedImage image, int x,  int y, int width, int height) {
		_pixels = new Pixel[width * height];
		
		int i = 0;
		for(int xOffset = 0; xOffset < width; xOffset++) {
			for(int yOffset = 0; yOffset < height; yOffset++) {
				_pixels[i] = new Pixel(image, x + xOffset, y + yOffset);
				i++;
			}
		}
		
		for(i = 0; i < _pixels.length; i++) {
			Pixel px = _pixels[i];
			red += px.red;
			green += px.green;
			blue += px.blue;
		}
		
		red = Math.round(red / _pixels.length);
		green = Math.round(green / _pixels.length);
		blue = Math.round(blue / _pixels.length);
		
	}
	
	/**
	 * Are the red, blue, and green values greater than or equal to values provided?
	 * 
	 * @param redTest
	 * @param blueTest
	 * @param greenTest
	 * @return
	 */
	public boolean rbgGtEq(int redTest, int blueTest, int greenTest) {
		return red >= redTest && blue >= blueTest && green >= greenTest;
	}
	
	/**
	 * Are the red, blue, and green values less than or equal to values provided?
	 * 
	 * @param redTest
	 * @param blueTest
	 * @param greenTest
	 * @return
	 */
	public boolean rbgLtEq(int redTest, int blueTest, int greenTest) {
		return red <= redTest && blue <= blueTest && green <= greenTest;
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
