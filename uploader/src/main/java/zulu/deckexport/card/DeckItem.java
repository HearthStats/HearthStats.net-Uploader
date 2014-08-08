/**
 * @author shyos
 */
package zulu.deckexport.card;

import java.awt.image.BufferedImage;


public class DeckItem {
	private Card card;
	private int count;
	private BufferedImage image;
	public DeckItem(Card myCard){
		this.card = myCard;
	}
	public DeckItem(Card myCard, int count)
	{
		this(myCard);
		this.count = count;
	}

	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
	public String toString()
	{
		return card.getName() + " x" + count; 
	}
	public Card getCard() {
		return card;
	}
	public void setCard(Card card) {
		this.card = card;
	}
	public BufferedImage getImage() {
		return image;
	}
	public void setImage(BufferedImage image) {
		this.image = image;
	}
}
