/**
 * @author shyos
 */
package zulu.deckexport.card;


public class CardCount {
	public CardCount(int card_count, String card_count_type, int[][] countRGB) {
		this.setDescription(card_count_type);
		this.setCount(card_count);
		this.h_1024x768 = countRGB;
	}
	public CardCount() {
		this.setCount(1);
	}
	private String description;
	private int count;
	private int[][] h_1024x768;
	public int[][] getHash() {
		return h_1024x768;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getCount() {
		return count;
	}
	public void setCount(int count) {
		this.count = count;
	}
}
