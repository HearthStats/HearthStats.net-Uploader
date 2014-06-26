/**
 * @author shyos
 */
package zulu.deckexport.card;



public class Card {
	private String name;
	private String description;
	private int attack;
	private int health;
	private int card_set_id;
	private int rarity_id;
	private int type_id;
	private int klass_id;
	private int race_id;
	private int mana;
	private boolean collectible;
	private String image_link;
	private int patch_id;
	private int hearthhead_id;
	
	private int[][] h_1024x768; //Stores RGB map of card
	
	public Card(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public int getAttack() {
		return attack;
	}
	public void setAttack(int attack) {
		this.attack = attack;
	}
	public int getHealth() {
		return health;
	}
	public void setHealth(int health) {
		this.health = health;
	}
	public int getCard_set_id() {
		return card_set_id;
	}
	public void setCard_set_id(int card_set_id) {
		this.card_set_id = card_set_id;
	}
	public int getRarity_id() {
		return rarity_id;
	}
	public void setRarity_id(int rarity_id) {
		this.rarity_id = rarity_id;
	}
	public int getType_id() {
		return type_id;
	}
	public void setType_id(int type_id) {
		this.type_id = type_id;
	}
	public int getKlass_id() {
		return klass_id;
	}
	public void setKlass_id(int klass_id) {
		this.klass_id = klass_id;
	}
	public int getRace_id() {
		return race_id;
	}
	public void setRace_id(int race_id) {
		this.race_id = race_id;
	}
	public int getMana() {
		return mana;
	}
	public void setMana(int mana) {
		this.mana = mana;
	}
	public boolean isCollectible() {
		return collectible;
	}
	public void setCollectible(boolean collectible) {
		this.collectible = collectible;
	}
	public String getImage_link() {
		return image_link;
	}
	public void setImage_link(String image_link) {
		this.image_link = image_link;
	}
	public int getPatch_id() {
		return patch_id;
	}
	public void setPatch_id(int patch_id) {
		this.patch_id = patch_id;
	}
	public int getHearthhead_id() {
		return hearthhead_id;
	}
	public void setHearthhead_id(int hearthhead_id) {
		this.hearthhead_id = hearthhead_id;
	}
	public int[][] getH_1024x768() {
		return h_1024x768;
	}
	public void setH_1024x768(int[][] h_1024x768) {
		this.h_1024x768 = h_1024x768;
	}
	public int[][] getHash() {
		return h_1024x768;
	}
	public void setHash(int[][] cardRGB) {
		h_1024x768 = cardRGB;
	}

}
