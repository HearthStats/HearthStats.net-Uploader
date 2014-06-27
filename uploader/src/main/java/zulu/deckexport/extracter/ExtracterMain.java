/**
 * @author shyos
 */
package zulu.deckexport.extracter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import zulu.deckexport.card.Card;
import zulu.deckexport.card.CardCount;
import zulu.deckexport.card.Deck;
import zulu.deckexport.card.DeckItem;
import zulu.deckexport.prob.ProbList;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class ExtracterMain {
	private static ArrayList<Card> cards;
	private static Map<String, Card> cardMap;
	private static Map<Integer, Card> cardIdMap;
	private static BufferedImage image;
	private static int numberOfCardInDeck = 21;
	private static ArrayList<CardCount> cardCounts;
	private static ArrayList<ProbList> probList;
	public static void main(String[] args) throws IOException{
		
		BufferedImage img = ImageIO.read(new File("src/test/resources/images/deckexportexamples/part1.png"));
		BufferedImage imgScroll = ImageIO.read(new File("src/test/resources/images/deckexportexamples/part2.png"));
		Deck deck  = exportDeck(img, imgScroll);
		for(DeckItem dI : deck.getCards())
			System.out.println(dI.getCard().getName() + " x" + dI.getCount());
		
	}
	
	/**
	 * Extracts Cards Image from Deck Image and converts into Deck Object.<br /><br />
	 * 
	 * <h3>How to use?</h3>
	 * Take two screenshots of hearthstone deck page. 'img' for normal and 'imgScroll' for scrolled part of the deck
	 * and then call this method.<br/>
	 * 
	 * @param deckName - name of the deck
	 * @param img - first part of the deck image
	 * @param imgScroll - second part of the deck image (Scrolled Deck Image)
	 * @return Deck - null if it is not a logical deck.
	 */
	public static Deck exportDeck(BufferedImage img, BufferedImage imgScroll)
	{
		BufferedImage img1 = PixelManager.rescaleImage(img);
		buildEnvironment();
		ArrayList<DeckItem> deckItems = new ArrayList<DeckItem>();
		probList = new ArrayList<ProbList>();
		deckItems = fetchCards(img1);
		boolean isScrollable = true;
		if(isScrollable)
		{
			ArrayList<DeckItem> scrolledDeckItems = new ArrayList<DeckItem>();
			BufferedImage img2 = PixelManager.rescaleImage(imgScroll);
			PixelManager.setPixelManager();
			scrolledDeckItems = fetchCards(img2);
			deckItems = LogicalDeckAlgorithm.mergeDeckParts(deckItems, scrolledDeckItems, probList);
		}
		if(deckItems != null)
			return new Deck(deckItems);
		else return null;	
	}
	
	/**
	 * <ul>
	 * <li>Sets pixel manager</li>
	 * <li>Reads Cards</li>
	 * <li>Reads Card Counts (2,3,4 available)</li>
	 * </ul>
	 */
	public static void buildEnvironment()
	{
		//Sets PixelManager with resolution
		PixelManager.setPixelManager();
		//Read card list
		readCards();	
		//Read card count list
		readCardCounts();
	}
	
	
	// Deck Export
	private static ArrayList<DeckItem> fetchCards(BufferedImage image) {
		
		ArrayList<DeckItem> deckItems = new ArrayList<DeckItem>();
		int manaFlag = 0;
		for(int i=0;i<numberOfCardInDeck;i++)
		{
			CropManager.cropImage(i, image);
			DeckItem deckItem = matchCards(CropManager.subImage, CropManager.countImage, manaFlag);
			if(!deckItem.getCard().getName().equals("UNKNOWN"))
			{
				//System.out.println((i+1) + "/"+ numberOfCardInDeck + " - " + deckItem.toString());
				deckItem.setImage(CropManager.deckItemImage);
				deckItems.add(deckItem);
			}
			else
			{
				System.out.println("This card does not have any trained data.");
				deckItems.add(deckItem);
			}
			manaFlag = deckItem.getCard().getMana();
		}	
		return deckItems;
	}
	
	// Finds similar of the card and cardcount
	private static DeckItem matchCards(BufferedImage image, BufferedImage countImage, int manaFlag) {
		DeckItem deckItem = new DeckItem(findSimilarCard(image, manaFlag));
		deckItem.setCount(findSimilarCount(countImage));
		return deckItem;
	}
	
	// Compares current card with others (Algortihm included)
	private static Card findSimilarCard(BufferedImage img, int manaFlag)
	{
		return findSimilarCardWithIndex(img, 4, manaFlag);
	}
	
	// Compares current card count with others (Algortihm included)
	private static int findSimilarCount(BufferedImage img)
	{
		return findSimilarCountWithIndex(img, 4);
	}
	
	// Sometimes images position changes few lines, to overcome this also check those coordinates.
	private static Card findSimilarCardWithIndex(BufferedImage img1,int lineIndex, int manaFlag) {
	    int width1 = img1.getWidth(null);
	    int height1 = img1.getHeight(null);

	    double maxDiff = 100;
	    double diffPercent = 100;
	    ProbList pL = new ProbList();
	    Card returnCard = new Card("UNKNOWN");
	    for(int h = 0; h<=lineIndex; h++)
	    {
		    int[][] imgRGB = new int [height1][width1];
		    for (int i = h; i < height1; i++) {
			      for (int j = 0; j < width1; j++) {  
					imgRGB[i-h][j] = img1.getRGB(j, i);
			      }
			 }
		
		    for(Card card : cards)
		    {
		    	if(card.getHash()!=null) //&& card.getMana() >= manaFlag manaFlag condition removed
		    	{
				    long diff = 0;
				    
				    int[][] cardRGB = card.getHash();
				    for (int i = 0; i < height1; i++) {
				      for (int j = 0; j < width1; j++) {
				        int rgb1 = imgRGB[i][j];
				        int rgb2 = cardRGB[i][j];
				        int r1 = (rgb1 >> 16) & 0xff;
				        int g1 = (rgb1 >>  8) & 0xff;
				        int b1 = (rgb1      ) & 0xff;
				        int r2 = (rgb2 >> 16) & 0xff;
				        int g2 = (rgb2 >>  8) & 0xff;
				        int b2 = (rgb2      ) & 0xff;
				        diff += Math.abs(r1 - r2);
				        diff += Math.abs(g1 - g2);
				        diff += Math.abs(b1 - b2);
				      }
				    }
				    double n = width1 * height1 * 3;
				    double p = diff / n / 255.0;
				    diffPercent = (p * 100.0);
				    if(diffPercent < maxDiff)
				    {
				    	maxDiff = diffPercent;
				    	returnCard = card;
				    	//System.out.println("Level: " + (h+1) + " Possible: " + card.getName() + " Similarity: " + diffPercent);
				    }
				    if(diffPercent < pL.getProbMax())
				    {
				    	pL.add(card, diffPercent);
				    }
				    if(diffPercent < 2) break;
		    	}
		    	if(diffPercent < 2) break;
		    }
		    if(diffPercent < 2) break;
	    }
	    probList.add(pL);
	   // System.out.println(returnCard.getName());
	    return returnCard;
	}
	
	// Sometimes images position changes few lines, to overcome this also check those coordinates (for the card counts).
	private static int findSimilarCountWithIndex(BufferedImage img1,int lineIndex) {
	    int width1 = img1.getWidth(null);
	    int height1 = img1.getHeight(null);

	    double maxDiff = 100;
	    double diffPercent = 100;
	    int returnCount = 1;
	    for(int h = 0; h<=lineIndex; h++)
	    {
		    int[][] imgRGB = new int [height1][width1];
		    for (int i = h; i < height1; i++) {
			      for (int j = 0; j < width1; j++) {  
					imgRGB[i-h][j] = img1.getRGB(j, i);
			      }
			 }
		
		    for(CardCount cc : cardCounts)
		    {
		    	if(cc.getHash()!=null)
		    	{
				    long diff = 0;
				    
				    int[][] cardRGB = cc.getHash();
				    for (int i = 0; i < height1; i++) {
				      for (int j = 0; j < width1; j++) {
				        int rgb1 = imgRGB[i][j];
				        int rgb2 = cardRGB[i][j];
				        int r1 = (rgb1 >> 16) & 0xff;
				        int g1 = (rgb1 >>  8) & 0xff;
				        int b1 = (rgb1      ) & 0xff;
				        int r2 = (rgb2 >> 16) & 0xff;
				        int g2 = (rgb2 >>  8) & 0xff;
				        int b2 = (rgb2      ) & 0xff;
				        diff += Math.abs(r1 - r2);
				        diff += Math.abs(g1 - g2);
				        diff += Math.abs(b1 - b2);
				      }
				    }
				    double n = width1 * height1 * 3;
				    double p = diff / n / 255.0;
				    diffPercent = (p * 100.0);
				    //System.out.println("diff percent: " + diffPercent);
				    if(diffPercent < maxDiff)
				    {
				    	maxDiff = diffPercent;
				    	returnCount = cc.getCount();
				    //	System.out.println("Level: " + (h+1) + " Possible: " + cc.getDescription() + " Similarity: " + diffPercent);
				    }
				    if(maxDiff < 2) break;
		    	}
		    	if(maxDiff < 2) break;
		    }
		    if(maxDiff < 2) break;
	    }
	    if(maxDiff < 7.5)
	    	return returnCount;
	    else 
	    	return 1;
	}


	// Used by TrainingAPP GUI to fetch card images
	public static void getCardImage(int k)
	{
		CropManager.cropImage(k, image);
	}
	
	
	//##################################################
	//
	//Read card counts list from txt
	private static void readCardCounts() {
		String guicardsText = readFromResourceFile(Constants.txtCardCounts);
		Type mapType = new TypeToken<List<CardCount>>(){}.getType(); 
		cardCounts =  new Gson().fromJson(guicardsText, mapType);
	}

	//Read card list from txt, build maps
	private static void readCards()
	{
		String cardsText = readFromResourceFile(Constants.txtCards);
		Type mapType = new TypeToken<List<Card>>(){}.getType(); 
		cards = new Gson().fromJson(cardsText, mapType);
		
		cardMap = new HashMap<String, Card>();
		cardIdMap = new HashMap<Integer, Card>();
		for(Card card : cards)
		{
			cardMap.put(card.getName(), card);
			cardIdMap.put(card.getHearthhead_id(), card);
		}
	}
	
	// Reads cards from resource txt
	public static String readFromResourceFile(String filename) {
			 
		String cardsText = "";
		try {
			cardsText = IOUtils.toString(
				      ExtracterMain.class.getResourceAsStream(filename),
				      "UTF-8"
				    );
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 

		return cardsText;
	}

}
