/**
 * @author shyos
 */
package zulu.deckexport.extracter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import zulu.deckexport.util.Myrect;

public class PixelManager {	
	private static int x_FirstCard;			// X of first card
	private static int y_FirstCard;			// Y of first card
	
	private static int x_Mana;				// X of Mana Segment
	private static int x_Text;				// X of Text Segment
	private static int x_Image;				// X of Image Segment
	private static int x_Count;				// X of Count Segment
	
	private static int w_Mana;				// W of Mana Segment
	private static int w_Text;				// W of Text Segment
	private static int w_Image;				// W of Image Segment
	private static int w_Count;				// W of Count Segment
	private static int w_Full;				// W of Full Image for widgets
	
	private static int cropHeight;			// Height of the parts that are cropped from both top and bottom of a card
	private static int heightOfACard;		// Height of a single card without crop
	
	private static int gap;					// Gap between two cards
	
	// Varialbes used by RobotManager
	private static int x_Search;			// X of Search
	private static int y_Search;			// Y of Search
	private static int x_SearchedCard;		// X of Card
	private static int y_SearchedCard;		// Y of Card
	private static int x_Scroll;			// X of Deck Scroller
	private static int y_Scroll;			// Y of Deck Scroller
	public static double ratio;
	public static int sideCrop;
	
	public static void setPixelManager(double ratio2, int sideCrop2) {
		ratio = ratio2;
		sideCrop = sideCrop2;
		initWithResolution();
	}

	public static void setPixelManager(){
		initWithResolution();
	}
	
	// Fetches coordinates according to resolution
	private static void initWithResolution() {
			x_FirstCard = (int) (800);
			y_FirstCard = (int) (80);
			
			x_Mana = (int) (800);
			x_Text = (int) (821);
			x_Image = (int) (895);
			x_Count = (int) (950);
			
			w_Mana = (int) (15);
			w_Text = (int) (90);
			w_Image = (int) (50);
			w_Count = (int) (12);
			w_Full = (int) (140);
			
			gap = 1;
			heightOfACard = 28;
			cropHeight = 7;	
			
			x_Search = (int) (sideCrop + 480*ratio);
			y_Search = (int) (722*ratio);
			x_SearchedCard = (int) (sideCrop + 120*ratio);
			y_SearchedCard = (int) (250*ratio);
			x_Scroll = (int)(sideCrop + 1000*ratio);
			y_Scroll = (int) (100*ratio);
		
	}

	
	/**
	 * Converts image into 1024x768 version
	 * @param tempImage
	 * @return
	 */
    static BufferedImage rescaleImage(BufferedImage tempImage) {
    	
    	int gh = 768;		// Global initial height
    	int gw = 1024;		// Global initial width
    	
    	int w = tempImage.getWidth();
		int h = tempImage.getHeight();
		
		double expectedW = ((double)h/gh)*gw;
		int expW = (int) expectedW;
		int side = (w - expW)/2;
		
		ratio = expectedW/gw;
		sideCrop = side;
		
		int type = tempImage.getType() == 0? BufferedImage.TYPE_INT_ARGB : tempImage.getType();
		BufferedImage resizedImage = new BufferedImage(gw, gh, type);
		Graphics2D g = resizedImage.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		g.drawImage(tempImage.getSubimage(sideCrop, 0, expW, h), 0, 0, gw, gh, null);
		g.dispose();

		return resizedImage;
	}
    
	public static Myrect getMana() {
		return new Myrect(x_Mana, getBaseY(), w_Mana, getBaseH());
	}

	public static Myrect getText() {

		return new Myrect(x_Text, getBaseY(), w_Text, getBaseH());
	}

	public static Myrect getImage() {
		return new Myrect(x_Image, getBaseY(), w_Image, getBaseH());
	}
	
	public static Myrect getCount() {
		return new Myrect(x_Count, getBaseY(), w_Count, getBaseH());
	}

	public static Myrect getFull() {
		return new Myrect(x_FirstCard, y_FirstCard, w_Full, heightOfACard);
	}
	
	public static int getBaseY() {
		return y_FirstCard + cropHeight;
	}
	
	public static int getBaseH(){
		return heightOfACard - 2*cropHeight;
	}

	public static int getHeightOfACard() {
		return heightOfACard;
	}

	public static int getGap() {
		return gap;
	}

	public static int getRgbH() {
		return heightOfACard - 2*cropHeight;
	}

	public static int getRgbW() {
		return w_Text;
	}

	public static int getX_Search() {
		return x_Search;
	}

	public static void setX_Search(int x_Search) {
		PixelManager.x_Search = x_Search;
	}

	public static int getY_Search() {
		return y_Search;
	}

	public static void setY_Search(int y_Search) {
		PixelManager.y_Search = y_Search;
	}

	public static int getX_SearchedCard() {
		return x_SearchedCard;
	}

	public static void setX_SearchedCard(int x_SearchedCard) {
		PixelManager.x_SearchedCard = x_SearchedCard;
	}

	public static int getY_SearchedCard() {
		return y_SearchedCard;
	}

	public static void setY_SearchedCard(int y_SearchedCard) {
		PixelManager.y_SearchedCard = y_SearchedCard;
	}

	public static int getX_Scroll() {
		return x_Scroll;
	}

	public static void setX_Scroll(int x_Scroll) {
		PixelManager.x_Scroll = x_Scroll;
	}

	public static int getY_Scroll() {
		return y_Scroll;
	}

	public static void setY_Scroll(int y_Scroll) {
		PixelManager.y_Scroll = y_Scroll;
	}

	public static int getW_Full() {
		return w_Full;
	}

	public static void setW_Full(int w_Full) {
		PixelManager.w_Full = w_Full;
	}

	public static int getCropHeight() {
		return cropHeight;
	}

	public static void setCropHeight(int cropHeight) {
		PixelManager.cropHeight = cropHeight;
	}






}
