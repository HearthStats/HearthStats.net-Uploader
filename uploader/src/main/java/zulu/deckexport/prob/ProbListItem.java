/**
 * @author shyos
 */
package zulu.deckexport.prob;

import zulu.deckexport.card.Card;

public class ProbListItem {
	private Card card;
	private double prob;
	public ProbListItem(Card card, double prob) {
		super();
		this.card = card;
		this.prob = prob;
	}
	public Card getCard() {
		return card;
	}
	public void setCard(Card card) {
		this.card = card;
	}
	public double getProb() {
		return prob;
	}
	public void setProb(double prob) {
		this.prob = prob;
	}
}
