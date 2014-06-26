/**
 * @author shyos
 */
package zulu.deckexport.extracter;

import java.awt.image.BufferedImage;

import zulu.deckexport.card.DeckItemImage;

public class CropManager {

	public static BufferedImage subImage;		// Text Image of a DeckItem
	public static BufferedImage countImage;		// Count Image of a DeckItem
	public static BufferedImage deckItemImage;
	public static void cropImage(int order, BufferedImage image)
	{
		DeckItemImage card = new DeckItemImage(order);
		
		subImage = image.getSubimage(card.getText().getX(),
								  card.getText().getY(),
								  card.getText().getW(),
								  card.getText().getH()); //crop image

	    countImage = image.getSubimage(card.getCount().getX(),
								  	   card.getCount().getY(),
								  	   card.getCount().getW(),
								  	   card.getCount().getH());
	    
	    deckItemImage = image.getSubimage(card.getFull().getX(),
	    								  card.getFull().getY(),
	    								  card.getFull().getW(),
	    								  card.getFull().getH());
	  	    
	}
}
