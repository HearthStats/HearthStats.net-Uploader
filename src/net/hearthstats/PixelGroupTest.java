package net.hearthstats;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class PixelGroupTest {

	protected ArrayList<PixelGroup> _pixelGroups = new ArrayList<>();
	protected int _pixelGroupSize = 2;
	protected int _pixelRgbTolerence = 30;
	protected boolean _passed = true;
	protected boolean _colorPixels = true;
	
	public PixelGroupTest(BufferedImage image, int[][] testParams) {
		this(image, testParams, false);
	}
	public PixelGroupTest(BufferedImage image, int[][] testParams, boolean showDebug) {
		
		// handle 4:3 screen ratios
		float ratio = image.getHeight() / (float) 768;
		int xOffset = 0;
		int width = image.getWidth();
		int height = image.getHeight();
		float screenRatio = (float) width / height;
		
		// handle widescreen x offsets
		if(screenRatio > 1.4) {
			xOffset = 107;
			xOffset = (int) (((float) width - (ratio * 1024)) / 2);
		}
		//System.out.println(screenRatio);
		
		String debugStr = "";
		// loop through test parameters
		for(int testI = 0; testI < testParams.length; testI++) {
			int[] testParam = testParams[testI];
			int x = (int) (testParam[0] * ratio) + xOffset;
			int y = (int) (testParam[1] * ratio);
			int size = (int) (ratio * _pixelGroupSize);
			PixelGroup pxGroup = new PixelGroup(image, x, y, size, size);
			debugStr += "[" + x + "," + y +"] {" + pxGroup.red + "," + pxGroup.blue + "," + pxGroup.green + "} ";
			int[] redRange = {testParam[2] - _pixelRgbTolerence, testParam[2] + _pixelRgbTolerence};
			int[] blueRange = {testParam[3] - _pixelRgbTolerence, testParam[3] + _pixelRgbTolerence};
			int[] greenRange = {testParam[4] - _pixelRgbTolerence, testParam[4] + _pixelRgbTolerence};
			if(!pxGroup.rbgInRange(redRange, blueRange, greenRange)) {
				_passed = false;
			}
			if(_colorPixels || showDebug) {
				pxGroup.markPixels();
			}
		}
		if(showDebug) {
			System.out.println(debugStr);
		}
	}
	
	public boolean passed() {
		return _passed;
	}

}
