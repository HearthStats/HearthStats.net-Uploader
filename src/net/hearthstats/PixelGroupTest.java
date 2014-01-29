package net.hearthstats;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class PixelGroupTest {

	protected ArrayList<PixelGroup> _pixelGroups = new ArrayList<>();
	protected int _pixelGroupSize = 2;
	protected int _pixelRgbTolerence = 30;
	protected boolean _passed = true;
	protected boolean _colorPixels = false;
	protected BufferedImage image;
	private float ratio;
	private int xOffset;
	private int width;
	private int height;
	private float screenRatio;
	private boolean showDebug;
	private int[][] _testParams;
	
	public PixelGroupTest(BufferedImage imageToTest, int[][] testParams) {
		this(imageToTest, testParams, false);
	}
	public PixelGroupTest(BufferedImage imageToTest, int[][] testParams, boolean showDebug) {
		image = imageToTest;
		_testParams = testParams;
		
		// handle 4:3 screen ratios
		ratio = image.getHeight() / (float) 768;
		xOffset = 0;
		width = image.getWidth();
		height = image.getHeight();
		screenRatio = (float) width / height;
		
		// handle widescreen x offsets
		if(screenRatio > 1.4) {
			xOffset = 107;
			xOffset = (int) (((float) width - (ratio * 1024)) / 2);
		}
		
	}
	
	public boolean passed() {
		boolean passed = true;
		String debugStr = "";
		// loop through test parameters
		for(int testI = 0; testI < _testParams.length; testI++) {
			int[] testParam = _testParams[testI];
			int x = (int) (testParam[0] * ratio) + xOffset;
			int y = (int) (testParam[1] * ratio);
			int size = (int) (ratio * _pixelGroupSize);
			PixelGroup pxGroup = new PixelGroup(image, x, y, size, size);
			debugStr += "[" + x + "," + y +"] {" + pxGroup.red + "," + pxGroup.blue + "," + pxGroup.green + "} ";
			int[] redRange = {testParam[2] - _pixelRgbTolerence, testParam[2] + _pixelRgbTolerence};
			int[] greenRange = {testParam[3] - _pixelRgbTolerence, testParam[3] + _pixelRgbTolerence};
			int[] blueRange = {testParam[4] - _pixelRgbTolerence, testParam[4] + _pixelRgbTolerence};
			if(!pxGroup.rgbInRange(redRange, greenRange, blueRange)) {
				passed = false;
			}
			if(_colorPixels || showDebug) {
				pxGroup.markPixels();
			}
		}
		if(showDebug) {
			System.out.println(debugStr);
		}
		return passed;
	}
	
	public boolean passedOr() {
		boolean passed = false;
		String debugStr = "";
		// loop through test parameters
		for(int testI = 0; testI < _testParams.length; testI++) {
			int[] testParam = _testParams[testI];
			int x = (int) (testParam[0] * ratio) + xOffset;
			int y = (int) (testParam[1] * ratio);
			int size = (int) (ratio * _pixelGroupSize);
			PixelGroup pxGroup = new PixelGroup(image, x, y, size, size);
			debugStr += "[" + x + "," + y +"] {" + pxGroup.red + "," + pxGroup.blue + "," + pxGroup.green + "} ";
			int[] redRange = {testParam[2] - _pixelRgbTolerence, testParam[2] + _pixelRgbTolerence};
			int[] greenRange = {testParam[3] - _pixelRgbTolerence, testParam[3] + _pixelRgbTolerence};
			int[] blueRange = {testParam[4] - _pixelRgbTolerence, testParam[4] + _pixelRgbTolerence};
			if(pxGroup.rgbInRange(redRange, greenRange, blueRange)) {
				passed = true;
			}
			if(_colorPixels || showDebug) {
				pxGroup.markPixels();
			}
		}
		if(showDebug) {
			System.out.println(debugStr);
		}
		return passed;
	}

}
