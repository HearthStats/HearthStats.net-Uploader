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
	
	public void markPixels() {
		for(int i = 0; i < _pixels.length; i++) {
			Pixel px = _pixels[i];
			px.mark();
		}
	}
	
	/**
	 * Are the red, green, and blue values within a given range
	 * 
	 * @param redTest
	 * @param greenTest
	 * @param blueTest
	 * @return
	 */
	public boolean rgbInRange(int[] redTest, int[] greenTest, int[] blueTest) {
		return red >= redTest[0] && green >= greenTest[0] && blue >= blueTest[0] &&
			   red <= redTest[1] && green <= greenTest[1] && blue <= blueTest[1];
	}
	
	/**
	 * Are the red, green, and blue values greater than or equal to values provided?
	 * 
	 * @param redTest
	 * @param greenTest
	 * @param blueTest
	 * @return
	 */
	public boolean rgbGtEq(int redTest, int greenTest, int blueTest) {
		return red >= redTest && green >= greenTest && blue >= blueTest;
	}
	
	/**
	 * Are the red, green, green, and blue less than or equal to values provided?
	 * 
	 * @param redTest
	 * @param greenTest
	 * @param blueTest
	 * @return
	 */
	public boolean rgbLtEq(int redTest, int greenTest, int blueTest) {
		return red <= redTest && green <= greenTest && blue <= blueTest;
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
