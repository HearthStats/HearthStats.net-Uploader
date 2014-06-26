/**
 * @author shyos
 */
package zulu.deckexport.prob;

import java.util.LinkedList;

import zulu.deckexport.card.Card;
import zulu.deckexport.extracter.Constants;

public class ProbList{
	LinkedList<ProbListItem> items;
	double probMax;
	double probMin;
	public ProbList()
	{
		items = new LinkedList<ProbListItem>();
		probMax = 100;
	}

	public void add(Card card, double prob)
	{
		int index = 0;
		int i;
		for(i=0;i<items.size();i++)
		{
			if(prob > items.get(i).getProb())
			{		
				break;
			}
		}
		index = i;
		if(items.size() < Constants.probItemLimit)
		{
			items.add(index,new ProbListItem(card, prob));
		}
		else
		{
			items.add(index, new ProbListItem(card, prob));
			items.removeFirst();
		}
		probMax = items.get(0).getProb();
		probMin = items.get(items.size()-1).getProb();
	}

	public LinkedList<ProbListItem> getItems() {
		return items;
	}

	public void setItems(LinkedList<ProbListItem> items) {
		this.items = items;
	}

	public double getProbMax() {
		return probMax;
	}

	public void setProbMax(double probMax) {
		this.probMax = probMax;
	}

	public double getProbMin() {
		return probMin;
	}

	public void setProbMin(double probMin) {
		this.probMin = probMin;
	}

	public boolean isInProbList(int id) {
		for(int i=0;i<items.size();i++)
			if(items.get(i).getCard().getHearthhead_id() == id)
				if(items.get(i).getProb()-items.getLast().getProb() < 5)
				return true;
		return false;
	}

	public Card getBestCardWithMana(int lowM, int highM) {
		for(int i=items.size()-1;i>=0;i--)
			if(items.get(i).getCard().getMana()>=lowM && items.get(i).getCard().getMana() <= highM)
				if(items.get(i).getProb()-items.getLast().getProb() < 5)
				{
					return items.get(i).getCard();
				}
		return new Card("UNKNOWN");
	}

}
