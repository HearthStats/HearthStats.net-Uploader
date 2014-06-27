/**
 * @author shyos
 */
package zulu.deckexport.card;

import java.util.ArrayList;
import java.util.List;



public class Deck {
	private String deckName;
	private String deckLink;
	private String deckOwner;
	private String deckClass;
	private List<DeckItem> cards;
	public Deck(ArrayList<DeckItem> deckItems) {
		this.setCards(deckItems);
	}
	public String getDeckName() {
		return deckName;
	}
	public void setDeckName(String deckName) {
		this.deckName = deckName;
	}
	public List<DeckItem> getCards() {
		return cards;
	}
	public void setCards(List<DeckItem> cards) {
		this.cards = cards;
	}
	public Object[] toArray(){
		Object[] myObject = new Object[cards.size()];
		int i=0;
		for(DeckItem d : cards)
		{
			myObject[i] = d.toString();
			i++;
		}
		return myObject; 
	}
	public String getDeckClass() {
		return deckClass;
	}
	public void setDeckClass(String deckClass) {
		this.deckClass = deckClass;
	}
	public String getDeckLink() {
		return deckLink;
	}
	public void setDeckLink(String deckLink) {
		this.deckLink = deckLink;
	}
	public String getDeckOwner() {
		return deckOwner;
	}
	public void setDeckOwner(String deckOwner) {
		this.deckOwner = deckOwner;
	}
}
