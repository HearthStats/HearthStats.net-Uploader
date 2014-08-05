package zulu.deckexport.extracter;

import java.util.ArrayList;

import zulu.deckexport.card.Card;
import zulu.deckexport.card.DeckItem;
import zulu.deckexport.prob.ProbList;

public class LogicalDeckAlgorithm {

	/**
	 * Merge two part of the deck. First part(without scroll) and second part(with scroll)
	 * @param part1
	 * @param part2
	 * @param probList 
	 * @return
	 */
	static ArrayList<DeckItem> mergeDeckParts(ArrayList<DeckItem> part1,
			ArrayList<DeckItem> part2, ArrayList<ProbList> probList) {
		
		ArrayList<ProbList> probListPart1 = new ArrayList<ProbList>();
		probListPart1.addAll(probList.subList(0, part1.size()));
		
		ArrayList<ProbList> probListPart2 = new ArrayList<ProbList>();
		probListPart2.addAll(probList.subList(part1.size(),probList.size()));
		
		int j = 0;
		boolean onStreak = false;
		for(int i = 0; i<part1.size();i++)
		{
			// If two parts have shared cards skip them.
			if(part1.get(i).getCard().getHearthhead_id() == part2.get(j).getCard().getHearthhead_id())
			{
			//	System.out.println(part1.get(i).getCard().getName() + " is equal " + part2.get(j).getCard().getName());
				onStreak = true;
				j++;
			}
			else if(probListPart2.get(j).isInProbList(part1.get(i).getCard().getHearthhead_id()))
			{
				onStreak = true;
				j++;
			}
			else
			{
				if(onStreak == true) break;
			}
		}	
		// Add left over cards from part2 to part1
		for(;j<part2.size();j++)
		{
			part1.add(part2.get(j));
			probListPart1.add(probListPart2.get(j));
		}	
		// If deck reaches 30 card count, rip off rest
		int count = 0;
		int i;
		j=0;
		ArrayList<DeckItem> mergedDeck = new ArrayList<DeckItem>();
		ArrayList<ProbList> mergedProbList = new ArrayList<ProbList>();
		for(i=0;i<part1.size() && count<30;i++)
		{
			count+=part1.get(i).getCount();
			if(i!=0)
			{
				if(part1.get(i).getCard().getHearthhead_id() == mergedDeck.get(j-1).getCard().getHearthhead_id())
				{
					mergedDeck.get(j-1).setCount(mergedDeck.get(j-1).getCount()+1);
				}
				else
				{
					mergedDeck.add(part1.get(i));
					mergedProbList.add(probListPart1.get(i));
					j++;
				}
			}
			else
			{
				mergedDeck.add(part1.get(i));
				mergedProbList.add(probListPart1.get(i));
				j++;
			}
		}
		return makeDeckLogical(mergedDeck, mergedProbList);
	}
	
	/**
	 * Send deck(each card added with the best similarity result) and problist of the each card.
	 * 
	 * <ul>Then apply `3 Shades of Logical Deck`. Find bankos(must card, similarity is accurate enough) for each card.
	 * <li>Level 1: If an undefined card has a difference with a defined card with less than %6 then mark this card as a banko(accurate enough)</li>
	 * <li>Level 2: For each  not-banko undefined card, find most similar card with `mana` value is proper according to closest banko cards.
	 *  i.e. Deck has 5 cards and 2nd and 4th ones are banko cards. Then 3rd card's mana value should be higger&equal than 2nd card and lower&equal than 4th card.</li>
	 * <li>Control Level 1: Sum counts of each card, if it is higher or lower than 30 deck is invalid.</li>
	 * <br />
	 * <br />
	 * Future Development: Class control can be added. If deck have banko class cards then other cards must be natural or specified class cards.
	 * </ul>
	 * @param deck
	 * @param probList
	 * @return
	 */
	private static ArrayList<DeckItem> makeDeckLogical(ArrayList<DeckItem> deck, ArrayList<ProbList> probList) {
		// TODO Auto-generated method stub
		int[] bankos = new int[deck.size()];
		int[] manas = new int[deck.size()];
		// Reset bankos
		for(int i = 0; i<bankos.length; i++)
		{
			bankos[i] = 0;
			manas[i] = 0;
		}
		// Find bankos level 1
		for(int i = 0; i<probList.size(); i++)
		{
			if(probList.get(i).getProbMin() < 6)
			{
				bankos[i] = 1;
				manas[i] = probList.get(i).getItems().getLast().getCard().getMana();
			}
		}
		// Find level 2
		for(int i=0;i<deck.size();i++)
		{
			if(bankos[i] == 0)
			{
				int lowM = findPrevMana(manas, i);
				int highM = findNextMana(manas, i);
				Card card = probList.get(i).getBestCardWithMana(lowM,highM);
				if(!card.getName().equals("UNKNOWN"))
				{
					bankos[i] = 1;
					manas[i] = card.getMana();
				}
				deck.get(i).setCard(card);
			}
		}
		
		//Invalid deck page
		int count = 0;
		int unkCount = 0;
		for(int i=0; i<deck.size();i++)
		{
			count += deck.get(i).getCount();
			if(deck.get(i).getCard().getName().equalsIgnoreCase("UNKNOWN"))
				unkCount++;
		}
		
		if(unkCount > 5)	// If there are 5 or more cards which we cannot detect then deck is invalid.
			return null;
		if(count < 30)		// If there are less than 30 cards
			return null;
		if(deck.size() < 15) // If deck has less than 15 different cards
			return null;
		
		return deck;
	}
	/**
	 * Helper for Logical Deck Algorithm
	 * @param manas
	 * @param i
	 * @return
	 */
	private static int findNextMana(int[] manas, int i) {
		for(int j=i;j<manas.length;j++)
			if(manas[j] > 0)
			{
				return manas[j];
			}
		return 20;
	}
	/**
	 * Helper for Logical Deck Algorithm
	 * @param manas
	 * @param i
	 * @return
	 */
	private static int findPrevMana(int[] manas, int i) {
		for(int j=i;j>=0;j--)
			if(manas[j] > 0)
			{
				return manas[j];
			}
		return 0;
	}

}
