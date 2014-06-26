/**
 * @author shyos
 */
package zulu.deckexport.card;

import zulu.deckexport.extracter.PixelManager;
import zulu.deckexport.util.Myrect;

public class DeckItemImage {
	private Myrect mana;
	private Myrect text;
	private Myrect image;
	private Myrect count;
	private Myrect full;
	private int baseY;
	public DeckItemImage(int order)
	{
		getCardByOrder(order);
		this.mana = (PixelManager.getMana());
		this.text = (PixelManager.getText());
		this.image = (PixelManager.getImage());
		this.count = (PixelManager.getCount());	
		this.full = (PixelManager.getFull());
		setBaseYs();
	}
	private void setBaseYs() {
		// TODO Auto-generated method stub
		this.mana.setY(baseY);
		this.text.setY(baseY);
		this.image.setY(baseY);
		this.count.setY(baseY);
		this.full.setY(baseY-PixelManager.getCropHeight());
	}
	
	/**
	 * Re-calculates card's coordinates
	 * @param order
	 */
	public void getCardByOrder(int order)
	{
		this.baseY = PixelManager.getBaseY() + (PixelManager.getHeightOfACard() * order) + (PixelManager.getGap() * order);
		
	}
	public Myrect getMana() {
		return mana;
	}
	public void setMana(Myrect mana) {
		this.mana = mana;
	}
	public Myrect getText() {
		return text;
	}
	public void setText(Myrect text) {
		this.text = text;
	}
	public Myrect getImage() {
		return image;
	}
	public void setImage(Myrect image) {
		this.image = image;
	}
	public Myrect getCount() {
		return count;
	}
	public void setCount(Myrect count) {
		this.count = count;
	}
	public int getBaseY() {
		return baseY;
	}
	public void setBaseY(int baseY) {
		this.baseY = baseY;
	}
	public Myrect getFull() {
		return full;
	}
	public void setFull(Myrect full) {
		this.full = full;
	}
}
