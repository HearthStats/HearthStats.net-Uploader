package net.hearthstats;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class PixelGroupTest {

    private final static Logger debugLog = LoggerFactory.getLogger(PixelGroupTest.class);

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
        debugLog.debug("    Test: {}", testParams);

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
        StringBuilder debugSb = new StringBuilder();
		// loop through test parameters
		for (int testI = 0; testI < _testParams.length; testI++) {
			int[] testParam = _testParams[testI];
			int x = (int) (testParam[0] * ratio) + xOffset;
			int y = (int) (testParam[1] * ratio);
			int size = (int) (ratio * _pixelGroupSize);
			PixelGroup pxGroup = new PixelGroup(image, x, y, size, size);
            if (debugLog.isDebugEnabled()) {
                debugSb.append("[").append(x).append(",").append(y).append("] {").append(pxGroup.red).append(",").append(pxGroup.blue).append(",").append(pxGroup.green).append("} ");
            }
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
        if (debugLog.isDebugEnabled()) {
            debugSb.insert(0, "    Found ");
            debugSb.append(" passed=");
            debugSb.append(passed);
            debugLog.debug(debugSb.toString());
        }
		return passed;
	}
	
	public boolean passedOr() {
		boolean passed = false;
        StringBuilder debugSb = new StringBuilder();
		// loop through test parameters
		for (int testI = 0; testI < _testParams.length; testI++) {
			int[] testParam = _testParams[testI];
			int x = (int) (testParam[0] * ratio) + xOffset;
			int y = (int) (testParam[1] * ratio);
			int size = (int) (ratio * _pixelGroupSize);
			PixelGroup pxGroup = new PixelGroup(image, x, y, size, size);
            if (debugLog.isDebugEnabled()) {
                debugSb.append("[").append(x).append(",").append(y).append("] {").append(pxGroup.red).append(",").append(pxGroup.blue).append(",").append(pxGroup.green).append("} ");
            }
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
        if (debugLog.isDebugEnabled()) {
            debugSb.insert(0, "    Found ");
            debugSb.append(" passed=");
            debugSb.append(passed);
            debugLog.debug(debugSb.toString());
        }
		return passed;
	}

}
