package net.hearthstats;

import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class PixelGroupTest {

	protected ArrayList<PixelGroup> _pixelGroups = new ArrayList<>();
	protected int _pixelGroupSize = 2;
	protected int _pixelRgbTolerence = 20;
	protected boolean _passed = true;
	
	public PixelGroupTest(BufferedImage image, int[][] testParams) {
		this(image, testParams, false);
	}
	public PixelGroupTest(BufferedImage image, int[][] testParams, boolean showDebug) {
		
		String debugStr = "";
		// loop through test parameters
		for(int testI = 0; testI < testParams.length; testI++) {
			int[] testParam = testParams[testI];
			PixelGroup pxGroup = new PixelGroup(image, testParam[0], testParam[1], _pixelGroupSize, _pixelGroupSize);
			debugStr += "{" + pxGroup.red + "," + pxGroup.blue + "," + pxGroup.green + "} ";
			int[] redRange = {testParam[2] - _pixelRgbTolerence, testParam[2] + _pixelRgbTolerence};
			int[] blueRange = {testParam[3] - _pixelRgbTolerence, testParam[3] + _pixelRgbTolerence};
			int[] greenRange = {testParam[4] - _pixelRgbTolerence, testParam[4] + _pixelRgbTolerence};
			if(!pxGroup.rbgInRange(redRange, blueRange, greenRange)) {
				_passed = false;
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
